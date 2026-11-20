#!/usr/bin/env python3
"""Backfill purchase_order_lines product/SOL/total_price_vnd and derive PO.order_id.

Dual-DB: reads ies.PO_PURCHASE_ORDER_LINE from cnh_legacy, updates cnh_wms.
Requires LEGACY_DATABASE_URL + TARGET_DATABASE_URL.
"""
from __future__ import annotations

import json
import os
import sys
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path
from typing import Any, Optional

import psycopg2
import psycopg2.extras


def money(v: Any) -> Optional[Decimal]:
    if v is None:
        return None
    return Decimal(str(v)).quantize(Decimal("1"), rounding=ROUND_HALF_UP)


def compute_total_vnd(
    total_price: Any, total_price_vnd: Any, currency: Any, exchange_rate: Any
) -> Optional[Decimal]:
    if total_price_vnd is not None:
        return money(total_price_vnd)
    tp = money(total_price)
    if tp is None:
        return None
    cur = (str(currency or "").strip().upper() or "VND")
    if cur in ("", "VND", "VNĐ"):
        return tp
    try:
        rate = Decimal(str(exchange_rate)) if exchange_rate is not None else None
    except Exception:
        rate = None
    if rate is not None and rate > 0:
        return (tp * rate).quantize(Decimal("1"), rounding=ROUND_HALF_UP)
    return tp


def main() -> int:
    legacy_url = os.environ.get("LEGACY_DATABASE_URL")
    target_url = os.environ.get("TARGET_DATABASE_URL")
    if not legacy_url or not target_url:
        print("LEGACY_DATABASE_URL and TARGET_DATABASE_URL required", file=sys.stderr)
        return 1

    src = psycopg2.connect(legacy_url)
    dst = psycopg2.connect(target_url)
    src.autocommit = True
    dst.autocommit = False

    report: dict[str, Any] = {}
    try:
        with src.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as sc, dst.cursor() as dc:
            dc.execute("SELECT id FROM products WHERE is_deleted = false")
            products = {r[0] for r in dc.fetchall()}
            dc.execute("SELECT id FROM order_lines WHERE is_deleted = false")
            order_lines = {r[0] for r in dc.fetchall()}
            print(f"products={len(products)} order_lines={len(order_lines)}")

            sc.execute(
                """
                SELECT id, product_id, sale_orderline_id, total_price, total_price_vnd,
                       currency, exchange_rate
                FROM ies."PO_PURCHASE_ORDER_LINE"
                WHERE deleted_at IS NULL
                """
            )
            legacy_rows = list(sc.fetchall())
            print(f"legacy_pol={len(legacy_rows)}")

            batch: list[tuple] = []
            for row in legacy_rows:
                pid = row["product_id"] if row["product_id"] in products else None
                sol = (
                    row["sale_orderline_id"]
                    if row["sale_orderline_id"] in order_lines
                    else None
                )
                total_vnd = compute_total_vnd(
                    row["total_price"],
                    row["total_price_vnd"],
                    row["currency"],
                    row["exchange_rate"],
                )
                batch.append((pid, sol, total_vnd, row["id"]))

            psycopg2.extras.execute_batch(
                dc,
                """
                UPDATE purchase_order_lines pol
                SET product_id = COALESCE(pol.product_id, %s),
                    sale_orderline_id = COALESCE(pol.sale_orderline_id, %s),
                    total_price_vnd = COALESCE(pol.total_price_vnd, %s)
                WHERE pol.id = %s
                  AND pol.is_deleted = false
                """,
                batch,
                page_size=500,
            )
            dst.commit()

            dc.execute(
                """
                SELECT
                  COUNT(*) FILTER (WHERE product_id IS NULL) AS null_product,
                  COUNT(*) FILTER (WHERE sale_orderline_id IS NULL) AS null_sol,
                  COUNT(*) FILTER (WHERE total_price_vnd IS NULL) AS null_vnd,
                  COUNT(*) AS total
                FROM purchase_order_lines
                WHERE is_deleted = false
                """
            )
            null_product, null_sol, null_vnd, total = dc.fetchone()
            report["pol_after"] = {
                "total": total,
                "null_product": null_product,
                "null_sol": null_sol,
                "null_vnd": null_vnd,
            }
            print(
                f"  pol after: total={total} null_product={null_product} "
                f"null_sol={null_sol} null_vnd={null_vnd}"
            )

            # Derive purchase_orders.order_id from sale_orderline → order_lines.order_id
            print("== derive purchase_orders.order_id ==")
            dc.execute(
                """
                WITH derived AS (
                  SELECT
                    pol.purchase_order_id AS po_id,
                    (array_agg(ol.order_id ORDER BY pol.created_at NULLS LAST, pol.id))[1] AS order_id
                  FROM purchase_order_lines pol
                  JOIN order_lines ol ON ol.id = pol.sale_orderline_id AND ol.is_deleted = false
                  WHERE pol.is_deleted = false
                    AND pol.sale_orderline_id IS NOT NULL
                  GROUP BY pol.purchase_order_id
                )
                UPDATE purchase_orders po
                SET order_id = d.order_id
                FROM derived d
                WHERE po.id = d.po_id
                  AND po.order_id IS NULL
                  AND d.order_id IS NOT NULL
                  AND po.is_deleted = false
                """
            )
            linked = dc.rowcount
            dst.commit()
            report["po_order_id_linked"] = linked
            print(f"  linked_order_id={linked}")

            dc.execute(
                """
                SELECT
                  COUNT(*) FILTER (WHERE order_id IS NULL) AS null_order,
                  COUNT(*) AS total
                FROM purchase_orders
                WHERE is_deleted = false
                """
            )
            null_order, po_total = dc.fetchone()
            report["po_after"] = {"total": po_total, "null_order_id": null_order}
            print(f"  po after: total={po_total} null_order_id={null_order}")

            # Spot-check PO_2026_11.039 if present
            dc.execute(
                """
                SELECT po.id, po.order_id,
                       (SELECT COUNT(*) FROM purchase_order_lines l
                        WHERE l.purchase_order_id = po.id AND l.product_id IS NOT NULL
                          AND l.is_deleted = false) AS lines_with_product
                FROM purchase_orders po
                WHERE po.po_prefix = 'PO_2026_11' AND po.po_number = 39
                  AND po.is_deleted = false
                """
            )
            spot = dc.fetchone()
            if spot:
                report["spot_PO_2026_11_039"] = {
                    "id": str(spot[0]),
                    "order_id": str(spot[1]) if spot[1] else None,
                    "lines_with_product": spot[2],
                }
                print(
                    f"  spot PO_2026_11.039 order_id={spot[1]} "
                    f"lines_with_product={spot[2]}"
                )

        out_dir = Path(__file__).resolve().parents[1] / "out"
        out_dir.mkdir(parents=True, exist_ok=True)
        out_path = out_dir / "backfill_po_links_report.json"
        out_path.write_text(json.dumps(report, indent=2, default=str))
        print(f"REPORT {out_path}")
        print("COMMIT OK")
        return 0
    except Exception as e:
        dst.rollback()
        print(f"FAILED: {e}", file=sys.stderr)
        raise
    finally:
        src.close()
        dst.close()


if __name__ == "__main__":
    sys.exit(main())

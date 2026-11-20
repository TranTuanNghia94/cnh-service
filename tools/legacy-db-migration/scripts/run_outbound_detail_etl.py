#!/usr/bin/env python3
"""Migrate ies.EXPORT_REQUEST_DETAIL → warehouse_outbound_details.

Resolves required order_line_id via EXPORT_REQUEST.sale_order_id + product_id:
  - exact 1 match → use it
  - multi match → earliest created_at order_line
  - zero match → skip (counted)

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


def money(v: Any) -> Decimal:
    if v is None:
        return Decimal("0")
    return Decimal(str(v)).quantize(Decimal("0.01"), ROUND_HALF_UP)


def qty(v: Any) -> Decimal:
    if v is None:
        return Decimal("0")
    return Decimal(str(v)).quantize(Decimal("0.001"), ROUND_HALF_UP)


def rate(v: Any) -> Decimal:
    if v is None:
        return Decimal("0")
    return Decimal(str(v)).quantize(Decimal("0.01"), ROUND_HALF_UP)


def notes_text(note: Any) -> Optional[str]:
    if note is None:
        return None
    if isinstance(note, (list, tuple)):
        return "\n".join(str(x) for x in note if x)
    return str(note)


def audit_user(user_by_id: dict, uid: Any, fallback: str = "legacy-migrate") -> str:
    if uid is None:
        return fallback
    return user_by_id.get(uid) or fallback


def main() -> int:
    legacy_url = os.environ.get("LEGACY_DATABASE_URL")
    target_url = os.environ.get("TARGET_DATABASE_URL")
    if not legacy_url or not target_url:
        print("LEGACY_DATABASE_URL and TARGET_DATABASE_URL required", file=sys.stderr)
        return 1

    src = psycopg2.connect(legacy_url)
    dst = psycopg2.connect(target_url)
    src.autocommit = False
    dst.autocommit = False

    try:
        with src.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as sc, dst.cursor() as dc:
            dc.execute("SELECT id FROM warehouse_outbounds")
            outbounds = {r[0] for r in dc.fetchall()}
            dc.execute("SELECT id FROM products")
            products = {r[0] for r in dc.fetchall()}
            dc.execute("SELECT id FROM warehouse_outbound_details")
            existing = {r[0] for r in dc.fetchall()}
            dc.execute("SELECT id, username FROM users WHERE is_deleted = false")
            user_by_id = {r[0]: r[1] for r in dc.fetchall()}

            # (order_id, product_id) → earliest order_line id
            dc.execute(
                """
                SELECT DISTINCT ON (order_id, product_id) order_id, product_id, id
                FROM order_lines
                WHERE is_deleted = false
                ORDER BY order_id, product_id, created_at NULLS LAST, id
                """
            )
            line_by_order_product = {(r[0], r[1]): r[2] for r in dc.fetchall()}
            print(
                f"outbounds={len(outbounds)} products={len(products)} "
                f"line_keys={len(line_by_order_product)} existing={len(existing)}"
            )

            sc.execute(
                """
                SELECT ed.id, ed.product_id, ed.quantity, ed.unit_price, ed.total_price,
                       ed.currency, ed.tax, ed.tax_amount, ed.box, ed.ref, ed.note,
                       ed.created_at, ed.updated_at, ed.deleted_at,
                       er.sale_order_id, er.created_by, er.updated_by,
                       ed.export_request_id
                FROM ies."EXPORT_REQUEST_DETAIL" ed
                JOIN ies."EXPORT_REQUEST" er ON er.id = ed.export_request_id
                WHERE ed.export_request_id IS NOT NULL
                ORDER BY ed.created_at NULLS LAST, ed.id
                """
            )

            inserted = 0
            skips = {
                "existing_id": 0,
                "missing_outbound": 0,
                "missing_product": 0,
                "no_sale_order": 0,
                "no_order_line": 0,
                "conflict": 0,
            }
            batch: list[tuple] = []
            batch_size = 500

            def flush() -> None:
                nonlocal inserted, batch
                if not batch:
                    return
                psycopg2.extras.execute_batch(
                    dc,
                    """
                    INSERT INTO warehouse_outbound_details (
                      id, outbound_id, order_line_id, product_id,
                      quantity, box, reference_code, unit_price, price_without_tax,
                      vat, currency, total_amount, tax_amount, note,
                      created_at, updated_at, created_by, updated_by, is_deleted
                    ) VALUES (
                      %s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,
                      COALESCE(%s,NOW()),COALESCE(%s,NOW()),%s,%s,%s
                    )
                    ON CONFLICT (id) DO NOTHING
                    """,
                    batch,
                    page_size=500,
                )
                inserted += len(batch)
                batch = []

            for row in sc:
                did = row["id"]
                if did in existing:
                    skips["existing_id"] += 1
                    continue
                outbound_id = row["export_request_id"]
                if outbound_id not in outbounds:
                    skips["missing_outbound"] += 1
                    continue
                product_id = row["product_id"]
                if not product_id or product_id not in products:
                    skips["missing_product"] += 1
                    continue
                sale_order_id = row["sale_order_id"]
                if not sale_order_id:
                    skips["no_sale_order"] += 1
                    continue
                order_line_id = line_by_order_product.get((sale_order_id, product_id))
                if not order_line_id:
                    skips["no_order_line"] += 1
                    continue

                unit = money(row["unit_price"])
                total = money(row["total_price"] or (unit * qty(row["quantity"])))
                vat = rate(row["tax"])
                tax_amt = money(row["tax_amount"]) if row["tax_amount"] is not None else (
                    money(total * vat / Decimal("100")) if vat else Decimal("0")
                )
                # price_without_tax ≈ total - tax when tax included; else unit*qty
                price_wo = money(total - tax_amt) if tax_amt else total
                created_by = audit_user(user_by_id, row["created_by"])
                updated_by = audit_user(user_by_id, row["updated_by"], created_by)
                currency = (row["currency"] or "VND")[:10]
                ref = (row["ref"] or None)
                if ref is not None:
                    ref = str(ref)[:200]
                box = (row["box"] or None)
                if box is not None:
                    box = str(box)[:100]

                batch.append(
                    (
                        did,
                        outbound_id,
                        order_line_id,
                        product_id,
                        qty(row["quantity"]),
                        box,
                        ref,
                        unit,
                        price_wo,
                        vat,
                        currency,
                        total,
                        tax_amt,
                        notes_text(row["note"]),
                        row["created_at"],
                        row["updated_at"] or row["created_at"],
                        created_by,
                        updated_by,
                        row["deleted_at"] is not None,
                    )
                )
                existing.add(did)
                if len(batch) >= batch_size:
                    flush()
                    dst.commit()
                    print(f"  progress inserted~={inserted}")

            flush()
            dst.commit()
            report = {"inserted": inserted, "skipped": skips}
            print(f"DONE inserted={inserted} skipped={skips}")
            out_dir = Path(__file__).resolve().parents[1] / "out"
            out_dir.mkdir(parents=True, exist_ok=True)
            out_path = out_dir / "etl_skip_report_outbound_details.json"
            out_path.write_text(json.dumps(report, indent=2, default=str))
            print(f"REPORT {out_path}")
        return 0
    except Exception as e:
        dst.rollback()
        src.rollback()
        print(f"FAILED: {e}", file=sys.stderr)
        raise
    finally:
        src.close()
        dst.close()


if __name__ == "__main__":
    sys.exit(main())

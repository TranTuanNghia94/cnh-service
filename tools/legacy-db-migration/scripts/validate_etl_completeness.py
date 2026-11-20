#!/usr/bin/env python3
"""Compare legacy vs target counts and fail on unexpected gaps.

Requires LEGACY_DATABASE_URL + TARGET_DATABASE_URL.
Exit 0 only when all hard gaps are within max_allowed.
"""
from __future__ import annotations

import json
import os
import sys
from pathlib import Path

import psycopg2

# (name, legacy_sql, target_sql, max_allowed_gap)
# gap = legacy - target (positive means target is short)
CHECKS = [
    (
        "products_active",
        'SELECT COUNT(*) FROM ies."PRO_PRODUCT" WHERE deleted_at IS NULL',
        "SELECT COUNT(*) FROM products WHERE is_deleted = false",
        0,
    ),
    (
        "customers_active",
        'SELECT COUNT(*) FROM ies."CUS_CUSTOMER" WHERE deleted_at IS NULL',
        "SELECT COUNT(*) FROM customers WHERE is_deleted = false",
        0,
    ),
    (
        "vendors_active",
        'SELECT COUNT(*) FROM ies."VEN_VENDOR" WHERE deleted_at IS NULL',
        "SELECT COUNT(*) FROM vendors WHERE is_deleted = false",
        0,
    ),
    (
        "categories_active",
        'SELECT COUNT(*) FROM ies."PRO_PRODUCT_CAT" WHERE deleted_at IS NULL',
        "SELECT COUNT(*) FROM categories WHERE is_deleted = false",
        0,
    ),
    (
        "customer_addresses_active",
        'SELECT COUNT(*) FROM ies."CUS_CUSTOMER_CONTACT" WHERE deleted_at IS NULL',
        "SELECT COUNT(*) FROM customer_addresses WHERE is_deleted = false",
        0,
    ),
    (
        "orders_active",
        'SELECT COUNT(*) FROM ies."ORD_ORDER" WHERE deleted_at IS NULL',
        "SELECT COUNT(*) FROM orders WHERE is_deleted = false",
        0,
    ),
    (
        "order_lines_active",
        'SELECT COUNT(*) FROM ies."ORD_ORDER_LINE" WHERE deleted_at IS NULL',
        "SELECT COUNT(*) FROM order_lines WHERE is_deleted = false",
        0,
    ),
    (
        "purchase_orders_active",
        'SELECT COUNT(*) FROM ies."PO_PURCHASE_ORDER" WHERE deleted_at IS NULL',
        "SELECT COUNT(*) FROM purchase_orders WHERE is_deleted = false",
        0,
    ),
    (
        "purchase_order_lines_active",
        'SELECT COUNT(*) FROM ies."PO_PURCHASE_ORDER_LINE" WHERE deleted_at IS NULL',
        "SELECT COUNT(*) FROM purchase_order_lines WHERE is_deleted = false",
        0,
    ),
    (
        "payment_requests_active",
        'SELECT COUNT(*) FROM ies."PAY_PAYMENT" WHERE deleted_at IS NULL',
        "SELECT COUNT(*) FROM payment_requests WHERE is_deleted = false",
        0,
    ),
    (
        "inbound_receipts_active",
        'SELECT COUNT(*) FROM ies."IMPORT_REQUEST" WHERE deleted_at IS NULL',
        "SELECT COUNT(*) FROM warehouse_inbound_receipts WHERE is_deleted = false",
        0,
    ),
    (
        "inbound_lines_active",
        'SELECT COUNT(*) FROM ies."IMPORT_REQUEST_DETAIL" WHERE deleted_at IS NULL',
        "SELECT COUNT(*) FROM warehouse_inbound_receipt_lines WHERE is_deleted = false",
        0,
    ),
    (
        "outbounds_active",
        'SELECT COUNT(*) FROM ies."EXPORT_REQUEST" WHERE deleted_at IS NULL AND sale_order_id IS NOT NULL',
        "SELECT COUNT(*) FROM warehouse_outbounds WHERE is_deleted = false",
        0,
    ),
    (
        "outbound_details_active",
        'SELECT COUNT(*) FROM ies."EXPORT_REQUEST_DETAIL" WHERE deleted_at IS NULL',
        "SELECT COUNT(*) FROM warehouse_outbound_details WHERE is_deleted = false",
        150,  # unresolved order_line matches
    ),
    (
        "stock_tx_active",
        'SELECT COUNT(*) FROM ies."INVENTORY_TRANSACTION" WHERE deleted_at IS NULL',
        "SELECT COUNT(*) FROM warehouse_stock_transactions WHERE is_deleted = false",
        30,  # zero-qty skips
    ),
]

ORPHAN_CHECKS = [
    (
        "order_lines_missing_product",
        """
        SELECT COUNT(*) FROM order_lines ol
        LEFT JOIN products p ON p.id = ol.product_id
        WHERE ol.product_id IS NOT NULL AND p.id IS NULL
        """,
    ),
    (
        "outbound_detail_missing_order_line",
        """
        SELECT COUNT(*) FROM warehouse_outbound_details d
        LEFT JOIN order_lines ol ON ol.id = d.order_line_id
        WHERE ol.id IS NULL
        """,
    ),
    (
        "inventory_missing_product",
        """
        SELECT COUNT(*) FROM warehouse_inventory i
        LEFT JOIN products p ON p.id = i.product_id
        WHERE p.id IS NULL
        """,
    ),
]

# (name, sql, max_allowed) — target-only quality
TARGET_QUALITY_CHECKS = [
    (
        "approvals_legacy_migrate",
        """
        SELECT COUNT(*) FROM payment_request_approvals
        WHERE created_by = 'legacy-migrate' OR updated_by = 'legacy-migrate'
        """,
        0,
    ),
    (
        "stock_tx_legacy_migrate_non_opening",
        """
        SELECT COUNT(*) FROM warehouse_stock_transactions
        WHERE created_by = 'legacy-migrate'
          AND reference_type NOT IN ('LEGACY_OPENING_STOCK', 'LEGACY_STOCKTAKE', 'LEGACY_INVENTORY_TRANSACTION')
        """,
        50,
    ),
    (
        "po_null_order_id_with_sol",
        """
        SELECT COUNT(*) FROM purchase_orders po
        WHERE po.is_deleted = false
          AND po.order_id IS NULL
          AND EXISTS (
            SELECT 1 FROM purchase_order_lines pol
            JOIN order_lines ol ON ol.id = pol.sale_orderline_id AND ol.is_deleted = false
            WHERE pol.purchase_order_id = po.id AND pol.is_deleted = false
          )
        """,
        20,
    ),
    (
        "pol_null_total_price_vnd",
        """
        SELECT COUNT(*) FROM purchase_order_lines
        WHERE is_deleted = false AND total_price_vnd IS NULL AND total_price IS NOT NULL
        """,
        100,
    ),
]


def main() -> int:
    legacy_url = os.environ.get("LEGACY_DATABASE_URL")
    target_url = os.environ.get("TARGET_DATABASE_URL")
    if not legacy_url or not target_url:
        print("LEGACY_DATABASE_URL and TARGET_DATABASE_URL required", file=sys.stderr)
        return 1

    src = psycopg2.connect(legacy_url)
    dst = psycopg2.connect(target_url)
    failures = []
    results = []

    try:
        with src.cursor() as sc, dst.cursor() as dc:
            print(f"{'check':32} {'legacy':>8} {'target':>8} {'gap':>8} {'max':>6} status")
            for name, lq, tq, max_gap in CHECKS:
                sc.execute(lq)
                legacy = sc.fetchone()[0]
                dc.execute(tq)
                target = dc.fetchone()[0]
                gap = legacy - target
                # Allow target to have more soft-deleted-included rows than legacy active filter
                # Fail only when target is short beyond allowlist.
                shortfall = max(0, gap)
                ok = shortfall <= max_gap
                status = "OK" if ok else "FAIL"
                print(f"{name:32} {legacy:8} {target:8} {gap:8} {max_gap:6} {status}")
                results.append(
                    {
                        "check": name,
                        "legacy": legacy,
                        "target": target,
                        "gap": gap,
                        "max_allowed": max_gap,
                        "ok": ok,
                    }
                )
                if not ok:
                    failures.append(name)

            print("\n-- orphan checks (target) --")
            for name, q in ORPHAN_CHECKS:
                dc.execute(q)
                n = dc.fetchone()[0]
                ok = n == 0
                print(f"{name:40} {n:8} {'OK' if ok else 'FAIL'}")
                results.append({"check": name, "orphans": n, "ok": ok})
                if not ok:
                    failures.append(name)

            print("\n-- quality checks (target) --")
            for name, q, max_allowed in TARGET_QUALITY_CHECKS:
                dc.execute(q)
                n = dc.fetchone()[0]
                ok = n <= max_allowed
                print(f"{name:40} {n:8} max={max_allowed} {'OK' if ok else 'FAIL'}")
                results.append(
                    {"check": name, "count": n, "max_allowed": max_allowed, "ok": ok}
                )
                if not ok:
                    failures.append(name)

            # Recoverable null POL product: legacy has product_id that exists in target
            print("\n-- recoverable POL product gaps --")
            sc.execute(
                """
                SELECT id, product_id FROM ies."PO_PURCHASE_ORDER_LINE"
                WHERE deleted_at IS NULL AND product_id IS NOT NULL
                """
            )
            legacy_pol_products = {r[0]: r[1] for r in sc.fetchall()}
            dc.execute(
                "SELECT id FROM purchase_order_lines WHERE is_deleted = false AND product_id IS NULL"
            )
            null_pol_ids = [r[0] for r in dc.fetchall()]
            dc.execute("SELECT id FROM products WHERE is_deleted = false")
            products = {r[0] for r in dc.fetchall()}
            recoverable = 0
            for tid in null_pol_ids:
                leg_pid = legacy_pol_products.get(tid)
                if leg_pid and leg_pid in products:
                    recoverable += 1
            ok = recoverable == 0
            print(f"{'pol_null_product_recoverable':40} {recoverable:8} {'OK' if ok else 'FAIL'}")
            results.append(
                {"check": "pol_null_product_recoverable", "count": recoverable, "ok": ok}
            )
            if not ok:
                failures.append("pol_null_product_recoverable")

            # Spot-check known order
            oid = "29ed3b27-598b-4a8f-880b-47c268b87c17"
            dc.execute(
                "SELECT COUNT(*) FROM order_lines WHERE order_id=%s AND is_deleted=false",
                (oid,),
            )
            lines = dc.fetchone()[0]
            ok = lines >= 4
            print(f"\nspot_order_{oid[:8]}_lines={lines} {'OK' if ok else 'FAIL'}")
            results.append({"check": "spot_order_lines", "lines": lines, "ok": ok})
            if not ok:
                failures.append("spot_order_lines")

            # Spot-check PO_2026_11.039
            dc.execute(
                """
                SELECT po.order_id IS NOT NULL,
                       (SELECT COUNT(*) FROM purchase_order_lines l
                        WHERE l.purchase_order_id = po.id AND l.is_deleted = false
                          AND l.product_id IS NOT NULL)
                FROM purchase_orders po
                WHERE po.po_prefix = 'PO_2026_11' AND po.po_number = 39
                  AND po.is_deleted = false
                """
            )
            spot = dc.fetchone()
            if spot:
                has_order, with_product = spot
                ok = bool(has_order) and with_product > 0
                print(
                    f"spot_PO_2026_11_039 order={has_order} lines_with_product={with_product} "
                    f"{'OK' if ok else 'FAIL'}"
                )
                results.append(
                    {
                        "check": "spot_PO_2026_11_039",
                        "has_order": bool(has_order),
                        "lines_with_product": with_product,
                        "ok": ok,
                    }
                )
                if not ok:
                    failures.append("spot_PO_2026_11_039")
            else:
                print("spot_PO_2026_11_039 not found (skip)")

        out_dir = Path(__file__).resolve().parents[1] / "out"
        out_dir.mkdir(parents=True, exist_ok=True)
        out_path = out_dir / "etl_completeness_report.json"
        out_path.write_text(
            json.dumps({"ok": not failures, "failures": failures, "results": results}, indent=2)
        )
        print(f"\nREPORT {out_path}")
        if failures:
            print(f"FAILED checks: {failures}", file=sys.stderr)
            return 1
        print("ALL CHECKS PASSED")
        return 0
    finally:
        src.close()
        dst.close()


if __name__ == "__main__":
    sys.exit(main())

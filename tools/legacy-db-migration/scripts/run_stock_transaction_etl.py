#!/usr/bin/env python3
"""Migrate ies.INVENTORY_TRANSACTION → warehouse_stock_transactions.

Maps:
  NHAP_KHO  → direction INBOUND,  reference LEGACY_IMPORT_REQUEST
  XUAT_KHO  → direction OUTBOUND, reference LEGACY_EXPORT_REQUEST
  TON_KHO   → direction IMPORT,   reference LEGACY_OPENING_STOCK
  KIEM_DEM  → INBOUND/OUTBOUND by quantity_change sign, LEGACY_STOCKTAKE

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


def qty(v: Any) -> Decimal:
    if v is None:
        return Decimal("0")
    return Decimal(str(abs(v))).quantize(Decimal("0.001"), rounding=ROUND_HALF_UP)


def map_direction(activity: str, quantity_change: Any) -> Optional[str]:
    act = (activity or "").upper()
    if act == "NHAP_KHO":
        return "INBOUND"
    if act == "XUAT_KHO":
        return "OUTBOUND"
    if act == "TON_KHO":
        return "IMPORT"
    if act == "KIEM_DEM":
        try:
            change = Decimal(str(quantity_change or 0))
        except Exception:
            change = Decimal("0")
        return "INBOUND" if change >= 0 else "OUTBOUND"
    return None


def map_reference(activity: str, import_id, export_id) -> tuple[str, Any]:
    act = (activity or "").upper()
    if act == "XUAT_KHO" and export_id:
        return "LEGACY_EXPORT_REQUEST", export_id
    if act == "NHAP_KHO" and import_id:
        return "LEGACY_IMPORT_REQUEST", import_id
    if act == "TON_KHO":
        return "LEGACY_OPENING_STOCK", import_id or export_id
    if act == "KIEM_DEM":
        return "LEGACY_STOCKTAKE", import_id or export_id
    if export_id:
        return "LEGACY_EXPORT_REQUEST", export_id
    if import_id:
        return "LEGACY_IMPORT_REQUEST", import_id
    return "LEGACY_INVENTORY_TRANSACTION", None


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
            dc.execute("SELECT id FROM products")
            products = {r[0] for r in dc.fetchall()}
            dc.execute("SELECT id FROM warehouse_stock_transactions")
            existing = {r[0] for r in dc.fetchall()}
            # Inherit creators from already-migrated inbound/outbound headers
            dc.execute(
                """
                SELECT id, created_by FROM warehouse_inbound_receipts
                WHERE created_by IS NOT NULL AND created_by <> 'legacy-migrate'
                """
            )
            inbound_creator = {r[0]: r[1] for r in dc.fetchall()}
            dc.execute(
                """
                SELECT id, created_by FROM warehouse_outbounds
                WHERE created_by IS NOT NULL AND created_by <> 'legacy-migrate'
                """
            )
            outbound_creator = {r[0]: r[1] for r in dc.fetchall()}
            print(
                f"products={len(products)} existing_tx={len(existing)} "
                f"inbound_creators={len(inbound_creator)} outbound_creators={len(outbound_creator)}"
            )

            sc.execute(
                """
                SELECT id, activity::text AS activity, quantity_change,
                       product_id, import_request_id, export_request_id, note,
                       created_at, updated_at, deleted_at
                FROM ies."INVENTORY_TRANSACTION"
                WHERE product_id IS NOT NULL
                ORDER BY created_at NULLS LAST, id
                """
            )

            batch: list[tuple] = []
            inserted = skipped = skipped_product = skipped_zero = skipped_activity = 0
            batch_size = 1000

            def flush() -> None:
                nonlocal inserted, batch
                if not batch:
                    return
                psycopg2.extras.execute_batch(
                    dc,
                    """
                    INSERT INTO warehouse_stock_transactions (
                      id, product_id, direction, quantity, reference_type, reference_id,
                      note, created_at, updated_at, created_by, updated_by, is_deleted
                    ) VALUES (
                      %s,%s,%s,%s,%s,%s,%s,
                      COALESCE(%s, NOW()), COALESCE(%s, NOW()),
                      %s,%s,%s
                    )
                    ON CONFLICT (id) DO NOTHING
                    """,
                    batch,
                    page_size=500,
                )
                inserted += len(batch)
                batch = []

            for row in sc:
                tid = row["id"]
                if tid in existing:
                    skipped += 1
                    continue
                pid = row["product_id"]
                if pid not in products:
                    skipped_product += 1
                    continue
                direction = map_direction(row["activity"], row["quantity_change"])
                if not direction:
                    skipped_activity += 1
                    continue
                quantity = qty(row["quantity_change"])
                if quantity == 0:
                    skipped_zero += 1
                    continue
                ref_type, ref_id = map_reference(
                    row["activity"], row["import_request_id"], row["export_request_id"]
                )
                created = row["created_at"]
                updated = row["updated_at"] or created
                # INVENTORY_TRANSACTION has no audit cols — inherit from parent receipt/outbound
                creator = "legacy-migrate"
                if ref_type == "LEGACY_IMPORT_REQUEST" and ref_id in inbound_creator:
                    creator = inbound_creator[ref_id]
                elif ref_type == "LEGACY_EXPORT_REQUEST" and ref_id in outbound_creator:
                    creator = outbound_creator[ref_id]
                batch.append(
                    (
                        tid,
                        pid,
                        direction,
                        quantity,
                        ref_type[:80],
                        ref_id,
                        row["note"],
                        created,
                        updated,
                        creator,
                        creator,
                        row["deleted_at"] is not None,
                    )
                )
                existing.add(tid)
                if len(batch) >= batch_size:
                    flush()
                    dst.commit()
                    print(f"  progress inserted~={inserted} skipped={skipped}")

            flush()
            dst.commit()
            report = {
                "inserted": inserted,
                "skipped_existing": skipped,
                "skipped_product": skipped_product,
                "skipped_zero": skipped_zero,
                "skipped_activity": skipped_activity,
            }
            print(
                f"DONE inserted={inserted} skipped_existing={skipped} "
                f"skipped_product={skipped_product} skipped_zero={skipped_zero} "
                f"skipped_activity={skipped_activity}"
            )
            out_dir = Path(__file__).resolve().parents[1] / "out"
            out_dir.mkdir(parents=True, exist_ok=True)
            out_path = out_dir / "etl_skip_report_stock.json"
            out_path.write_text(json.dumps(report, indent=2))
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

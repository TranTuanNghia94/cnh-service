#!/usr/bin/env python3
"""Rewrite legacy-migrate / raw-UUID created_by|updated_by → users.username.

Dual-DB: reads creator UUIDs from cnh_legacy, resolves via target users, updates cnh_wms.
Also inherits audit from parent docs for stock txs / inbound lines / payment children.
Requires LEGACY_DATABASE_URL + TARGET_DATABASE_URL.
"""
from __future__ import annotations

import os
import re
import sys
import uuid
from typing import Any, Optional

import psycopg2
import psycopg2.extras

UUID_RE = re.compile(
    r"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
    re.I,
)

# (target_table, legacy_schema.table)
ENTITY_MAPPINGS = [
    ("orders", 'ies."ORD_ORDER"'),
    ("order_lines", 'ies."ORD_ORDER_LINE"'),
    ("purchase_orders", 'ies."PO_PURCHASE_ORDER"'),
    ("purchase_order_lines", 'ies."PO_PURCHASE_ORDER_LINE"'),
    ("payment_requests", 'ies."PAY_PAYMENT"'),
    ("warehouse_outbounds", 'ies."EXPORT_REQUEST"'),
    ("warehouse_inbound_receipts", 'ies."IMPORT_REQUEST"'),
    ("file_infos", 'core."SYS_MEDIA"'),
    ("payment_request_approvals", 'ies."PAY_APPROVAL_HISTORY"'),
]


def as_uuid(v: Any) -> Optional[uuid.UUID]:
    if v is None:
        return None
    if isinstance(v, uuid.UUID):
        return v
    try:
        return uuid.UUID(str(v))
    except Exception:
        return None


def audit_user(
    user_by_id: dict[uuid.UUID, str], uid: Any, fallback: Optional[str] = None
) -> Optional[str]:
    key = as_uuid(uid)
    if key is None:
        return fallback
    return user_by_id.get(key) or fallback


def needs_rewrite(value: Optional[str]) -> bool:
    if value is None or value == "":
        return True
    if value == "legacy-migrate":
        return True
    return bool(UUID_RE.match(value))


def backfill_table(
    sc,
    dc,
    target_table: str,
    legacy_table: str,
    user_by_id: dict[uuid.UUID, str],
) -> tuple[int, int]:
    sc.execute(
        f"""
        SELECT id, created_by, updated_by
        FROM {legacy_table}
        WHERE created_by IS NOT NULL OR updated_by IS NOT NULL
        """
    )
    legacy_rows = {as_uuid(r["id"]): r for r in sc.fetchall() if as_uuid(r["id"])}

    dc.execute(
        f"""
        SELECT id::text, created_by, updated_by
        FROM {target_table}
        WHERE created_by = 'legacy-migrate'
           OR updated_by = 'legacy-migrate'
           OR created_by ~* '^[0-9a-f]{{8}}-[0-9a-f]{{4}}-[0-9a-f]{{4}}-[0-9a-f]{{4}}-[0-9a-f]{{12}}$'
           OR updated_by ~* '^[0-9a-f]{{8}}-[0-9a-f]{{4}}-[0-9a-f]{{4}}-[0-9a-f]{{4}}-[0-9a-f]{{12}}$'
        """
    )
    updated = skipped = 0
    batch: list[tuple] = []
    for row in dc.fetchall():
        tid = as_uuid(row[0])
        if tid is None or tid not in legacy_rows:
            skipped += 1
            continue
        leg = legacy_rows[tid]
        cur_created, cur_updated = row[1], row[2]
        new_created = (
            audit_user(user_by_id, leg["created_by"], cur_created)
            if needs_rewrite(cur_created)
            else cur_created
        )
        new_updated = (
            audit_user(user_by_id, leg["updated_by"], new_created or cur_updated)
            if needs_rewrite(cur_updated)
            else cur_updated
        )
        if new_created == cur_created and new_updated == cur_updated:
            skipped += 1
            continue
        if new_created is None and new_updated is None:
            skipped += 1
            continue
        batch.append((new_created, new_updated, str(tid)))

    if batch:
        psycopg2.extras.execute_batch(
            dc,
            f"""
            UPDATE {target_table}
            SET created_by = COALESCE(%s, created_by),
                updated_by = COALESCE(%s, updated_by)
            WHERE id = %s::uuid
            """,
            batch,
            page_size=500,
        )
        updated = len(batch)
    return updated, skipped


def backfill_payment_via_requestor(dc) -> int:
    dc.execute(
        """
        UPDATE payment_requests pr
        SET created_by = u.username,
            updated_by = CASE
              WHEN pr.updated_by = 'legacy-migrate'
                OR pr.updated_by ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
              THEN u.username
              ELSE pr.updated_by
            END
        FROM users u
        WHERE u.id = pr.requestor_id
          AND u.is_deleted = false
          AND (
            pr.created_by = 'legacy-migrate'
            OR pr.created_by ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
          )
        """
    )
    return dc.rowcount


def backfill_approvals_via_approver(dc) -> int:
    """Fill missing approver_id from legacy UUID already mapped into created_by when possible,
    then set created_by/updated_by from users.username via approver_id (UI reads updatedBy).
    """
    # Fix null approver_id using legacy created_by UUID when it matches a user
    # (handled in backfill_table for audit cols; here use existing approver_id)
    dc.execute(
        """
        UPDATE payment_request_approvals a
        SET created_by = u.username,
            updated_by = u.username
        FROM users u
        WHERE u.id = a.approver_id
          AND u.is_deleted = false
          AND (
            a.created_by = 'legacy-migrate'
            OR a.updated_by = 'legacy-migrate'
            OR a.created_by ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
            OR a.updated_by ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
          )
        """
    )
    return dc.rowcount


def backfill_approvals_approver_id(sc, dc, user_by_id: dict[uuid.UUID, str]) -> int:
    """Set null approver_id from legacy PAY_APPROVAL_HISTORY.created_by."""
    sc.execute(
        """
        SELECT id, created_by
        FROM ies."PAY_APPROVAL_HISTORY"
        WHERE created_by IS NOT NULL
        """
    )
    legacy = {as_uuid(r["id"]): as_uuid(r["created_by"]) for r in sc.fetchall()}
    dc.execute(
        """
        SELECT id::text FROM payment_request_approvals
        WHERE approver_id IS NULL
        """
    )
    batch: list[tuple] = []
    for (tid,) in dc.fetchall():
        aid = as_uuid(tid)
        uid = legacy.get(aid) if aid else None
        if uid is None or uid not in user_by_id:
            continue
        batch.append((str(uid), str(aid)))
    if batch:
        psycopg2.extras.execute_batch(
            dc,
            """
            UPDATE payment_request_approvals
            SET approver_id = %s::uuid
            WHERE id = %s::uuid AND approver_id IS NULL
            """,
            batch,
            page_size=500,
        )
    return len(batch)


def backfill_stock_from_parents(dc) -> tuple[int, int]:
    dc.execute(
        """
        UPDATE warehouse_stock_transactions wst
        SET created_by = wir.created_by,
            updated_by = wir.created_by
        FROM warehouse_inbound_receipts wir
        WHERE wst.reference_type = 'LEGACY_IMPORT_REQUEST'
          AND wst.reference_id = wir.id
          AND wst.created_by = 'legacy-migrate'
          AND wir.created_by IS NOT NULL
          AND wir.created_by <> 'legacy-migrate'
        """
    )
    inbound = dc.rowcount
    dc.execute(
        """
        UPDATE warehouse_stock_transactions wst
        SET created_by = wo.created_by,
            updated_by = wo.created_by
        FROM warehouse_outbounds wo
        WHERE wst.reference_type = 'LEGACY_EXPORT_REQUEST'
          AND wst.reference_id = wo.id
          AND wst.created_by = 'legacy-migrate'
          AND wo.created_by IS NOT NULL
          AND wo.created_by <> 'legacy-migrate'
        """
    )
    outbound = dc.rowcount
    return inbound, outbound


def backfill_inbound_lines_from_parent(dc) -> int:
    dc.execute(
        """
        UPDATE warehouse_inbound_receipt_lines l
        SET created_by = r.created_by,
            updated_by = COALESCE(NULLIF(r.updated_by, 'legacy-migrate'), r.created_by)
        FROM warehouse_inbound_receipts r
        WHERE l.receipt_id = r.id
          AND (
            l.created_by = 'legacy-migrate'
            OR l.updated_by = 'legacy-migrate'
          )
          AND r.created_by IS NOT NULL
          AND r.created_by <> 'legacy-migrate'
        """
    )
    return dc.rowcount


def backfill_payment_children_from_parent(dc) -> dict[str, int]:
    results: dict[str, int] = {}
    for table, fk in (
        ("payment_request_purchase_order_lines", "payment_request_id"),
        ("payment_request_extra_fees", "payment_request_id"),
    ):
        dc.execute(
            f"""
            UPDATE {table} c
            SET created_by = pr.created_by,
                updated_by = COALESCE(NULLIF(pr.updated_by, 'legacy-migrate'), pr.created_by)
            FROM payment_requests pr
            WHERE c.{fk} = pr.id
              AND (
                c.created_by = 'legacy-migrate'
                OR c.updated_by = 'legacy-migrate'
              )
              AND pr.created_by IS NOT NULL
              AND pr.created_by <> 'legacy-migrate'
            """
        )
        results[table] = dc.rowcount

    # item_documents parent is payment_request_purchase_order_lines
    dc.execute(
        """
        UPDATE payment_request_item_documents d
        SET created_by = p.created_by,
            updated_by = COALESCE(NULLIF(p.updated_by, 'legacy-migrate'), p.created_by)
        FROM payment_request_purchase_order_lines p
        WHERE d.payment_request_item_id = p.id
          AND (
            d.created_by = 'legacy-migrate'
            OR d.updated_by = 'legacy-migrate'
          )
          AND p.created_by IS NOT NULL
          AND p.created_by <> 'legacy-migrate'
        """
    )
    results["payment_request_item_documents"] = dc.rowcount
    return results


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

    try:
        with src.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as sc, dst.cursor() as dc:
            dc.execute("SELECT id::text, username FROM users WHERE is_deleted = false")
            user_by_id = {
                as_uuid(r[0]): r[1] for r in dc.fetchall() if as_uuid(r[0])
            }
            print(f"users loaded={len(user_by_id)}")

            for target, legacy in ENTITY_MAPPINGS:
                print(f"== {target} ==")
                try:
                    updated, skipped = backfill_table(
                        sc, dc, target, legacy, user_by_id
                    )
                    dst.commit()
                    print(f"  updated={updated} skipped={skipped}")
                except Exception as e:
                    dst.rollback()
                    print(f"  FAILED: {e}", file=sys.stderr)

            print("== payment_requests via requestor_id ==")
            n = backfill_payment_via_requestor(dc)
            dst.commit()
            print(f"  updated={n}")

            print("== payment_request_approvals approver_id ==")
            n = backfill_approvals_approver_id(sc, dc, user_by_id)
            dst.commit()
            print(f"  updated={n}")

            print("== payment_request_approvals via approver_id ==")
            n = backfill_approvals_via_approver(dc)
            dst.commit()
            print(f"  updated={n}")

            print("== warehouse_stock_transactions from parents ==")
            inbound, outbound = backfill_stock_from_parents(dc)
            dst.commit()
            print(f"  inbound={inbound} outbound={outbound}")

            print("== warehouse_inbound_receipt_lines from parent ==")
            n = backfill_inbound_lines_from_parent(dc)
            dst.commit()
            print(f"  updated={n}")

            print("== payment children from parent ==")
            child = backfill_payment_children_from_parent(dc)
            dst.commit()
            for k, v in child.items():
                print(f"  {k}={v}")

            print("== validation ==")
            for table in (
                "orders",
                "order_lines",
                "purchase_orders",
                "payment_requests",
                "payment_request_approvals",
                "warehouse_outbounds",
                "warehouse_inbound_receipts",
                "warehouse_inbound_receipt_lines",
                "warehouse_stock_transactions",
                "payment_request_purchase_order_lines",
                "payment_request_item_documents",
                "payment_request_extra_fees",
            ):
                dc.execute(
                    f"""
                    SELECT
                      COUNT(*) FILTER (WHERE created_by = 'legacy-migrate') AS lm,
                      COUNT(*) FILTER (
                        WHERE created_by ~* '^[0-9a-f]{{8}}-[0-9a-f]{{4}}-[0-9a-f]{{4}}-[0-9a-f]{{4}}-[0-9a-f]{{12}}$'
                      ) AS uuid_like,
                      COUNT(*) AS total
                    FROM {table}
                    """
                )
                lm, uuid_like, total = dc.fetchone()
                print(f"  {table}: total={total} legacy-migrate={lm} uuid-like={uuid_like}")

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

#!/usr/bin/env python3
"""Migrate payment child rows + warehouse inbound receipts/lines.

Prereq: payment_requests, purchase_order_lines already loaded (run_remaining_etl.py).
"""
from __future__ import annotations

import os
import sys
import uuid
from decimal import Decimal, ROUND_HALF_UP
from typing import Any, Optional

import psycopg2
import psycopg2.extras

ADMIN_USER = "d0d0d0d0-0000-4000-8000-000000000001"


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


def as_id(s: Any) -> Optional[str]:
    """Normalize DB uuid/text to lowercase string id."""
    if s is None:
        return None
    try:
        return str(uuid.UUID(str(s)))
    except Exception:
        return None


def audit_user(user_by_id: dict, uid: Any, fallback: str = "legacy-migrate") -> str:
    """Map legacy SYS_USER UUID → target users.username."""
    key = as_id(uid) if uid is not None and not isinstance(uid, str) else uid
    if key is None and uid is not None:
        key = as_id(uid)
    if key is None:
        return fallback
    # user_by_id may be keyed by UUID objects or strings
    return user_by_id.get(key) or user_by_id.get(uid) or fallback


def main() -> int:
    legacy_url = os.environ["LEGACY_DATABASE_URL"]
    target_url = os.environ["TARGET_DATABASE_URL"]
    src = psycopg2.connect(legacy_url)
    dst = psycopg2.connect(target_url)
    src.autocommit = False
    dst.autocommit = False

    try:
        with src.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as sc, dst.cursor() as dc:
            dc.execute("SELECT id FROM payment_requests")
            payments = {as_id(r[0]) for r in dc.fetchall()}
            dc.execute("SELECT id FROM purchase_order_lines")
            polines = {as_id(r[0]) for r in dc.fetchall()}
            dc.execute("SELECT id FROM users")
            users = {as_id(r[0]) for r in dc.fetchall()}
            dc.execute("SELECT id, username FROM users WHERE is_deleted = false")
            user_by_id = {as_id(r[0]): r[1] for r in dc.fetchall()}
            dc.execute(
                """
                SELECT payment_request_id, purchase_order_line_id, id
                FROM payment_request_purchase_order_lines
                WHERE is_deleted = false
                """
            )
            existing_prpol = {
                (as_id(r[0]), as_id(r[1])): as_id(r[2]) for r in dc.fetchall()
            }
            dc.execute("SELECT id FROM payment_request_extra_fees")
            existing_fees = {as_id(r[0]) for r in dc.fetchall()}
            dc.execute("SELECT id FROM payment_request_approvals")
            existing_appr = {as_id(r[0]) for r in dc.fetchall()}
            dc.execute("SELECT id FROM warehouse_inbound_receipts")
            existing_rcpt = {as_id(r[0]) for r in dc.fetchall()}
            dc.execute("SELECT id FROM warehouse_inbound_receipt_lines")
            existing_rlines = {as_id(r[0]) for r in dc.fetchall()}

            # ---------- PR ↔ PO lines from PAY_PAYMENT_LINE.poline_ids ----------
            print("== payment_request_purchase_order_lines ==")
            sc.execute(
                """
                SELECT id, req_payment_id, document_no, document_type, poline_ids, is_fee,
                       total, total_to_pay, created_at, updated_at, deleted_at,
                       created_by, updated_by
                FROM ies."PAY_PAYMENT_LINE"
                WHERE COALESCE(is_fee, false) = false
                  AND poline_ids IS NOT NULL
                  AND cardinality(poline_ids) > 0
                """
            )
            ins_prpol = skip_prpol = 0
            prpol_by_pay_pol: dict[tuple, uuid.UUID] = dict(existing_prpol)
            for pl in sc.fetchall():
                pay_id = as_id(pl["req_payment_id"])
                if pay_id not in payments:
                    skip_prpol += 1
                    continue
                ids = [as_id(x) for x in (pl["poline_ids"] or [])]
                ids = [i for i in ids if i and i in polines]
                if not ids:
                    skip_prpol += 1
                    continue
                amount = money(pl["total_to_pay"] or pl["total"])
                share = (amount / Decimal(len(ids))).quantize(Decimal("0.01"), ROUND_HALF_UP)
                docs = pl["document_type"] or pl["document_no"] or "LEGACY"
                created_by = audit_user(user_by_id, pl["created_by"])
                updated_by = audit_user(user_by_id, pl["updated_by"], created_by)
                for i, pol_id in enumerate(ids):
                    key = (pay_id, pol_id)
                    if key in prpol_by_pay_pol:
                        skip_prpol += 1
                        continue
                    # use payment_line id for first poline, else new
                    row_id = as_id(pl["id"]) if i == 0 else str(uuid.uuid4())
                    if i == 0:
                        dc.execute(
                            "SELECT 1 FROM payment_request_purchase_order_lines WHERE id=%s",
                            (row_id,),
                        )
                        if dc.fetchone():
                            row_id = str(uuid.uuid4())
                    req_amt = share if i < len(ids) - 1 else (amount - share * (len(ids) - 1))
                    dc.execute(
                        """
                        INSERT INTO payment_request_purchase_order_lines (
                          id, payment_request_id, purchase_order_line_id, selected_documents,
                          requested_amount, paid_amount, note,
                          created_at, updated_at, created_by, updated_by, is_deleted
                        ) VALUES (
                          %s,%s,%s,%s,%s,0,%s,
                          COALESCE(%s,NOW()),COALESCE(%s,NOW()),%s,%s,%s
                        )
                        ON CONFLICT (id) DO NOTHING
                        """,
                        (
                            row_id,
                            pay_id,
                            pol_id,
                            str(docs)[:500],
                            req_amt,
                            pl["document_no"],
                            pl["created_at"],
                            pl["updated_at"] or pl["created_at"],
                            created_by,
                            updated_by,
                            pl["deleted_at"] is not None,
                        ),
                    )
                    if dc.rowcount:
                        ins_prpol += 1
                        prpol_by_pay_pol[key] = row_id
                        if pl["document_type"]:
                            dtype = str(pl["document_type"])[:50]
                            dc.execute(
                                """
                                SELECT 1 FROM payment_request_item_documents
                                WHERE payment_request_item_id=%s AND document_type=%s
                                  AND is_deleted=false
                                """,
                                (row_id, dtype),
                            )
                            if not dc.fetchone():
                                dc.execute(
                                    """
                                    INSERT INTO payment_request_item_documents (
                                      id, payment_request_item_id, document_type,
                                      created_at, updated_at, created_by, updated_by, is_deleted
                                    ) VALUES (
                                      %s,%s,%s,COALESCE(%s,NOW()),COALESCE(%s,NOW()),
                                      %s,%s,false
                                    )
                                    """,
                                    (
                                        str(uuid.uuid4()),
                                        row_id,
                                        dtype,
                                        pl["created_at"],
                                        pl["updated_at"] or pl["created_at"],
                                        created_by,
                                        updated_by,
                                    ),
                                )
                    else:
                        skip_prpol += 1
            print(f"  inserted={ins_prpol} skipped={skip_prpol}")
            dst.commit()

            # ---------- Extra fees from is_fee lines + FEE_MANAGEMENT ----------
            print("== payment_request_extra_fees ==")
            ins_fee = skip_fee = 0
            sc.execute(
                """
                SELECT id, req_payment_id, document_no, document_type, total, total_to_pay,
                       created_at, updated_at, deleted_at, created_by, updated_by
                FROM ies."PAY_PAYMENT_LINE"
                WHERE COALESCE(is_fee, false) = true
                """
            )
            for pl in sc.fetchall():
                fid = as_id(pl["id"])
                pay_id = as_id(pl["req_payment_id"])
                if fid in existing_fees:
                    skip_fee += 1
                    continue
                if pay_id not in payments:
                    skip_fee += 1
                    continue
                name = (pl["document_type"] or pl["document_no"] or "LEGACY_FEE")[:100]
                created_by = audit_user(user_by_id, pl["created_by"])
                updated_by = audit_user(user_by_id, pl["updated_by"], created_by)
                dc.execute(
                    """
                    INSERT INTO payment_request_extra_fees (
                      id, payment_request_id, fee_name, fee_type, amount, note,
                      created_at, updated_at, created_by, updated_by, is_deleted
                    ) VALUES (
                      %s,%s,%s,%s,%s,%s,
                      COALESCE(%s,NOW()),COALESCE(%s,NOW()),%s,%s,%s
                    )
                    ON CONFLICT (id) DO NOTHING
                    """,
                    (
                        fid,
                        pay_id,
                        name,
                        "LEGACY_PAY_LINE",
                        money(pl["total_to_pay"] or pl["total"]),
                        pl["document_no"],
                        pl["created_at"],
                        pl["updated_at"] or pl["created_at"],
                        created_by,
                        updated_by,
                        pl["deleted_at"] is not None,
                    ),
                )
                if dc.rowcount:
                    ins_fee += 1
                    existing_fees.add(fid)
                else:
                    skip_fee += 1

            sc.execute(
                """
                SELECT id, req_payment_id, fee_name, fee_description, fee_value, fee_unit,
                       note, created_at, updated_at, deleted_at, created_by, updated_by
                FROM ies."FEE_MANAGEMENT"
                WHERE req_payment_id IS NOT NULL
                """
            )
            for fee in sc.fetchall():
                fid = as_id(fee["id"])
                pay_id = as_id(fee["req_payment_id"])
                if fid in existing_fees:
                    skip_fee += 1
                    continue
                if pay_id not in payments:
                    skip_fee += 1
                    continue
                created_by = audit_user(user_by_id, fee["created_by"])
                updated_by = audit_user(user_by_id, fee["updated_by"], created_by)
                dc.execute(
                    """
                    INSERT INTO payment_request_extra_fees (
                      id, payment_request_id, fee_name, fee_type, amount, note,
                      created_at, updated_at, created_by, updated_by, is_deleted
                    ) VALUES (
                      %s,%s,%s,%s,%s,%s,
                      COALESCE(%s,NOW()),COALESCE(%s,NOW()),%s,%s,%s
                    )
                    ON CONFLICT (id) DO NOTHING
                    """,
                    (
                        fid,
                        pay_id,
                        (fee["fee_name"] or "FEE")[:100],
                        (fee["fee_unit"] or "LEGACY_FEE")[:50],
                        money(fee["fee_value"]),
                        notes_text(fee["note"]) or fee["fee_description"],
                        fee["created_at"],
                        fee["updated_at"] or fee["created_at"],
                        created_by,
                        updated_by,
                        fee["deleted_at"] is not None,
                    ),
                )
                if dc.rowcount:
                    ins_fee += 1
                    existing_fees.add(fid)
                else:
                    skip_fee += 1
            print(f"  inserted={ins_fee} skipped={skip_fee}")
            dst.commit()

            # ---------- Approvals ----------
            print("== payment_request_approvals ==")
            sc.execute(
                """
                SELECT id, req_payment_id, approved, note, reason, created_by, updated_by,
                       created_at, updated_at, deleted_at
                FROM ies."PAY_APPROVAL_HISTORY"
                ORDER BY req_payment_id, created_at NULLS LAST
                """
            )
            level_counter: dict = {}
            ins_ap = skip_ap = 0
            for a in sc.fetchall():
                aid = as_id(a["id"])
                pay_id = as_id(a["req_payment_id"])
                if aid in existing_appr:
                    skip_ap += 1
                    continue
                if pay_id not in payments:
                    skip_ap += 1
                    continue
                level_counter[pay_id] = level_counter.get(pay_id, 0) + 1
                level = level_counter[pay_id]
                status = "APPROVED" if a["approved"] else "REJECTED"
                approver = as_id(a["created_by"])
                if approver not in users:
                    approver = None
                created_by = audit_user(user_by_id, a["created_by"])
                updated_by = audit_user(user_by_id, a["updated_by"], created_by)
                dc.execute(
                    """
                    INSERT INTO payment_request_approvals (
                      id, payment_request_id, approval_level, approval_role, approver_id,
                      status, approved_at, rejection_reason, note,
                      created_at, updated_at, created_by, updated_by, is_deleted
                    ) VALUES (
                      %s,%s,%s,'LEGACY',%s,%s,
                      CASE WHEN %s THEN COALESCE(%s,NOW()) ELSE NULL END,
                      %s,%s,
                      COALESCE(%s,NOW()),COALESCE(%s,NOW()),%s,%s,%s
                    )
                    ON CONFLICT (id) DO NOTHING
                    """,
                    (
                        aid,
                        pay_id,
                        level,
                        approver,
                        status,
                        a["approved"],
                        a["created_at"],
                        a["reason"] if not a["approved"] else None,
                        notes_text(a["note"]),
                        a["created_at"],
                        a["updated_at"] or a["created_at"],
                        created_by,
                        updated_by,
                        a["deleted_at"] is not None,
                    ),
                )
                if dc.rowcount:
                    ins_ap += 1
                    existing_appr.add(aid)
                else:
                    skip_ap += 1
            print(f"  inserted={ins_ap} skipped={skip_ap}")
            dst.commit()

            # ---------- Inbound receipts ----------
            print("== warehouse_inbound_receipts ==")
            # Best payment link via PO line → PO → PAY_PAYMENT
            sc.execute(
                """
                WITH ir_pay AS (
                  SELECT DISTINCT ON (d.nhapkho_id)
                         d.nhapkho_id AS import_id,
                         p.id AS payment_id
                  FROM ies."IMPORT_REQUEST_DETAIL" d
                  JOIN ies."PO_PURCHASE_ORDER_LINE" pol ON pol.id = d.purchase_order_line_id
                  JOIN ies."PAY_PAYMENT" p
                    ON p.purchase_order_id = pol.purchase_order_id
                   AND p.deleted_at IS NULL
                  WHERE d.purchase_order_line_id IS NOT NULL
                    AND d.nhapkho_id IS NOT NULL
                  ORDER BY d.nhapkho_id, p.created_at NULLS LAST
                )
                SELECT ir.id, ir.import_request_number, ir.import_date, ir.note, ir.status::text AS status,
                       ir.exchange_rate, ir.total_price, ir.total_price_include_tax,
                       ir.created_at, ir.updated_at, ir.deleted_at, ir.approved_time,
                       ir.created_by, ir.updated_by, ip.payment_id
                FROM ies."IMPORT_REQUEST" ir
                LEFT JOIN ir_pay ip ON ip.import_id = ir.id
                """
            )
            ins_r = skip_r = linked_r = 0
            for ir in sc.fetchall():
                rid = as_id(ir["id"])
                if rid in existing_rcpt:
                    skip_r += 1
                    continue
                pay_id = as_id(ir["payment_id"])
                if pay_id not in payments:
                    pay_id = None
                if pay_id:
                    linked_r += 1
                status = (ir["status"] or "DRAFT")[:50]
                if status == "APPROVED" or ir["approved_time"]:
                    status = "APPROVED"
                num = (ir["import_request_number"] or rid)[:50]
                recv = (ir["import_date"] or ir["created_at"])
                recv_date = recv.date() if recv else None
                fx_raw = Decimal(str(ir["exchange_rate"] or 1))
                # NUMERIC(15,6) absolute value < 1e9
                if fx_raw.copy_abs() >= Decimal("1000000000"):
                    fx_raw = Decimal("1")
                fx = fx_raw.quantize(Decimal("0.000001"), ROUND_HALF_UP)
                created_by = audit_user(user_by_id, ir["created_by"])
                updated_by = audit_user(user_by_id, ir["updated_by"], created_by)
                dc.execute(
                    """
                    INSERT INTO warehouse_inbound_receipts (
                      id, payment_request_id, currency, exchange_rate, fee_amount,
                      real_bill_amount, bill_on_paper_amount, note, status,
                      approval_levels, current_approval_level, inventory_posted_at,
                      receipt_number, received_date,
                      created_at, updated_at, created_by, updated_by, is_deleted
                    ) VALUES (
                      %s,%s,'VND',%s,0,%s,%s,%s,%s,0,0,
                      %s,%s,%s,
                      COALESCE(%s,NOW()),COALESCE(%s,NOW()),%s,%s,%s
                    )
                    ON CONFLICT (id) DO NOTHING
                    """,
                    (
                        rid,
                        pay_id,
                        fx,
                        money(ir["total_price"]),
                        money(ir["total_price_include_tax"]),
                        notes_text(ir["note"]),
                        status,
                        ir["approved_time"],
                        num,
                        recv_date,
                        ir["created_at"],
                        ir["updated_at"] or ir["created_at"],
                        created_by,
                        updated_by,
                        ir["deleted_at"] is not None,
                    ),
                )
                if dc.rowcount:
                    ins_r += 1
                    existing_rcpt.add(rid)
                else:
                    skip_r += 1
            print(f"  inserted={ins_r} skipped={skip_r} with_payment_link={linked_r}")
            dst.commit()

            # ---------- Inbound lines ----------
            print("== warehouse_inbound_receipt_lines ==")
            sc.execute(
                """
                SELECT id, nhapkho_id, purchase_order_line_id, quantity, tax, tax_amount,
                       bill_on_paper, note, created_at, updated_at, deleted_at
                FROM ies."IMPORT_REQUEST_DETAIL"
                WHERE nhapkho_id IS NOT NULL
                """
            )
            ins_rl = skip_rl = 0
            for d in sc.fetchall():
                did = as_id(d["id"])
                rid = as_id(d["nhapkho_id"])
                if did in existing_rlines:
                    skip_rl += 1
                    continue
                if rid not in existing_rcpt:
                    skip_rl += 1
                    continue
                pol_id = as_id(d["purchase_order_line_id"])
                if pol_id not in polines:
                    pol_id = None
                prpol_id = None
                if pol_id:
                    dc.execute(
                        "SELECT payment_request_id FROM warehouse_inbound_receipts WHERE id=%s",
                        (rid,),
                    )
                    row = dc.fetchone()
                    pay_id = as_id(row[0]) if row else None
                    if pay_id:
                        prpol_id = prpol_by_pay_pol.get((pay_id, pol_id))
                q = qty(d["quantity"])
                # IMPORT_REQUEST_DETAIL has no created_by/updated_by — inherit receipt creator if present
                dc.execute(
                    "SELECT created_by, updated_by FROM warehouse_inbound_receipts WHERE id=%s",
                    (rid,),
                )
                parent = dc.fetchone()
                created_by = (parent[0] if parent and parent[0] else None) or "legacy-migrate"
                updated_by = (parent[1] if parent and parent[1] else None) or created_by
                dc.execute(
                    """
                    INSERT INTO warehouse_inbound_receipt_lines (
                      id, receipt_id, payment_request_purchase_order_line_id, purchase_order_line_id,
                      quantity_expected, quantity_received, tax_percent, tax_included,
                      bill_on_paper, line_note,
                      created_at, updated_at, created_by, updated_by, is_deleted
                    ) VALUES (
                      %s,%s,%s,%s,%s,%s,%s,false,%s,%s,
                      COALESCE(%s,NOW()),COALESCE(%s,NOW()),%s,%s,%s
                    )
                    ON CONFLICT (id) DO NOTHING
                    """,
                    (
                        did,
                        rid,
                        prpol_id,
                        pol_id,
                        q,
                        q,
                        rate(d["tax"]),
                        d["bill_on_paper"],
                        notes_text(d["note"]),
                        d["created_at"],
                        d["updated_at"] or d["created_at"],
                        created_by,
                        updated_by,
                        d["deleted_at"] is not None,
                    ),
                )
                if dc.rowcount:
                    ins_rl += 1
                    existing_rlines.add(did)
                else:
                    skip_rl += 1
            print(f"  inserted={ins_rl} skipped={skip_rl}")
            dst.commit()

            # Relink file_infos warehouse_inbound_receipt_id from SYS_MEDIA
            print("== re-link file_infos.warehouse_inbound_receipt_id ==")
            sc.execute(
                """
                SELECT id, import_request_document_id
                FROM core."SYS_MEDIA"
                WHERE import_request_document_id IS NOT NULL
                """
            )
            linked = 0
            for m in sc.fetchall():
                rid = as_id(m["import_request_document_id"])
                mid = as_id(m["id"])
                if rid not in existing_rcpt:
                    continue
                dc.execute(
                    """
                    UPDATE file_infos
                    SET warehouse_inbound_receipt_id = %s
                    WHERE id = %s AND warehouse_inbound_receipt_id IS NULL
                    """,
                    (rid, mid),
                )
                linked += dc.rowcount
            print(f"  updated={linked}")
            dst.commit()

        src.commit()
        print("COMMIT OK")
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

#!/usr/bin/env python3
"""Migrate remaining commercial domains: orders/lines, POs/lines, payments, inventory, outbounds.

Connects to cnh_legacy + cnh_wms via env:
  LEGACY_DATABASE_URL / TARGET_DATABASE_URL (or PG* + db names)
  PGPASSWORD / DB_USERNAME / DB_PASSWORD from BE .env
"""
from __future__ import annotations

import json
import os
import re
import sys
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path
from typing import Any, Optional

import psycopg2
import psycopg2.extras

ADMIN_USER = "d0d0d0d0-0000-4000-8000-000000000001"

PAY_STATUS = {
    "DRAFT": "DRAFT",
    "OPEN": "SUBMITTED",
    "IN_PROGRESS": "PENDING_ACC_APPROVAL",
    "APPROVED": "APPROVED",
    "PAID": "PAID",
    "REJECTED": "REJECTED",
    "CANCELLED": "CANCELLED",
    "CLOSED": "PAID",
    "REQUIRED_UPDATE": "SUBMITTED",
    "RE_OPEN": "SUBMITTED",
}

ORDER_STATUS_KEEP = {
    "DRAFT",
    "OPEN",
    "CLOSED_PURCHASING",
    "CLOSED_FOR_INVOICING",
    "CLOSED_FOR_SHIPPING",
    "CLOSED_COMPLETE",
    "CANCELLED",
    "PAID",
    "PARTIALLY_PAID",
    "PENDING",
}


def money(v: Any) -> Decimal:
    if v is None:
        return Decimal("0")
    d = Decimal(str(v))
    return d.quantize(Decimal("1"), rounding=ROUND_HALF_UP)


def compute_total_vnd(
    total_price: Any, total_price_vnd: Any, currency: Any, exchange_rate: Any
) -> Optional[Decimal]:
    if total_price_vnd is not None:
        return money(total_price_vnd)
    if total_price is None:
        return None
    tp = money(total_price)
    cur = (str(currency or "").strip().upper() or "VND")
    if cur in ("", "VND", "VNĐ"):
        return tp
    try:
        rate_v = Decimal(str(exchange_rate)) if exchange_rate is not None else None
    except Exception:
        rate_v = None
    if rate_v is not None and rate_v > 0:
        return (tp * rate_v).quantize(Decimal("1"), rounding=ROUND_HALF_UP)
    return tp


def qty(v: Any) -> Decimal:
    if v is None:
        return Decimal("0")
    return Decimal(str(v)).quantize(Decimal("0.001"), rounding=ROUND_HALF_UP)


def rate(v: Any) -> Decimal:
    if v is None:
        return Decimal("0")
    return Decimal(str(v)).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)


def parse_code(code: str) -> tuple[str, int]:
    """OD_2024_07.019 → ('OD_2024_07', 19)."""
    code = (code or "").strip()
    if "." in code:
        prefix, num = code.rsplit(".", 1)
        return prefix[:10], int(re.sub(r"\D", "", num) or "0")
    digits = re.findall(r"\d+", code)
    return code[:10] or "LEGACY", int(digits[-1]) if digits else 0


def notes_text(note: Any) -> Optional[str]:
    if note is None:
        return None
    if isinstance(note, (list, tuple)):
        return "\n".join(str(x) for x in note if x)
    return str(note)


def audit_user(user_by_id: dict, uid: Any, fallback: str = "legacy-migrate") -> str:
    """Map legacy SYS_USER UUID → target users.username (app stores usernames, not UUIDs)."""
    if uid is None:
        return fallback
    return user_by_id.get(uid) or fallback


def connect(url: str):
    return psycopg2.connect(url)


def main() -> int:
    legacy_url = os.environ.get("LEGACY_DATABASE_URL")
    target_url = os.environ.get("TARGET_DATABASE_URL")
    if not legacy_url or not target_url:
        print("LEGACY_DATABASE_URL and TARGET_DATABASE_URL required", file=sys.stderr)
        return 1

    src = connect(legacy_url)
    dst = connect(target_url)
    src.autocommit = False
    dst.autocommit = False

    try:
        with src.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as sc, dst.cursor() as dc:
            # Lookups on target
            dc.execute("SELECT id FROM customers")
            customers = {r[0] for r in dc.fetchall()}
            dc.execute("SELECT id FROM customer_addresses")
            addresses = {r[0] for r in dc.fetchall()}
            dc.execute("SELECT id FROM products")
            products = {r[0] for r in dc.fetchall()}
            dc.execute("SELECT id FROM vendors")
            vendors = {r[0] for r in dc.fetchall()}
            dc.execute("SELECT id FROM users")
            users = {r[0] for r in dc.fetchall()}
            dc.execute("SELECT id, username FROM users WHERE is_deleted = false")
            user_by_id = {r[0]: r[1] for r in dc.fetchall()}
            dc.execute(
                "SELECT order_prefix, order_number FROM orders WHERE is_deleted = false"
            )
            existing_order_keys = {(r[0], r[1]) for r in dc.fetchall()}
            dc.execute("SELECT id FROM orders")
            existing_order_ids = {r[0] for r in dc.fetchall()}
            dc.execute("SELECT id FROM order_lines")
            existing_line_ids = {r[0] for r in dc.fetchall()}
            dc.execute("SELECT id FROM purchase_orders")
            existing_po_ids = {r[0] for r in dc.fetchall()}
            dc.execute("SELECT id FROM purchase_order_lines")
            existing_pol_ids = {r[0] for r in dc.fetchall()}
            dc.execute("SELECT id FROM payment_requests")
            existing_pay_ids = {r[0] for r in dc.fetchall()}
            dc.execute(
                "SELECT request_number FROM payment_requests WHERE is_deleted = false"
            )
            existing_pay_nums = {r[0] for r in dc.fetchall()}
            dc.execute("SELECT id FROM warehouse_inventory")
            existing_inv_ids = {r[0] for r in dc.fetchall()}
            dc.execute(
                "SELECT product_id FROM warehouse_inventory WHERE is_deleted = false"
            )
            existing_inv_products = {r[0] for r in dc.fetchall()}
            dc.execute("SELECT id FROM warehouse_outbounds")
            existing_ob_ids = {r[0] for r in dc.fetchall()}

            # ---------- ORDERS ----------
            print("== orders ==")
            sc.execute(
                """
                SELECT id, sale_order_code, contract_number, ngay_tao, customer_id,
                       customer_address_id, status::text AS status, note,
                       total_before_tax, total_price, created_at, updated_at, deleted_at,
                       created_by, updated_by, _version
                FROM ies."ORD_ORDER"
                """
            )
            orders = sc.fetchall()
            ins_o = skip_o = 0
            for o in orders:
                oid = o["id"]
                if oid in existing_order_ids:
                    skip_o += 1
                    continue
                prefix, number = parse_code(o["sale_order_code"])
                if (prefix, number) in existing_order_keys:
                    # different id, same business key — skip to avoid unique violation
                    skip_o += 1
                    continue
                cust = o["customer_id"] if o["customer_id"] in customers else None
                addr = (
                    o["customer_address_id"]
                    if o["customer_address_id"] in addresses
                    else None
                )
                status = o["status"] if o["status"] in ORDER_STATUS_KEEP else "DRAFT"
                total = money(o["total_before_tax"] or o["total_price"])
                final = money(o["total_price"] or o["total_before_tax"])
                order_date = (o["ngay_tao"] or o["created_at"] or o["updated_at"]).date()
                contract = (o["contract_number"] or o["sale_order_code"] or "UNKNOWN")[:200]
                created_by = audit_user(user_by_id, o["created_by"])
                updated_by = audit_user(user_by_id, o["updated_by"], created_by)
                dc.execute(
                    """
                    INSERT INTO orders (
                      id, version, order_number, order_prefix, customer_id, customer_address_id,
                      contract_number, order_date, delivery_date, status,
                      total_amount, discount_amount, is_included_tax, tax_rate, tax_amount, final_amount,
                      notes, created_at, updated_at, created_by, updated_by, is_deleted
                    ) VALUES (
                      %s,%s,%s,%s,%s,%s,%s,%s,NULL,%s,%s,0,false,0,0,%s,%s,%s,%s,%s,%s,%s
                    )
                    ON CONFLICT (id) DO NOTHING
                    """,
                    (
                        oid,
                        int(o["_version"] or 1),
                        number,
                        prefix,
                        cust,
                        addr,
                        contract,
                        order_date,
                        status[:20],
                        total,
                        final,
                        notes_text(o["note"]),
                        o["created_at"],
                        o["updated_at"],
                        created_by,
                        updated_by,
                        o["deleted_at"] is not None,
                    ),
                )
                if dc.rowcount:
                    ins_o += 1
                    existing_order_ids.add(oid)
                    existing_order_keys.add((prefix, number))
                else:
                    skip_o += 1
            print(f"  inserted={ins_o} skipped={skip_o}")
            dst.commit()

            # ---------- ORDER LINES ----------
            print("== order_lines ==")
            sc.execute(
                """
                SELECT id, order_id, product_id, vendor_id, cust_product_code, cust_product_name,
                       cust_vendor_code, cust_vendor_name, quantity, uom_1, unit_price,
                       is_tax_included, tax, total_before_tax, total_price, note, ref,
                       created_at, updated_at, deleted_at, created_by, updated_by, _version
                FROM ies."ORD_ORDER_LINE"
                """
            )
            ins_ol = 0
            skip_ol = {
                "existing_id": 0,
                "missing_order": 0,
                "missing_product": 0,
                "conflict": 0,
            }
            for ol in sc.fetchall():
                if ol["id"] in existing_line_ids:
                    skip_ol["existing_id"] += 1
                    continue
                if ol["order_id"] not in existing_order_ids:
                    skip_ol["missing_order"] += 1
                    continue
                if not ol["product_id"] or ol["product_id"] not in products:
                    skip_ol["missing_product"] += 1
                    continue
                vendor = ol["vendor_id"] if ol["vendor_id"] in vendors else None
                unit = money(ol["unit_price"])
                q = qty(ol["quantity"])
                tax_rate = rate(ol["tax"])
                total = money(ol["total_price"] or ol["total_before_tax"] or (unit * q))
                tax_amt = money(total * tax_rate / Decimal("100")) if tax_rate else Decimal("0")
                created_by = audit_user(user_by_id, ol["created_by"])
                updated_by = audit_user(user_by_id, ol["updated_by"], created_by)
                dc.execute(
                    """
                    INSERT INTO order_lines (
                      id, version, order_id, product_id, vendor_id,
                      product_code_suggest, product_name_suggest, vendor_code_suggest, vendor_name_suggest,
                      quantity, unit_price, uom, discount_percent, discount_amount,
                      is_included_tax, tax_rate, tax_amount, total_amount,
                      notes, reference_note, created_at, updated_at, created_by, updated_by, is_deleted
                    ) VALUES (
                      %s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,0,0,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s
                    )
                    ON CONFLICT (id) DO NOTHING
                    """,
                    (
                        ol["id"],
                        int(ol["_version"] or 1),
                        ol["order_id"],
                        ol["product_id"],
                        vendor,
                        (ol["cust_product_code"] or "")[:200] or None,
                        (ol["cust_product_name"] or "")[:200] or None,
                        (ol["cust_vendor_code"] or "")[:200] or None,
                        (ol["cust_vendor_name"] or "")[:200] or None,
                        q,
                        unit,
                        (ol["uom_1"] or "pce")[:50],
                        bool(ol["is_tax_included"]),
                        tax_rate,
                        tax_amt,
                        total,
                        notes_text(ol["note"]),
                        ol["ref"],
                        ol["created_at"],
                        ol["updated_at"],
                        created_by,
                        updated_by,
                        ol["deleted_at"] is not None,
                    ),
                )
                if dc.rowcount:
                    ins_ol += 1
                    existing_line_ids.add(ol["id"])
                else:
                    skip_ol["conflict"] += 1
            print(f"  inserted={ins_ol} skipped={skip_ol}")
            dst.commit()

            # ---------- PURCHASE ORDERS ----------
            print("== purchase_orders ==")
            # Resolve order_id from first sale_orderline → order_lines.order_id (junction empty)
            sc.execute(
                """
                SELECT pol.purchase_order_id AS po_id,
                       (array_agg(ol.order_id ORDER BY pol.created_at NULLS LAST, pol.id))[1] AS order_id
                FROM ies."PO_PURCHASE_ORDER_LINE" pol
                JOIN ies."ORD_ORDER_LINE" ol ON ol.id = pol.sale_orderline_id
                WHERE pol.deleted_at IS NULL
                  AND pol.sale_orderline_id IS NOT NULL
                  AND ol.deleted_at IS NULL
                GROUP BY pol.purchase_order_id
                """
            )
            po_order_by_id = {
                r["po_id"]: r["order_id"]
                for r in sc.fetchall()
                if r["order_id"] in existing_order_ids
            }
            sc.execute(
                """
                SELECT id, purchase_order_number, status::text AS status, note,
                       created_at, updated_at, deleted_at, created_by, updated_by, _version
                FROM ies."PO_PURCHASE_ORDER"
                """
            )
            # Existing PO keys
            dc.execute(
                "SELECT po_prefix, po_number FROM purchase_orders WHERE is_deleted = false"
            )
            existing_po_keys = {(r[0], r[1]) for r in dc.fetchall()}
            ins_po = skip_po = 0
            for po in sc.fetchall():
                if po["id"] in existing_po_ids:
                    skip_po += 1
                    continue
                prefix, number = parse_code(po["purchase_order_number"])
                if (prefix, number) in existing_po_keys:
                    skip_po += 1
                    continue
                status = (po["status"] or "DRAFT")[:20]
                order_date = (po["created_at"] or po["updated_at"]).date()
                created_by = audit_user(user_by_id, po["created_by"])
                updated_by = audit_user(user_by_id, po["updated_by"], created_by)
                order_id = po_order_by_id.get(po["id"])
                dc.execute(
                    """
                    INSERT INTO purchase_orders (
                      id, version, po_number, po_prefix, order_id, order_date,
                      expected_delivery_date, status, notes,
                      created_at, updated_at, created_by, updated_by, is_deleted
                    ) VALUES (
                      %s,%s,%s,%s,%s,%s,NULL,%s,%s,%s,%s,%s,%s,%s
                    )
                    ON CONFLICT (id) DO NOTHING
                    """,
                    (
                        po["id"],
                        int(po["_version"] or 1),
                        number,
                        prefix,
                        order_id,
                        order_date,
                        status,
                        notes_text(po["note"]),
                        po["created_at"],
                        po["updated_at"],
                        created_by,
                        updated_by,
                        po["deleted_at"] is not None,
                    ),
                )
                if dc.rowcount:
                    ins_po += 1
                    existing_po_ids.add(po["id"])
                    existing_po_keys.add((prefix, number))
                else:
                    skip_po += 1
            print(f"  inserted={ins_po} skipped={skip_po}")
            dst.commit()

            # ---------- PO LINES ----------
            print("== purchase_order_lines ==")
            sc.execute(
                """
                SELECT id, purchase_order_id, sale_orderline_id, product_id, vendor_id, link,
                       quantity, uom_1, uom_2, unit_price, is_tax_included, tax,
                       total_before_tax, total_price, currency, exchange_rate, total_price_vnd,
                       note, i_quote, i_invoice, i_billoflanding, i_receiptwarehouse,
                       "i_trackId" AS i_track_id, i_purchasecontractnumber,
                       created_at, updated_at, deleted_at, created_by, updated_by, _version
                FROM ies."PO_PURCHASE_ORDER_LINE"
                """
            )
            ins_pol = skip_pol = 0
            for pol in sc.fetchall():
                if pol["id"] in existing_pol_ids:
                    skip_pol += 1
                    continue
                if pol["purchase_order_id"] not in existing_po_ids:
                    skip_pol += 1
                    continue
                product = pol["product_id"] if pol["product_id"] in products else None
                vendor = pol["vendor_id"] if pol["vendor_id"] in vendors else None
                sol = (
                    pol["sale_orderline_id"]
                    if pol["sale_orderline_id"] in existing_line_ids
                    else None
                )
                total_vnd = compute_total_vnd(
                    pol["total_price"],
                    pol["total_price_vnd"],
                    pol["currency"],
                    pol["exchange_rate"],
                )
                created_by = audit_user(user_by_id, pol["created_by"])
                updated_by = audit_user(user_by_id, pol["updated_by"], created_by)
                dc.execute(
                    """
                    INSERT INTO purchase_order_lines (
                      id, version, purchase_order_id, sale_orderline_id, product_id, vendor_id, link,
                      quantity, uom_1, uom_2, unit_price, is_tax_included, tax,
                      total_before_tax, total_price, currency, exchange_rate, total_price_vnd,
                      note, quote, invoice, bill_of_ladding, receipt_warehouse, track_id,
                      purchase_contract_number, created_at, updated_at, created_by, updated_by, is_deleted
                    ) VALUES (
                      %s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,
                      %s,%s,%s
                    )
                    ON CONFLICT (id) DO NOTHING
                    """,
                    (
                        pol["id"],
                        int(pol["_version"] or 1),
                        pol["purchase_order_id"],
                        sol,
                        product,
                        vendor,
                        pol["link"],
                        qty(pol["quantity"]),
                        (pol["uom_1"] or None),
                        (pol["uom_2"] or None),
                        money(pol["unit_price"]),
                        bool(pol["is_tax_included"]),
                        rate(pol["tax"]),
                        money(pol["total_before_tax"]) if pol["total_before_tax"] is not None else None,
                        money(pol["total_price"]) if pol["total_price"] is not None else None,
                        (pol["currency"] or None),
                        Decimal(str(pol["exchange_rate"])).quantize(Decimal("0.0001"))
                        if pol["exchange_rate"] is not None
                        else None,
                        total_vnd,
                        notes_text(pol["note"]),
                        pol["i_quote"],
                        pol["i_invoice"],
                        pol["i_billoflanding"],
                        pol["i_receiptwarehouse"],
                        pol["i_track_id"],
                        pol["i_purchasecontractnumber"],
                        pol["created_at"],
                        pol["updated_at"],
                        created_by,
                        updated_by,
                        pol["deleted_at"] is not None,
                    ),
                )
                if dc.rowcount:
                    ins_pol += 1
                    existing_pol_ids.add(pol["id"])
                else:
                    skip_pol += 1
            print(f"  inserted={ins_pol} skipped={skip_pol}")
            dst.commit()

            # ---------- PAYMENTS (headers only) ----------
            print("== payment_requests ==")
            sc.execute(
                """
                SELECT id, req_payment_code, vendor_id, total, total_to_pay, currency, status::text AS status,
                       note, created_by, updated_by, created_at, updated_at, deleted_at, exchange_rate,
                       already_paid, payment_rate, ben_bank_account_no, ben_bank_name, ben_name
                FROM ies."PAY_PAYMENT"
                """
            )
            ins_pay = skip_pay = 0
            for p in sc.fetchall():
                if p["id"] in existing_pay_ids:
                    skip_pay += 1
                    continue
                code = (p["req_payment_code"] or str(p["id"]))[:50]
                if code in existing_pay_nums:
                    skip_pay += 1
                    continue
                requestor = p["created_by"] if p["created_by"] in users else ADMIN_USER
                vendor = p["vendor_id"] if p["vendor_id"] in vendors else None
                status = PAY_STATUS.get(p["status"], "DRAFT")
                amount = money(p["total_to_pay"] or p["total"])
                paid = money(p["already_paid"])
                fx = Decimal(str(p["exchange_rate"] or 1)).quantize(Decimal("0.000001"))
                currency = (p["currency"] or "VND")[:3]
                created_by = audit_user(user_by_id, p["created_by"])
                updated_by = audit_user(user_by_id, p["updated_by"], created_by)
                bank_info = None
                if p["ben_bank_name"] or p["ben_bank_account_no"]:
                    bank_info = psycopg2.extras.Json(
                        {
                            "bankName": p["ben_bank_name"],
                            "accountNumber": p["ben_bank_account_no"],
                            "accountName": p["ben_name"],
                        }
                    )
                dc.execute(
                    """
                    INSERT INTO payment_requests (
                      id, request_number, request_date, requestor_id, amount, currency, purpose, status,
                      notes, vendor_id, approval_levels, current_approval_level,
                      requested_amount, fee_amount, total_amount, paid_amount, exchange_rate,
                      papers, bank_info, bank_note,
                      requested_amount_vnd, fee_amount_vnd, total_amount_vnd, paid_amount_vnd, paid_percentage,
                      created_at, updated_at, created_by, updated_by, is_deleted,
                      paid_at, paid_by
                    ) VALUES (
                      %s,%s,%s,%s,%s,%s,%s,%s,%s,%s,2,0,
                      %s,0,%s,%s,%s,
                      NULL,%s,NULL,
                      %s,0,%s,%s,%s,
                      %s,%s,%s,%s,%s,
                      CASE WHEN %s = 'PAID' THEN %s ELSE NULL END,
                      CASE WHEN %s = 'PAID' THEN %s::uuid ELSE NULL END
                    )
                    ON CONFLICT (id) DO NOTHING
                    """,
                    (
                        p["id"],
                        code,
                        p["created_at"],
                        requestor,
                        amount,
                        currency,
                        notes_text(p["note"]),
                        status,
                        notes_text(p["note"]),
                        vendor,
                        amount,
                        amount,
                        paid,
                        fx,
                        bank_info,
                        amount,
                        amount,
                        paid,
                        Decimal(str(p["payment_rate"] or 0)).quantize(Decimal("0.01"))
                        if p["payment_rate"] is not None
                        else (Decimal("100") if status == "PAID" else Decimal("0")),
                        p["created_at"],
                        p["updated_at"],
                        created_by,
                        updated_by,
                        p["deleted_at"] is not None,
                        status,
                        p["updated_at"] or p["created_at"],
                        status,
                        ADMIN_USER,
                    ),
                )
                if dc.rowcount:
                    ins_pay += 1
                    existing_pay_ids.add(p["id"])
                    existing_pay_nums.add(code)
                else:
                    skip_pay += 1
            print(f"  inserted={ins_pay} skipped={skip_pay}")

            # Relink file_infos payment_request_id where possible
            print("== re-link file_infos.payment_request_id ==")
            dc.execute(
                """
                SELECT EXISTS (
                  SELECT 1 FROM information_schema.tables
                  WHERE table_name = 'legacy_media_sync'
                )
                """
            )
            if dc.fetchone()[0]:
                dc.execute(
                    """
                    UPDATE file_infos f
                    SET payment_request_id = s.payment_request_id
                    FROM legacy_media_sync s
                    WHERE f.id = s.id
                      AND s.payment_request_id IS NOT NULL
                      AND EXISTS (SELECT 1 FROM payment_requests pr WHERE pr.id = s.payment_request_id)
                      AND f.payment_request_id IS NULL
                    """
                )
                print(f"  updated={dc.rowcount}")
            else:
                print("  legacy_media_sync missing — skip staging re-link")
            # Direct re-link from legacy SYS_MEDIA payment FKs
            sc.execute(
                """
                SELECT id,
                       COALESCE("banknote_paymentRequest_id", req_payment_id) AS pay_id
                FROM core."SYS_MEDIA"
                WHERE COALESCE("banknote_paymentRequest_id", req_payment_id) IS NOT NULL
                """
            )
            linked = 0
            for m in sc.fetchall():
                if m["pay_id"] not in existing_pay_ids:
                    continue
                dc.execute(
                    """
                    UPDATE file_infos
                    SET payment_request_id = %s
                    WHERE id = %s AND payment_request_id IS NULL
                    """,
                    (m["pay_id"], m["id"]),
                )
                linked += dc.rowcount
            print(f"  media-link updated={linked}")
            dst.commit()

            # ---------- INVENTORY (aggregate by product) ----------
            print("== warehouse_inventory ==")
            sc.execute(
                """
                SELECT product_id,
                       SUM(quantity)::numeric AS qty,
                       (array_agg(id ORDER BY created_at NULLS LAST))[1] AS id,
                       MIN(created_at) AS created_at,
                       MAX(updated_at) AS updated_at,
                       BOOL_OR(deleted_at IS NOT NULL) AS any_deleted
                FROM ies."INVENTORY"
                WHERE product_id IS NOT NULL
                GROUP BY product_id
                """
            )
            ins_inv = 0
            skip_inv = {
                "missing_product": 0,
                "existing_product": 0,
                "existing_id": 0,
                "conflict": 0,
            }
            for inv in sc.fetchall():
                if inv["product_id"] not in products:
                    skip_inv["missing_product"] += 1
                    continue
                if inv["product_id"] in existing_inv_products:
                    skip_inv["existing_product"] += 1
                    continue
                iid = inv["id"]
                if iid in existing_inv_ids:
                    # use new uuid if id collision with different product — rare
                    skip_inv["existing_id"] += 1
                    continue
                created = inv["created_at"]
                updated = inv["updated_at"] or created
                # ies.INVENTORY has no created_by/updated_by
                dc.execute(
                    """
                    INSERT INTO warehouse_inventory (
                      id, product_id, quantity_on_hand, created_at, updated_at,
                      created_by, updated_by, is_deleted
                    ) VALUES (%s,%s,%s,COALESCE(%s, NOW()),COALESCE(%s, NOW()),
                              'legacy-migrate','legacy-migrate',false)
                    ON CONFLICT (id) DO NOTHING
                    """,
                    (
                        iid,
                        inv["product_id"],
                        qty(inv["qty"]),
                        created,
                        updated,
                    ),
                )
                if dc.rowcount:
                    ins_inv += 1
                    existing_inv_ids.add(iid)
                    existing_inv_products.add(inv["product_id"])
                else:
                    skip_inv["conflict"] += 1
            print(f"  inserted={ins_inv} skipped={skip_inv}")
            dst.commit()

            # ---------- OUTBOUNDS (only when sale order exists) ----------
            print("== warehouse_outbounds ==")
            sc.execute(
                """
                SELECT id, export_request_number, export_date, total_price, description,
                       sale_order_id, note, reason, status::text AS status,
                       created_at, updated_at, deleted_at, created_by, updated_by, paper_number
                FROM ies."EXPORT_REQUEST"
                WHERE sale_order_id IS NOT NULL
                """
            )
            ins_ob = skip_ob = 0
            for ob in sc.fetchall():
                if ob["id"] in existing_ob_ids:
                    skip_ob += 1
                    continue
                if ob["sale_order_id"] not in existing_order_ids:
                    skip_ob += 1
                    continue
                # contract from order
                dc.execute(
                    "SELECT contract_number FROM orders WHERE id = %s",
                    (ob["sale_order_id"],),
                )
                row = dc.fetchone()
                contract = (row[0] if row else None) or "UNKNOWN"
                num = (ob["export_request_number"] or str(ob["id"]))[:50]
                created_by = audit_user(user_by_id, ob["created_by"])
                updated_by = audit_user(user_by_id, ob["updated_by"], created_by)
                dc.execute(
                    """
                    INSERT INTO warehouse_outbounds (
                      id, outbound_number, order_id, contract_number, outbound_reason, currency,
                      outbound_date, total_amount, tax_amount, note, status,
                      approval_levels, current_approval_level,
                      created_at, updated_at, created_by, updated_by, is_deleted
                    ) VALUES (
                      %s,%s,%s,%s,%s,'VND',%s,%s,0,%s,%s,0,0,%s,%s,%s,%s,%s
                    )
                    ON CONFLICT (id) DO NOTHING
                    """,
                    (
                        ob["id"],
                        num,
                        ob["sale_order_id"],
                        contract[:200],
                        (ob["reason"] or ob["description"] or "LEGACY_EXPORT")[:200],
                        (ob["export_date"] or ob["created_at"]).date(),
                        money(ob["total_price"]),
                        notes_text(ob["note"]) or ob["description"],
                        (ob["status"] or "DRAFT")[:50],
                        ob["created_at"],
                        ob["updated_at"],
                        created_by,
                        updated_by,
                        ob["deleted_at"] is not None,
                    ),
                )
                if dc.rowcount:
                    ins_ob += 1
                    existing_ob_ids.add(ob["id"])
                else:
                    skip_ob += 1
            print(f"  inserted={ins_ob} skipped={skip_ob}")
            dst.commit()

            # Inbound receipts: loaded by run_payment_inbound_etl.py
            print("== warehouse_inbound_receipts ==")
            print("  deferred to run_payment_inbound_etl.py")

            report = {
                "order_lines": {"inserted": ins_ol, "skipped": skip_ol},
                "warehouse_inventory": {"inserted": ins_inv, "skipped": skip_inv},
            }
            out_dir = Path(__file__).resolve().parents[1] / "out"
            out_dir.mkdir(parents=True, exist_ok=True)
            out_path = out_dir / "etl_skip_report_remaining.json"
            out_path.write_text(json.dumps(report, indent=2, default=str))
            print(f"REPORT {out_path}")

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

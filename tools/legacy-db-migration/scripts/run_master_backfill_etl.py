#!/usr/bin/env python3
"""Backfill missing master data: products, customer_addresses, and optional users.

Requires LEGACY_DATABASE_URL + TARGET_DATABASE_URL.
Idempotent: ON CONFLICT DO NOTHING / skip existing ids.
"""
from __future__ import annotations

import json
import os
import sys
import uuid
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path
from typing import Any, Optional

import psycopg2
import psycopg2.extras

# Disabled placeholder hash so row can exist for FK/audit without login.
DISABLED_PASSWORD = "$2a$12$disabledlegacyuserplaceholderhashxxxxxxu."


def money(v: Any) -> Decimal:
    if v is None:
        return Decimal("0")
    return Decimal(str(v)).quantize(Decimal("0.01"), ROUND_HALF_UP)


def clip(s: Any, n: int) -> Optional[str]:
    if s is None:
        return None
    t = str(s).strip()
    if not t:
        return None
    return t[:n]


def join_address(row: dict) -> Optional[str]:
    parts = [
        row.get("add_line_1"),
        row.get("add_line_2"),
        row.get("ward"),
        row.get("district"),
        row.get("province"),
    ]
    text = ", ".join(str(p).strip() for p in parts if p and str(p).strip())
    return text or None


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
    report: dict[str, Any] = {}

    try:
        with src.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as sc, dst.cursor() as dc:
            dc.execute("SELECT id FROM categories")
            categories = {r[0] for r in dc.fetchall()}
            dc.execute("SELECT id FROM customers")
            customers = {r[0] for r in dc.fetchall()}
            dc.execute("SELECT id FROM products")
            existing_products = {r[0] for r in dc.fetchall()}
            dc.execute("SELECT code FROM products WHERE code IS NOT NULL")
            existing_codes = {r[0] for r in dc.fetchall()}
            dc.execute("SELECT id FROM customer_addresses")
            existing_addrs = {r[0] for r in dc.fetchall()}
            dc.execute("SELECT id FROM users")
            existing_users = {r[0] for r in dc.fetchall()}
            dc.execute("SELECT id, username FROM users WHERE is_deleted = false")
            user_by_id = {r[0]: r[1] for r in dc.fetchall()}

            # ---------- PRODUCTS ----------
            print("== products ==")
            sc.execute(
                """
                SELECT id, code, name, description, cat_id, uom_1, uom_2, tax, misa_code,
                       created_at, updated_at, deleted_at
                FROM ies."PRO_PRODUCT"
                """
            )
            ins = skip_id = skip_code = 0
            for p in sc.fetchall():
                if p["id"] in existing_products:
                    skip_id += 1
                    continue
                code = clip(p["code"], 50)
                if not code:
                    skip_code += 1
                    continue
                if code in existing_codes:
                    skip_code += 1
                    continue
                name = clip(p["name"], 200) or code
                cat = p["cat_id"] if p["cat_id"] in categories else None
                tax = money(p["tax"]) if p["tax"] is not None else Decimal("0")
                dc.execute(
                    """
                    INSERT INTO products (
                      id, code, name, description, category_id, unit_1, unit_2,
                      price, cost_price, tax, misa_code, is_active,
                      created_at, updated_at, created_by, updated_by, is_deleted
                    ) VALUES (
                      %s,%s,%s,%s,%s,%s,%s,0,0,%s,%s,true,
                      COALESCE(%s,NOW()),COALESCE(%s,NOW()),'legacy-migrate','legacy-migrate',%s
                    )
                    ON CONFLICT (id) DO NOTHING
                    """,
                    (
                        p["id"],
                        code,
                        name,
                        p["description"],
                        cat,
                        clip(p["uom_1"], 20),
                        clip(p["uom_2"], 20),
                        tax,
                        clip(p["misa_code"], 100),
                        p["created_at"],
                        p["updated_at"] or p["created_at"],
                        p["deleted_at"] is not None,
                    ),
                )
                if dc.rowcount:
                    ins += 1
                    existing_products.add(p["id"])
                    existing_codes.add(code)
                else:
                    skip_id += 1
            dst.commit()
            report["products"] = {
                "inserted": ins,
                "skip_existing_id": skip_id,
                "skip_code": skip_code,
            }
            print(f"  inserted={ins} skip_id={skip_id} skip_code={skip_code}")

            # ---------- CUSTOMER ADDRESSES ----------
            print("== customer_addresses ==")
            sc.execute(
                """
                SELECT id, contact_name, phone, email, add_line_1, add_line_2,
                       ward, district, province, customer_id,
                       created_at, updated_at, deleted_at
                FROM ies."CUS_CUSTOMER_CONTACT"
                """
            )
            ins_a = skip_a = skip_cust = 0
            for a in sc.fetchall():
                if a["id"] in existing_addrs:
                    skip_a += 1
                    continue
                cust = a["customer_id"]
                if not cust or cust not in customers:
                    skip_cust += 1
                    continue
                dc.execute(
                    """
                    INSERT INTO customer_addresses (
                      id, customer_id, address, contact_person, phone, email,
                      created_at, updated_at, created_by, updated_by, is_deleted
                    ) VALUES (
                      %s,%s,%s,%s,%s,%s,
                      COALESCE(%s,NOW()),COALESCE(%s,NOW()),'legacy-migrate','legacy-migrate',%s
                    )
                    ON CONFLICT (id) DO NOTHING
                    """,
                    (
                        a["id"],
                        cust,
                        join_address(a),
                        clip(a["contact_name"], 100),
                        clip(a["phone"], 20),
                        clip(a["email"], 100),
                        a["created_at"],
                        a["updated_at"] or a["created_at"],
                        a["deleted_at"] is not None,
                    ),
                )
                if dc.rowcount:
                    ins_a += 1
                    existing_addrs.add(a["id"])
                else:
                    skip_a += 1
            dst.commit()
            report["customer_addresses"] = {
                "inserted": ins_a,
                "skip_existing": skip_a,
                "skip_missing_customer": skip_cust,
            }
            print(f"  inserted={ins_a} skip_existing={skip_a} skip_customer={skip_cust}")

            # Relink orders.customer_address_id from legacy
            print("== relink orders.customer_address_id ==")
            sc.execute(
                """
                SELECT id, customer_address_id
                FROM ies."ORD_ORDER"
                WHERE customer_address_id IS NOT NULL
                """
            )
            relinked = 0
            for o in sc.fetchall():
                addr = o["customer_address_id"]
                if addr not in existing_addrs:
                    continue
                dc.execute(
                    """
                    UPDATE orders
                    SET customer_address_id = %s
                    WHERE id = %s
                      AND (customer_address_id IS NULL OR customer_address_id IS DISTINCT FROM %s)
                    """,
                    (addr, o["id"], addr),
                )
                relinked += dc.rowcount
            dst.commit()
            report["orders_address_relink"] = {"updated": relinked}
            print(f"  updated={relinked}")

            # ---------- USERS (missing only; disabled login) ----------
            print("== users (missing) ==")
            sc.execute(
                """
                SELECT id, username, email, phone, password, firstname, lastname,
                       created_at, updated_at, deleted_at, disabled_at
                FROM core."SYS_USER"
                """
            )
            ins_u = skip_u = 0
            for u in sc.fetchall():
                if u["id"] in existing_users:
                    skip_u += 1
                    continue
                username = clip(u["username"], 50)
                email = clip(u["email"], 100)
                if not username or not email:
                    skip_u += 1
                    continue
                # avoid unique collisions on renamed usernames
                dc.execute(
                    "SELECT 1 FROM users WHERE username=%s OR email=%s",
                    (username, email),
                )
                if dc.fetchone():
                    skip_u += 1
                    continue
                full_name = " ".join(
                    x for x in [u.get("firstname"), u.get("lastname")] if x
                ).strip() or username
                pwd = u["password"] if u["password"] and len(str(u["password"])) > 20 else DISABLED_PASSWORD
                is_active = u["disabled_at"] is None and u["deleted_at"] is None
                dc.execute(
                    """
                    INSERT INTO users (
                      id, username, email, password, first_name, last_name, full_name, phone,
                      is_active, created_at, updated_at, created_by, updated_by, is_deleted
                    ) VALUES (
                      %s,%s,%s,%s,%s,%s,%s,%s,%s,
                      COALESCE(%s,NOW()),COALESCE(%s,NOW()),'legacy-migrate','legacy-migrate',%s
                    )
                    ON CONFLICT (id) DO NOTHING
                    """,
                    (
                        u["id"],
                        username,
                        email,
                        pwd[:255],
                        clip(u["firstname"], 100),
                        clip(u["lastname"], 100),
                        clip(full_name, 255),
                        clip(u["phone"], 20),
                        is_active,
                        u["created_at"],
                        u["updated_at"] or u["created_at"],
                        u["deleted_at"] is not None,
                    ),
                )
                if dc.rowcount:
                    ins_u += 1
                    existing_users.add(u["id"])
                else:
                    skip_u += 1
            dst.commit()
            report["users"] = {"inserted": ins_u, "skipped": skip_u}
            print(f"  inserted={ins_u} skipped={skip_u}")

        out_dir = Path(__file__).resolve().parents[1] / "out"
        out_dir.mkdir(parents=True, exist_ok=True)
        out_path = out_dir / "etl_skip_report_master.json"
        out_path.write_text(json.dumps(report, indent=2, default=str))
        print(f"REPORT {out_path}")
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

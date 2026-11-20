#!/usr/bin/env bash
# On TARGET DB: create postgres_fdw schemas legacy_core / legacy_ies → cnh_legacy.
# Imports only tables needed for ETL (avoids enum-typed tables like SYS_CONFIG / ORD_ORDER).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if [[ -z "${TARGET_DATABASE_URL:-}" ]]; then
  echo "TARGET_DATABASE_URL is required (cnh_wms or non-prod target)." >&2
  exit 1
fi

if [[ -z "${LEGACY_DATABASE_URL:-}" ]]; then
  echo "LEGACY_DATABASE_URL is required (restored cnh_legacy)." >&2
  echo "Run restore_legacy_dump.sh first." >&2
  exit 1
fi

eval "$(python3 - <<'PY'
from urllib.parse import urlparse, unquote
import os, shlex
u = urlparse(os.environ["LEGACY_DATABASE_URL"])
host = u.hostname or "localhost"
port = str(u.port or 5432)
db = (u.path or "/cnh_legacy").lstrip("/") or "cnh_legacy"
user = unquote(u.username or "postgres")
password = unquote(u.password or "")
# Prefer PGPASSWORD if URL has no password
if not password:
    password = os.environ.get("PGPASSWORD", "")
print(f"FDW_HOST={shlex.quote(host)}")
print(f"FDW_PORT={shlex.quote(port)}")
print(f"FDW_DBNAME={shlex.quote(db)}")
print(f"FDW_USER={shlex.quote(user)}")
print(f"FDW_PASSWORD={shlex.quote(password)}")
PY
)"

echo "== Linking TARGET → legacy FDW ($FDW_DBNAME @ $FDW_HOST:$FDW_PORT) =="

psql "$TARGET_DATABASE_URL" -v ON_ERROR_STOP=1 <<SQL
CREATE EXTENSION IF NOT EXISTS postgres_fdw;

DO \$\$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_foreign_server WHERE srvname = 'legacy_cnh') THEN
    CREATE SERVER legacy_cnh FOREIGN DATA WRAPPER postgres_fdw
      OPTIONS (host '$FDW_HOST', dbname '$FDW_DBNAME', port '$FDW_PORT');
  ELSE
    ALTER SERVER legacy_cnh OPTIONS (
      SET host '$FDW_HOST',
      SET dbname '$FDW_DBNAME',
      SET port '$FDW_PORT'
    );
  END IF;
END \$\$;

DROP USER MAPPING IF EXISTS FOR CURRENT_USER SERVER legacy_cnh;
CREATE USER MAPPING FOR CURRENT_USER SERVER legacy_cnh
  OPTIONS (user '$FDW_USER', password '$FDW_PASSWORD');

CREATE SCHEMA IF NOT EXISTS legacy_core;
CREATE SCHEMA IF NOT EXISTS legacy_ies;

DO \$\$
DECLARE r RECORD;
BEGIN
  FOR r IN
    SELECT foreign_table_schema, foreign_table_name
    FROM information_schema.foreign_tables
    WHERE foreign_table_schema IN ('legacy_core', 'legacy_ies')
  LOOP
    EXECUTE format('DROP FOREIGN TABLE IF EXISTS %I.%I CASCADE',
      r.foreign_table_schema, r.foreign_table_name);
  END LOOP;
END \$\$;

-- Only tables without custom enum columns (full schema import fails on target)
IMPORT FOREIGN SCHEMA core
  LIMIT TO ("SYS_MEDIA", "SYS_USER", "SYS_ROLE")
  FROM SERVER legacy_cnh INTO legacy_core;

IMPORT FOREIGN SCHEMA ies
  LIMIT TO (
    "VEN_VENDOR",
    "CUS_CUSTOMER",
    "CUS_CUSTOMER_CONTACT",
    "PRO_PRODUCT_CAT",
    "PRO_PRODUCT"
  )
  FROM SERVER legacy_cnh INTO legacy_ies;
SQL

echo "FDW ready: limited legacy_core / legacy_ies tables on TARGET."
echo "Next: ./scripts/run_stage.sh 10"

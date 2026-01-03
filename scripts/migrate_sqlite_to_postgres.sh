#!/usr/bin/env bash
set -euo pipefail

SQLITE_DB="${1:-backend/data/fairshare.db}"
USE_DOCKER="${USE_DOCKER:-true}"
COMPOSE_CMD="${COMPOSE_CMD:-docker compose}"
PG_DB="${PG_DB:-fairshare}"
PG_USER="${PG_USER:-fairshare}"
PG_HOST="${PG_HOST:-localhost}"
PG_PORT="${PG_PORT:-5432}"

if ! command -v sqlite3 >/dev/null 2>&1; then
  echo "sqlite3 is required but not found." >&2
  exit 1
fi

if [ ! -f "$SQLITE_DB" ]; then
  echo "SQLite DB not found at $SQLITE_DB" >&2
  exit 1
fi

TMP_DIR="$(mktemp -d)"
cleanup() {
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT

sqlite3 -separator $'\t' -nullvalue '\N' "$SQLITE_DB" "SELECT id, name, type, rank FROM categories ORDER BY id;" > "$TMP_DIR/categories.tsv"
sqlite3 -separator $'\t' -nullvalue '\N' "$SQLITE_DB" "SELECT id, name FROM persons ORDER BY id;" > "$TMP_DIR/persons.tsv"
sqlite3 -separator $'\t' -nullvalue '\N' "$SQLITE_DB" "
SELECT
  id,
  name,
  amount,
  type,
  frequency,
  CASE
    WHEN active IS NULL THEN NULL
    WHEN active = 1 THEN 'true'
    ELSE 'false'
  END AS active,
  CASE
    WHEN start_date IS NULL THEN NULL
    WHEN typeof(start_date) = 'integer' THEN strftime('%Y-%m-%d', start_date / 1000, 'unixepoch')
    ELSE strftime('%Y-%m-%d', start_date)
  END AS start_date,
  CASE
    WHEN end_date IS NULL THEN NULL
    WHEN typeof(end_date) = 'integer' THEN strftime('%Y-%m-%d', end_date / 1000, 'unixepoch')
    ELSE strftime('%Y-%m-%d', end_date)
  END AS end_date,
  category_id,
  person_id
FROM budget_items
ORDER BY id;
" > "$TMP_DIR/budget_items.tsv"

if [ "$USE_DOCKER" = "true" ]; then
  PSQL_CMD=("$COMPOSE_CMD" exec -T db psql -U "$PG_USER" -d "$PG_DB")
else
  PSQL_CMD=(psql -h "$PG_HOST" -p "$PG_PORT" -U "$PG_USER" -d "$PG_DB")
fi

"${PSQL_CMD[@]}" <<SQL
TRUNCATE TABLE budget_items, categories, persons RESTART IDENTITY CASCADE;
SQL

cat "$TMP_DIR/categories.tsv" | "${PSQL_CMD[@]}" \
  -c "\\copy categories(id, name, type, rank) FROM STDIN WITH (FORMAT text, NULL '\\N')"
cat "$TMP_DIR/persons.tsv" | "${PSQL_CMD[@]}" \
  -c "\\copy persons(id, name) FROM STDIN WITH (FORMAT text, NULL '\\N')"
cat "$TMP_DIR/budget_items.tsv" | "${PSQL_CMD[@]}" \
  -c "\\copy budget_items(id, name, amount, type, frequency, active, start_date, end_date, category_id, person_id) FROM STDIN WITH (FORMAT text, NULL '\\N')"

"${PSQL_CMD[@]}" <<SQL
SELECT setval(pg_get_serial_sequence('categories','id'), COALESCE(MAX(id), 0)) FROM categories;
SELECT setval(pg_get_serial_sequence('persons','id'), COALESCE(MAX(id), 0)) FROM persons;
SELECT setval(pg_get_serial_sequence('budget_items','id'), COALESCE(MAX(id), 0)) FROM budget_items;
SQL

echo "Migration complete."

#!/usr/bin/env bash
# Deletes ALL events from a PicPeak instance.
# Reads credentials from configuration/picpeak-credentials.properties
# (same file used by the Java app).
#
# Usage:
#   ./picpeak-delete-all-events.sh              # force-delete all events (non-interactive)
#   ./picpeak-delete-all-events.sh --dry-run    # list events without deleting

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROPS_FILE="${SCRIPT_DIR}/configuration/picpeak-credentials.properties"

DRY_RUN=false
for arg in "$@"; do
  case "$arg" in
    --dry-run)
      DRY_RUN=true
      ;;
    *)
      echo "ERROR: Unknown argument: $arg" >&2
      echo "Usage: $0 [--dry-run]" >&2
      exit 1
      ;;
  esac
done

# Read a property value from configuration/picpeak-credentials.properties
# (strips \r from Windows line endings)
read_prop() {
  local key="$1"
  grep -E "^${key}=" "$PROPS_FILE" 2>/dev/null | head -1 | cut -d'=' -f2- | tr -d '\r'
}

# Verify configuration/picpeak-credentials.properties exists
if [[ ! -f "$PROPS_FILE" ]]; then
  echo "ERROR: $PROPS_FILE not found." >&2
  exit 1
fi

API_URL="$(read_prop 'app.picpeak.api-url')"
USERNAME="$(read_prop 'app.picpeak.username')"
PASSWORD="$(read_prop 'app.picpeak.password')"

if [[ -z "$API_URL" || -z "$USERNAME" || -z "$PASSWORD" ]]; then
  echo "ERROR: configuration/picpeak-credentials.properties must contain api-url, username, and password." >&2
  exit 1
fi

# Strip trailing slash
API_URL="${API_URL%/}"

echo "PicPeak instance: $API_URL"
echo ""

# Login (use a temp cookie jar to capture admin_token reliably)
echo "Logging in as $USERNAME..."
COOKIE_JAR=$(mktemp)
trap 'rm -f "$COOKIE_JAR"' EXIT

LOGIN_BODY=$(curl -s -c "$COOKIE_JAR" \
  -X POST "${API_URL}/api/auth/admin/login" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\",\"recaptchaToken\":null}")

# Try cookie jar first, then fall back to JSON body
ADMIN_TOKEN=$(grep 'admin_token' "$COOKIE_JAR" 2>/dev/null | awk '{print $NF}' || true)
if [[ -z "$ADMIN_TOKEN" ]]; then
  ADMIN_TOKEN=$(echo "$LOGIN_BODY" | grep -oP '"token"\s*:\s*"\K[^"]+' || true)
fi

if [[ -z "$ADMIN_TOKEN" ]]; then
  echo "ERROR: Login failed — could not extract admin_token." >&2
  echo "$LOGIN_BODY" >&2
  exit 1
fi

echo "Login successful."
echo ""

# Fetch all events with pagination
echo "Fetching events..."
LIMIT=100
PAGE=1
EVENT_IDS=""

while true; do
  PAGE_JSON=$(curl -s \
    "${API_URL}/api/admin/events?page=${PAGE}&limit=${LIMIT}" \
    -H "Accept: application/json" \
    -b "admin_token=${ADMIN_TOKEN}")

  PAGE_IDS=$(echo "$PAGE_JSON" | grep -oP '"id"\s*:\s*\K[0-9]+' || true)
  PAGE_COUNT=$(echo "$PAGE_IDS" | grep -c '[0-9]' || true)

  if [[ "$PAGE_COUNT" -eq 0 ]]; then
    break
  fi

  EVENT_IDS+="$PAGE_IDS"
  EVENT_IDS+=$'\n'

  if [[ "$PAGE_COUNT" -lt "$LIMIT" ]]; then
    break
  fi

  PAGE=$((PAGE + 1))
done

# Normalize to unique IDs
EVENT_IDS=$(echo "$EVENT_IDS" | grep -E '^[0-9]+$' | sort -n | uniq)
EVENT_COUNT=$(echo "$EVENT_IDS" | grep -c '[0-9]' || true)

if [[ "$EVENT_COUNT" -eq 0 ]]; then
  echo "No events found. Nothing to delete."
  exit 0
fi

echo "Found $EVENT_COUNT event(s)."
echo ""

if [[ "$DRY_RUN" == true ]]; then
  echo "DRY RUN — would delete the following event IDs:"
  echo "$EVENT_IDS" | tr '\n' ' '
  echo ""
  exit 0
fi

# Force mode: no interactive confirmation
echo "FORCE DELETE enabled: deleting ALL $EVENT_COUNT event(s) without prompt."
echo ""

# Delete each event
DELETED=0
FAILED=0
for EVENT_ID in $EVENT_IDS; do
  HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    -X DELETE "${API_URL}/api/admin/events/${EVENT_ID}" \
    -H "Accept: application/json" \
    -b "admin_token=${ADMIN_TOKEN}")

  if [[ "$HTTP_CODE" -ge 200 && "$HTTP_CODE" -lt 300 ]]; then
    echo "  Deleted event #${EVENT_ID} (HTTP ${HTTP_CODE})"
    DELETED=$((DELETED + 1))
  else
    echo "  FAILED to delete event #${EVENT_ID} (HTTP ${HTTP_CODE})" >&2
    FAILED=$((FAILED + 1))
  fi
done

echo ""
echo "Done. Deleted: $DELETED, Failed: $FAILED"

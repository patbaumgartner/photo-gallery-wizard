#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR=$(find "$SCRIPT_DIR" -maxdepth 1 -name "photo-gallery-wizard-*.jar" | head -1)

if [ -z "$JAR" ]; then
  echo "ERROR: No photo-gallery-wizard JAR found in $SCRIPT_DIR" >&2
  exit 1
fi

java -jar "$JAR" "$@"

#!/usr/bin/env bash
set -euo pipefail
LOG=deep-test.log
: > "$LOG"
exec > >(tee -a "$LOG") 2>&1

echo "START $(date -Is)"
echo "STEP unit-clean"
./mvnw -q clean test

echo "STEP random-order-x10"
i=1
while [ "$i" -le 10 ]; do
  echo "RUN_RANDOM_$i"
  ./mvnw -q -DskipITs=false -Dsurefire.runOrder=random test
  i=$((i+1))
done

echo "STEP timezone-matrix"
for tz in UTC Europe/Berlin America/New_York Asia/Tokyo; do
  echo "RUN_TZ_$tz"
  TZ="$tz" ./mvnw -q -DskipITs=false test
done

echo "STEP locale-matrix"
for lang in C en_US.UTF-8 de_CH.UTF-8; do
  echo "RUN_LANG_$lang"
  LANG="$lang" LC_ALL="$lang" ./mvnw -q -DskipITs=false test
done

echo "STEP verify"
./mvnw -q verify

echo "DONE $(date -Is)"

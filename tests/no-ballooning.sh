#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
main_activity="$repo_root/app/src/main/java/com/coordinate/measuring/dreams/MainActivity.java"
readme="$repo_root/README.md"

for forbidden in \
  'Drawing Ballooning' \
  'DrawingBalloonView' \
  'BalloonRecord' \
  'REQUEST_OPEN_DRAWING' \
  'REQUEST_CAPTURE_DRAWING' \
  'showDrawingBallooning' \
  'balloonRecords'; do
  if grep -Fqi "$forbidden" "$main_activity" "$readme"; then
    printf 'FAIL: removed drawing-ballooning feature is still referenced: %s\n' "$forbidden" >&2
    exit 1
  fi
done

printf 'PASS: drawing-ballooning feature is absent from the app source and README.\n'

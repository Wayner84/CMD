#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source_file="$repo_root/app/src/main/java/com/coordinate/measuring/dreams/MainActivity.java"

require_source() {
  local description="$1"
  local pattern="$2"
  if ! grep -Eq "$pattern" "$source_file"; then
    printf 'FAIL: %s\n' "$description" >&2
    exit 1
  fi
}

require_source 'angle typing must preserve the focused field' 'updateFromAngles\(true\)'
require_source 'IJK typing must preserve the focused field' 'setVector\(ii, jj, kk, true\)'
require_source 'UI refresh must not rewrite the focused angle input' '!preserveFocusedInput \|\| !xyInput\.hasFocus\(\)'
require_source 'UI refresh must not rewrite the focused IJK input' '!preserveFocusedInput \|\| !iInput\.hasFocus\(\)'
require_source 'probe calculator content must be scrollable on small screens' 'ScrollView page = new ScrollView\(this\)'
require_source 'probe diameter must have a live-update watcher' 'addProbeWatcher\(probeDia, solve\)'
require_source 'shaft diameter must have a live-update watcher' 'addProbeWatcher\(shaftDia, solve\)'
require_source 'usable length must have a live-update watcher' 'addProbeWatcher\(length, solve\)'
require_source 'probe preview must use a restrained maximum shaft width' 'Math\.min\(dp\(18\),'
require_source 'probe reference geometry must use a thin line' 'paint\.setStrokeWidth\(dp\(1\)\)'

printf 'PASS: editable angle inputs and responsive live probe preview regressions are covered.\n'

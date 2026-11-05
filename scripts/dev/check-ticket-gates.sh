#!/usr/bin/env bash
#
# check-ticket-gates.sh — the closure gate for a local-markdown feature.
#
# Checks that every resolved ticket has all acceptance boxes ticked and a `## Comments`
# entry, then (unless --skip-verify) runs scripts/ci/verify.sh and checks the JUnit XML for
# failures. Unresolved tickets are reported, not failed — a `ready-for-human` ticket is a
# legitimate state.
#
# Usage:
#   scripts/dev/check-ticket-gates.sh --feature <slug> [--skip-verify]
#
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
FEATURE=""
SKIP_VERIFY=0

usage() { awk 'NR>1 && /^#/ {sub(/^# ?/, ""); print; next} NR>1 {exit}' "${BASH_SOURCE[0]}"; }

while [ $# -gt 0 ]; do
  case "$1" in
    -h|--help) usage; exit 0 ;;
    --feature) FEATURE="${2:?--feature needs a value}"; shift 2 ;;
    --skip-verify) SKIP_VERIFY=1; shift ;;
    -*) echo "unknown option: $1" >&2; exit 2 ;;
    *) echo "unexpected argument: $1" >&2; exit 2 ;;
  esac
done

if [ -z "$FEATURE" ]; then usage; exit 2; fi
TICKETS="$ROOT/.scratch/$FEATURE/issues"
if [ ! -d "$TICKETS" ]; then echo "no ticket directory: $TICKETS" >&2; exit 2; fi

if [ -t 1 ]; then R=$'\e[31m'; G=$'\e[32m'; Y=$'\e[33m'; Z=$'\e[0m'; else R= G= Y= Z=; fi
fail=0
bad() { printf '%sFAIL%s %s\n' "$R" "$Z" "$1"; fail=1; }
ok() { printf '%sPASS%s %s\n' "$G" "$Z" "$1"; }
note() { printf '%sOPEN%s %s\n' "$Y" "$Z" "$1"; }

echo "feature: $FEATURE"
echo "tickets: $TICKETS"
echo
for file in "$TICKETS"/*.md; do
  [ -e "$file" ] || continue
  name="$(basename "$file")"
  status="$(sed -n 's/^[*][*]Status:[*][*][[:space:]]*//p' "$file" | head -1)"
  open="$(grep -c '^- \[ \]' "$file" || true)"
  done="$(grep -c '^- \[x\]' "$file" || true)"

  if [ "$status" = "resolved" ]; then
    if [ "$open" -gt 0 ]; then
      bad "$name — resolved but $open acceptance box(es) still open"
    fi
    if ! grep -q '^## Comments' "$file"; then
      bad "$name — resolved but no '## Comments' closure entry"
    fi
    if [ "$open" -eq 0 ] && grep -q '^## Comments' "$file"; then
      ok "$name — resolved, $done boxes ticked"
    fi
  else
    note "$name — ${status:-no Status line}; $done ticked, $open open"
  fi
done

if [ "$SKIP_VERIFY" = 0 ]; then
  echo
  echo "running scripts/ci/verify.sh ..."
  log="$(mktemp "${TMPDIR:-/tmp}/ticket-gates.XXXXXX")"
  if "$ROOT/scripts/ci/verify.sh" >"$log" 2>&1; then
    ok "scripts/ci/verify.sh — OVERALL: PASS"
  else
    bad "scripts/ci/verify.sh — OVERALL: FAIL (see $log)"
    tail -20 "$log" | sed 's/^/       /'
  fi
fi

if command -v python3 >/dev/null 2>&1; then
  totals="$(cd "$ROOT" && python3 - <<'EOF'
import glob, xml.etree.ElementTree as ET
tests = failures = errors = skipped = files = 0
for path in glob.glob('**/build/test-results/**/TEST-*.xml', recursive=True):
    root = ET.parse(path).getroot()
    files += 1
    tests += int(root.get('tests', 0))
    failures += int(root.get('failures', 0))
    errors += int(root.get('errors', 0))
    skipped += int(root.get('skipped', 0))
print(f"{files} {tests} {failures} {errors} {skipped}")
EOF
)"
  read -r files tests failures errors skipped <<<"$totals"
  if [ "$files" -eq 0 ]; then
    note "no JUnit XML found under build/test-results"
  elif [ "$failures" -gt 0 ] || [ "$errors" -gt 0 ]; then
    bad "JUnit: $tests tests, $failures failures, $errors errors"
  else
    ok "JUnit: $tests tests, 0 failures, 0 errors ($skipped skipped) across $files files"
  fi
fi

echo
if [ "$fail" = 0 ]; then echo "ticket gates: PASS"; else echo "ticket gates: FAILED"; fi
exit "$fail"

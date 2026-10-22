#!/bin/sh
set -eu

mode=${1:-}
case "$mode" in
  pre-commit|pre-push) ;;
  *) printf 'Usage: %s pre-commit|pre-push\n' "$0" >&2; exit 2 ;;
esac

repo_root=$(git rev-parse --show-toplevel)
cd "$repo_root"
check_dir=$(mktemp -d "${TMPDIR:-/tmp}/fincore-git-checks.XXXXXX")
check_dir=$(cd "$check_dir" && pwd -P)
trap 'rm -rf "$check_dir"' 0
trap 'exit 1' HUP INT TERM

# Run against an isolated snapshot, so partially staged files are checked exactly
# as committed and local changes cannot conceal a failure in a pushed commit.
check_java() (
  cd "$check_dir/tree"
  if [ ! -f fincore-java-server/pom.xml ]; then
    printf 'No Java server in this snapshot; skipping Java checks.\n'
    exit 0
  fi

  cd fincore-java-server
  if [ "$mode" = pre-commit ]; then
    printf 'Checking staged Java formatting and Checkstyle...\n'
    sh ./mvnw --batch-mode --no-transfer-progress validate </dev/null
  else
    printf 'Checking committed Java formatting, Checkstyle, and all tests...\n'
    sh ./mvnw --batch-mode --no-transfer-progress clean verify </dev/null
  fi
)

check_python() (
  cd "$check_dir/tree"
  if [ ! -f finecore-ai-server/pyproject.toml ]; then
    exit 0
  fi

  cd finecore-ai-server
  printf 'Checking Python lint and formatting...\n'
  uv sync --locked </dev/null
  uv run --frozen ruff check . </dev/null
  uv run --frozen ruff format --check . </dev/null
  if [ "$mode" = pre-push ]; then
    printf 'Running Python tests...\n'
    uv run --frozen pytest </dev/null
  fi
)

if [ "$mode" = pre-commit ]; then
  git diff --cached --check
  mkdir "$check_dir/tree"
  git checkout-index --all --prefix="$check_dir/tree/"
  check_java
  check_python
else
  # Git supplies one line per pushed ref. Deletions have an all-zero local OID.
  # Maven gets /dev/null above so it cannot consume the remaining ref lines.
  while read -r local_ref local_oid remote_ref remote_oid; do
    case "$local_oid" in
      ''|*[!0]*) ;;
      *) continue ;;
    esac
    commit_oid=$(git rev-parse --verify "$local_oid^{commit}" 2>/dev/null) || continue
    # A commit can be pushed under several branches or tags; check it once.
    if [ -f "$check_dir/checked-$commit_oid" ]; then
      continue
    fi
    printf 'Checking pushed ref %s (%s)...\n' "$local_ref" "$commit_oid"
    rm -rf "$check_dir/tree"
    mkdir "$check_dir/tree"
    git archive --format=tar --output="$check_dir/snapshot.tar" "$commit_oid"
    tar -xf "$check_dir/snapshot.tar" -C "$check_dir/tree"
    check_java
    check_python
    touch "$check_dir/checked-$commit_oid"
  done
fi

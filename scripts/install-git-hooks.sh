#!/bin/sh
set -eu

repo_root=$(git rev-parse --show-toplevel)
cd "$repo_root"

existing_path=$(git config --local --get core.hooksPath || :)
if [ -n "$existing_path" ] && [ "$existing_path" != '.githooks' ]; then
  printf 'Existing hooks path is %s. Integrate those hooks before changing it.\n' "$existing_path" >&2
  exit 1
fi

chmod +x .githooks/pre-commit .githooks/pre-push
git config --local core.hooksPath .githooks
printf 'Installed pre-commit and pre-push checks for this clone.\n'

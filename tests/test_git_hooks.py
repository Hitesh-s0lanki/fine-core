"""Exercise Git hooks in temporary repositories without changing the real index."""

import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[1]


class GitHooksTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="fincore hooks ")
        self.addCleanup(self.temp.cleanup)
        self.repo = Path(self.temp.name)
        self.log = self.repo / "maven.log"
        self.env = dict(os.environ, FINCORE_HOOK_TEST_LOG=str(self.log))
        for folder in (".githooks", "scripts"):
            shutil.copytree(ROOT / folder, self.repo / folder)
        self.git("init", "--quiet")
        self.git("config", "user.email", "hooks@example.test")
        self.git("config", "user.name", "Hook Tests")
        self.module = self.repo / "fincore-java-server"
        self.module.mkdir()
        (self.module / "pom.xml").write_text("<project/>\n")
        self.source = self.module / "Example.java"
        self.source.write_text("GOOD\n")
        (self.module / "mvnw").write_text(
            '#!/bin/sh\nset -eu\n'
            'printf "%s\\n" "$*" >> "$FINCORE_HOOK_TEST_LOG"\n'
            'cat Example.java >> "$FINCORE_HOOK_TEST_LOG"\n'
            '! grep -q BROKEN Example.java\n'
        )
        self.git("add", ".")
        self.commit()
        self.run_command("sh", "scripts/install-git-hooks.sh")

    def run_command(self, *args, check=True, input=None):
        return subprocess.run(
            args, cwd=self.repo, env=self.env, input=input,
            text=True, capture_output=True, check=check,
        )

    def git(self, *args):
        return self.run_command("git", *args).stdout.strip()

    def commit(self):
        self.git("-c", "core.hooksPath=/dev/null", "commit", "--quiet", "-m", "fixture")
        return self.git("rev-parse", "HEAD")

    def hook(self, name, input=None):
        return self.run_command("git", "hook", "run", name, check=False, input=input)

    def test_install_is_local_and_repeatable(self):
        self.run_command("sh", "scripts/install-git-hooks.sh")
        self.assertEqual(self.git("config", "--local", "core.hooksPath"), ".githooks")

    def test_commit_checks_index_without_changing_partial_staging(self):
        self.source.write_text("STAGED\n")
        self.git("add", "fincore-java-server/Example.java")
        self.source.write_text("BROKEN unstaged edit\n")
        before = self.git("diff", "--cached", "--binary")
        self.assertEqual(self.hook("pre-commit").returncode, 0)
        self.assertIn("STAGED", self.log.read_text())
        self.assertNotIn("BROKEN", self.log.read_text())
        self.assertEqual(self.git("diff", "--cached", "--binary"), before)
        self.assertEqual(self.source.read_text(), "BROKEN unstaged edit\n")

    def test_commit_rejects_staged_failure_even_when_worktree_is_fixed(self):
        self.source.write_text("BROKEN\n")
        self.git("add", "fincore-java-server/Example.java")
        self.source.write_text("GOOD\n")
        self.assertNotEqual(self.hook("pre-commit").returncode, 0)

    def test_commit_checks_whitespace_outside_java(self):
        (self.repo / "notes.txt").write_text("trailing whitespace  \n")
        self.git("add", "notes.txt")
        self.assertNotEqual(self.hook("pre-commit").returncode, 0)
        self.assertFalse(self.log.exists())

    def test_push_checks_each_distinct_tip_and_skips_deletions(self):
        first = self.git("rev-parse", "HEAD")
        self.source.write_text("SECOND\n")
        self.git("add", "fincore-java-server/Example.java")
        second = self.commit()
        self.source.write_text("BROKEN unstaged\n")
        zero = "0" * 40
        updates = (
            f"refs/heads/main {first} refs/heads/main {zero}\n"
            f"refs/heads/feature {second} refs/heads/feature {zero}\n"
            f"refs/tags/release {second} refs/tags/release {zero}\n"
            f"(delete) {zero} refs/heads/old {first}\n"
        )
        # Invoke the script directly: git hook run does not forward stdin by default.
        result = self.run_command("sh", ".githooks/pre-push", check=False, input=updates)
        self.assertEqual(result.returncode, 0, result.stderr)
        log = self.log.read_text()
        self.assertEqual(log.count("clean verify"), 2)
        self.assertIn("GOOD", log)
        self.assertIn("SECOND", log)
        self.assertNotIn("BROKEN", log)

    def test_push_rejects_committed_failure_even_when_worktree_is_fixed(self):
        self.source.write_text("BROKEN\n")
        self.git("add", "fincore-java-server/Example.java")
        commit = self.commit()
        self.source.write_text("GOOD\n")
        update = f"refs/heads/main {commit} refs/heads/main {'0' * 40}\n"
        result = self.run_command("sh", ".githooks/pre-push", check=False, input=update)
        self.assertNotEqual(result.returncode, 0)


if __name__ == "__main__":
    unittest.main()

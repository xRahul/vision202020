"""Tests for update_fdroid_metadata (stdlib unittest, pytest-compatible)."""

import re
import unittest
from pathlib import Path

import update_fdroid_metadata as script

SHA = "a" * 40
SIGNING_KEY = "9ec14141874c3a8f2e3843f52582c311880fe5417eb6f097fdf8515db92f2511"
BINARIES = (
    "https://github.com/xRahul/vision202020/releases/download/"
    "v%v/in.rahulja.vision202020-v%v-release.apk"
)
RECIPE = (
    Path(__file__).resolve().parent.parent / "metadata" / "in.rahulja.vision202020.yml"
)


def generate() -> str:
    return script.update_recipe(RECIPE.read_text(encoding="utf-8"), "1.1.8", "49", SHA)


class UpdateFdroidMetadataTest(unittest.TestCase):
    def test_gradle_block_is_only_yes(self) -> None:
        out = generate()
        match = re.search(r"^    gradle:\n((?:      - .*\n)+)", out, flags=re.MULTILINE)
        self.assertIsNotNone(match)
        items = [line.strip()[2:] for line in match.group(1).splitlines()]
        self.assertEqual(items, ["yes"])
        self.assertNotIn("-PversionCode", out)
        self.assertNotIn("-PversionName", out)

    def test_binaries_and_signing_key_present(self) -> None:
        out = generate()
        self.assertIn(f"AllowedAPKSigningKeys: {SIGNING_KEY}\n", out)
        self.assertIn(f"Binaries: {BINARIES}\n", out)

    def test_idempotent(self) -> None:
        once = generate()
        twice = script.update_recipe(once, "1.1.8", "49", SHA)
        self.assertEqual(once, twice)

    def test_version_fields_updated(self) -> None:
        out = script.update_recipe(generate(), "1.2.0", "50", "b" * 40)
        self.assertIn("CurrentVersion: 1.2.0\n", out)
        self.assertIn("CurrentVersionCode: 50\n", out)
        self.assertIn(f"    commit: {'b' * 40}\n", out)


if __name__ == "__main__":
    unittest.main()

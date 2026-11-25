#!/usr/bin/env python3
"""Update the F-Droid recipe for a release.

Usage:
    update_fdroid_metadata.py <version> <version_code> <commit_sha>

commit_sha must be the full 40-character commit hash. F-Droid requires a
full hash, not a tag or branch name. The version is pinned by
version.properties in the repo, so the build needs no -P overrides.
"""

import os
import re
import sys

METADATA_PATH = "metadata/in.rahulja.vision202020.yml"
COMMIT_RE = re.compile(r"^[0-9a-f]{40}$")
VERSION_RE = re.compile(r"^\d+\.\d+\.\d+$")

SIGNING_KEY = "9ec14141874c3a8f2e3843f52582c311880fe5417eb6f097fdf8515db92f2511"
BINARIES = (
    "https://github.com/xRahul/vision202020/releases/download/"
    "v%v/in.rahulja.vision202020-v%v-release.apk"
)
UPDATE_CHECK_MODE = r"UpdateCheckMode: Tags ^v[0-9]+\.[0-9]+\.[0-9]+$"
UPDATE_CHECK_DATA = (
    r"UpdateCheckData: version.properties|versionCode=(\d+)|.|versionName=(.+)"
)


def fail(message: str) -> None:
    print(f"ERROR: {message}", file=sys.stderr)
    sys.exit(1)


def set_line(content: str, key: str, value: str, after: str | None = None) -> str:
    """Replace the top-level `key:` line, or insert it after the `after:` line."""
    line = f"{key}: {value}"
    pattern = rf"^{key}:.*$"
    if re.search(pattern, content, flags=re.MULTILINE):
        return re.sub(pattern, lambda _: line, content, count=1, flags=re.MULTILINE)
    if after is not None:
        anchor = rf"^{after}:.*\n"
        if re.search(anchor, content, flags=re.MULTILINE):
            return re.sub(
                anchor,
                lambda m: f"{m.group(0)}{line}\n",
                content,
                count=1,
                flags=re.MULTILINE,
            )
    return content.rstrip("\n") + f"\n{line}\n"


def update_recipe(
    content: str, version: str, version_code: str, commit_sha: str
) -> str:
    """Return the recipe text updated for the given release."""
    content = content.replace("\r\n", "\n")
    content = set_line(content, "AllowedAPKSigningKeys", SIGNING_KEY, after="Repo")
    content = set_line(content, "Binaries", BINARIES, after="AllowedAPKSigningKeys")

    new_build = "\n".join(
        [
            "Builds:",
            f"  - versionName: {version}",
            f"    versionCode: {version_code}",
            f"    commit: {commit_sha}",
            "    subdir: app",
            "    gradle:",
            "      - yes",
            "",
            "AutoUpdateMode:",
        ]
    )
    content, replacements = re.subn(
        r"^Builds:\n[\s\S]*?\nAutoUpdateMode:",
        lambda _: new_build,
        content,
        count=1,
        flags=re.MULTILINE,
    )
    if replacements != 1:
        fail("could not find the Builds/AutoUpdateMode block in the recipe")

    content = set_line(content, "AutoUpdateMode", "Version")
    content = set_line(content, "UpdateCheckMode", UPDATE_CHECK_MODE.split(": ", 1)[1])
    content = set_line(
        content,
        "UpdateCheckData",
        UPDATE_CHECK_DATA.split(": ", 1)[1],
        after="UpdateCheckMode",
    )
    content = set_line(content, "CurrentVersion", version)
    content = set_line(content, "CurrentVersionCode", version_code)

    if not content.endswith("\n"):
        content += "\n"
    return content


def main() -> None:
    if len(sys.argv) != 4:
        fail("usage: update_fdroid_metadata.py <version> <version_code> <commit_sha>")

    version, version_code, commit_sha = sys.argv[1:4]

    if not VERSION_RE.fullmatch(version):
        fail(f"invalid version: {version}")
    if not version_code.isdigit():
        fail(f"invalid versionCode: {version_code}")
    if not COMMIT_RE.fullmatch(commit_sha):
        fail(f"commit_sha must be a full 40-character lowercase hash: {commit_sha}")

    if not os.path.exists(METADATA_PATH):
        print(f"Notice: {METADATA_PATH} not found. Skipping.")
        return

    with open(METADATA_PATH, "r", encoding="utf-8") as handle:
        content = handle.read()

    content = update_recipe(content, version, version_code, commit_sha)

    with open(METADATA_PATH, "w", encoding="utf-8", newline="\n") as handle:
        handle.write(content)

    print(
        f"Updated {METADATA_PATH} to version {version} "
        f"(code: {version_code}, commit: {commit_sha})"
    )


if __name__ == "__main__":
    main()

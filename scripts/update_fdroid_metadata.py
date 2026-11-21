#!/usr/bin/env python3
"""Update the F-Droid recipe for a release.

Usage:
    update_fdroid_metadata.py <version> <version_code> <commit_sha>

commit_sha must be the full 40-character commit hash. F-Droid requires a
full hash, not a tag or branch name.
"""

import os
import re
import sys

METADATA_PATH = "metadata/in.rahulja.vision202020.yml"
COMMIT_RE = re.compile(r"^[0-9a-f]{40}$")
VERSION_RE = re.compile(r"^\d+\.\d+\.\d+$")


def fail(message: str) -> None:
    print(f"ERROR: {message}", file=sys.stderr)
    sys.exit(1)


def main() -> None:
    if len(sys.argv) != 4:
        fail("usage: update_fdroid_metadata.py <version> <version_code> <commit_sha>")

    version = sys.argv[1]
    version_code = sys.argv[2]
    commit_sha = sys.argv[3]

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
        content = handle.read().replace("\r\n", "\n")

    content = re.sub(
        r"^CurrentVersion:.*$",
        f"CurrentVersion: {version}",
        content,
        count=1,
        flags=re.M,
    )
    content = re.sub(
        r"^CurrentVersionCode:.*$",
        f"CurrentVersionCode: {version_code}",
        content,
        count=1,
        flags=re.M,
    )

    new_build = "\n".join(
        [
            "Builds:",
            f"  - versionName: {version}",
            f"    versionCode: {version_code}",
            f"    commit: {commit_sha}",
            "    subdir: app",
            "    gradle:",
            "      - yes",
            f"      - -PversionCode={version_code}",
            f"      - -PversionName={version}",
            "",
            "AutoUpdateMode:",
        ]
    )

    content, replacements = re.subn(
        r"^Builds:\n[\s\S]*?\nAutoUpdateMode:",
        new_build,
        content,
        count=1,
        flags=re.M,
    )
    if replacements != 1:
        fail("could not find the Builds/AutoUpdateMode block in the recipe")

    content = re.sub(
        r"^AutoUpdateMode:.*$",
        "AutoUpdateMode: Version",
        content,
        count=1,
        flags=re.M,
    )
    content = re.sub(
        r"^UpdateCheckMode:.*$",
        lambda _: r"UpdateCheckMode: Tags ^v[0-9]+\.[0-9]+\.[0-9]+$",
        content,
        count=1,
        flags=re.M,
    )
    if not re.search(r"^UpdateCheckData:", content, flags=re.M):
        content = content.replace(
            "UpdateCheckMode: Tags ^v[0-9]+\\.[0-9]+\\.[0-9]+$\n",
            "UpdateCheckMode: Tags ^v[0-9]+\\.[0-9]+\\.[0-9]+$\n"
            "UpdateCheckData: version.properties|versionCode=(\\d+)|.|versionName=(.+)\n",
            1,
        )

    if not content.endswith("\n"):
        content += "\n"

    with open(METADATA_PATH, "w", encoding="utf-8", newline="\n") as handle:
        handle.write(content)

    print(
        f"Updated {METADATA_PATH} to version {version} "
        f"(code: {version_code}, commit: {commit_sha})"
    )


if __name__ == "__main__":
    main()

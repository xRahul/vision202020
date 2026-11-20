#!/usr/bin/env python3
import sys
import re
import os


def main():
    if len(sys.argv) < 4:
        print("Usage: update_fdroid_metadata.py <version> <version_code> <tag>")
        sys.exit(1)

    version = sys.argv[1]
    version_code = sys.argv[2]
    tag = sys.argv[3]
    metadata_path = "metadata/in.rahulja.vision202020.yml"

    if not os.path.exists(metadata_path):
        print(f"Notice: {metadata_path} not found. Skipping.")
        sys.exit(0)

    with open(metadata_path, "r", encoding="utf-8") as f:
        content = f.read()

    content = re.sub(r"CurrentVersion:\s*.+", f"CurrentVersion: {version}", content)
    content = re.sub(r"CurrentVersionCode:\s*.+", f"CurrentVersionCode: {version_code}", content)

    # Pin the version explicitly so F-Droid builds do not depend on git metadata,
    # which is absent or shallow in their build sandbox.
    new_build = f"""Builds:
  - versionName: {version}
    versionCode: {version_code}
    commit: {tag}
    subdir: app
    gradle:
      - yes
      - -PversionCode={version_code}
      - -PversionName={version}"""

    content = re.sub(
        r"Builds:[\s\S]*?\n\nAutoUpdateMode:",
        new_build + "\n\nAutoUpdateMode:",
        content,
    )

    with open(metadata_path, "w", encoding="utf-8") as f:
        f.write(content)

    print(f"Updated {metadata_path} to version {version} (code: {version_code}, tag: {tag})")


if __name__ == "__main__":
    main()

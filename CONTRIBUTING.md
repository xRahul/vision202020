# Contributing

## Requirements

- JDK 21
- Android SDK Platform 36
- Git

## Build and test

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

A release APK needs a keystore:

```bash
KEYSTORE_FILE=/path/to/release.keystore \
KEYSTORE_PASSWORD=... \
KEY_ALIAS=... \
KEY_PASSWORD=... \
./gradlew assembleRelease
```

Never commit a keystore, password, token, or signing configuration.

## Code style

- Kotlin, Jetpack Compose, and Glance conventions already used in the repository.
- Keep changes focused. Do not add a dependency for a few lines.
- Do not add `android.permission.INTERNET` or any network SDK.
- Add or update tests for non-trivial logic.
- Run the full test and lint commands before opening a pull request.

## Commits

Use Conventional Commits:

```text
feat: add a feature
fix: correct a bug
docs: update documentation
ci: change a workflow
chore: maintenance
```

## Pull requests

- Explain what changed and why.
- List the commands you ran.
- Include screenshots for UI changes.
- Confirm that no new permission or network access was added.

## Releases

Releases are created only by the GitHub Actions release workflow. Do not create tags or GitHub releases by hand.

# Translating Vision 20-20-20

Translations are managed with Weblate.

Project: https://hosted.weblate.org/projects/vision-20-20-20/

## Components

| Component | Source file | Notes |
|---|---|---|
| Android strings | `app/src/main/res/values/strings.xml` | App UI text |
| Fastlane metadata | `fastlane/metadata/android/en-US/` | Store title, descriptions, changelogs |

## Rules

- Use the existing English terms for the 20-20-20 rule, eye rest, and work session.
- Do not translate the app name.
- Keep placeholders such as `%s`, `%d`, and `%1$s` unchanged.
- Keep store descriptions under the same limits as English: short description at most 80 characters.
- Do not add a new locale until at least the app strings are translated.

## Adding a locale manually

1. Copy `app/src/main/res/values/strings.xml` to `app/src/main/res/values-<locale>/strings.xml`.
2. Translate only the values, not the resource names.
3. Run `./gradlew lintDebug` and `./gradlew assembleDebug`.
4. Open a pull request with the new locale.

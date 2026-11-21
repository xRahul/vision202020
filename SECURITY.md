# Security Policy

## Supported versions

Only the latest GitHub release receives security fixes. Older releases are not supported.

## Reporting a vulnerability

Report privately through GitHub:

**https://github.com/xRahul/vision202020/security/advisories/new**

Do not open a public issue for a vulnerability. Include:

- affected app version and `versionCode`;
- Android version and device model;
- install source (GitHub Releases, Obtainium, F-Droid, IzzyOnDroid);
- reproduction steps;
- impact and any proof of concept.

## Scope

In scope:

- the Android app and its manifest permissions;
- the release workflow and published APK artifacts;
- the F-Droid recipe in `metadata/`;
- dependency and signing configuration.

Out of scope:

- vulnerabilities in Android itself or the device vendor's software;
- issues that require a rooted device or physical access;
- social engineering.

## Response

- Acknowledgement target: 7 days.
- Triage and fix target: depends on severity and complexity.
- No bug bounty is offered.

## Privacy

The app has no `INTERNET` permission and no telemetry. A report does not need to include personal data; redact logcat lines that contain device identifiers.

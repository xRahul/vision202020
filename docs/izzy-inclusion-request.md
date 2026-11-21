# IzzyOnDroid inclusion request

Status: ready to submit. No request has been opened yet.

## Preflight

| Criterion | Current state |
|---|---|
| Tagged GitHub release | Yes, `v1.1.7` |
| Release APK under 30 MB | Yes, 2,014,975 bytes |
| Release-signed, not debug-signed | Yes, `CN=Vision 20-20-20` |
| Fastlane title, descriptions, icon, feature graphic | Present |
| Fastlane changelog | `fastlane/metadata/android/en-US/changelogs/43.txt` |
| No proprietary dependencies or trackers | No `INTERNET`, no analytics, no ads |
| Package name stable | `in.rahulja.vision202020` |
| Source repository public | https://github.com/xRahul/vision202020 |

## Submit

Open an issue at https://codeberg.org/IzzyOnDroid/repodata/issues and use this body:

```markdown
App: Vision 20-20-20
Package: in.rahulja.vision202020
Source: https://github.com/xRahul/vision202020
License: GPL-3.0-or-later
Release: https://github.com/xRahul/vision202020/releases/latest

Summary:
Prevent digital eye strain with the science-backed 20-20-20 rule. 100% offline.

Description:
Vision 20-20-20 is an offline Android eye-rest timer. Every 20 minutes of screen
time it asks you to look at an object 20 feet away for 20 seconds. The work
session stays free of overlays; only the 20-second break uses a full-screen
guided rest screen.

- No INTERNET permission, no analytics, no ads.
- Screen-aware timer that pauses when the device locks.
- Quick Settings tile and home-screen widget.
- GPL-3.0-or-later.

Screenshots:
https://github.com/xRahul/vision202020/tree/main/fastlane/metadata/android/en-US/images/phoneScreenshots
```

## After inclusion

Verify the package page returns HTTP 200:

```bash
curl -sL -o /dev/null -w '%{http_code}\n' \
  "https://apt.izzysoft.de/fdroid/index/apk/in.rahulja.vision202020"
```

Add the IzzyOnDroid badge to README only after this returns `200`.

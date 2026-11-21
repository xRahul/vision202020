# Repo Trust, Supply-Chain & Distribution Hardening Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every public trust signal on `xRahul/vision202020` true, verifiable, and automatic: secure CI, signed and attested releases, dependency transparency, valid F-Droid/IzzyOnDroid listings, and a README that proves the claims it makes.

**Architecture:** Keep the existing single-module Android app and release workflow. Add security workflows and repo settings around them; do not rewrite the app. Treat the GitHub release as the canonical artifact, the F-Droid recipe as the store contract, and Fastlane as the single source of store copy. Release artifacts become: one universal release APK, one `.sha256`, and a GitHub build-provenance attestation. Debug APKs leave the public release.

**Tech Stack:** Kotlin 2.3.21, AGP 9.2.1, Gradle 9.7.1, JDK 21, GitHub Actions, CodeQL, OpenSSF Scorecard, Renovate, `gh` CLI, F-Droid `fdroidserver`, Fastlane metadata.

**Spec:** This plan and the live audit below. No separate design doc exists.

---

## Audit Baseline (verified 2026-11-21)

### Repository state

| Surface | Evidence | Verdict |
|---|---|---|
| Local branch | `main...origin/main [behind 1]`; local `versionCode=39/versionName=1.1.5`; origin `43/1.1.7` | Stale; pull first |
| CI | `.github/workflows/ci.yml`: `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleRelease`; recent runs green | Working; missing supply-chain steps |
| Release workflow | `.github/workflows/release.yml`; v1.1.6 green | Working; assets incomplete |
| Release assets v1.1.7 | `in.rahulja.vision202020-v1.1.7-release.apk` = 2,014,975 B; `...-debug.apk` = 21,973,349 B; no `.sha256`; no attestation | Debug asset pollutes release; no integrity proof |
| Signing | `apksigner verify --print-certs` on v1.1.7: `CN=Vision 20-20-20`, SHA-256 `9ec14141874c3a8f2e3843f52582c311880fe5417eb6f097fdf8515db92f2511` | Real release key; fingerprint not published |
| Secret scanning | enabled; push protection enabled | Good |
| Dependabot alerts | disabled | Gap |
| Private vulnerability reporting | disabled | Gap |
| CodeQL | no workflow; code-scanning alerts API returns 0 | Gap, not clean |
| Dependency graph | no submission workflow | Gap |
| Branch protection | `Branch not protected` (404) | Gap |
| Actions | tags only (`@v4`, `@v5`); no `timeout-minutes` | Scorecard/reproducibility risk |
| Repo metadata | no topics; Discussions off; Wiki/Projects on; community health 42% | Gap |
| Store workflow | `.github/workflows/store-publish.yml` prints a "ready" summary; does not sync anything | Misleading; reframe |

### F-Droid and IzzyOnDroid

| Surface | Evidence | Verdict |
|---|---|---|
| F-Droid MR | `fdroid/fdroiddata!57825` "New app: Vision 20-20-20" closed 2026-11-17 | Rejected; the inclusion policy requires 3 months of active development unless an exception applies |
| F-Droid eligibility | Policy: "actively developed for at least 3 months" unless proven users, author maintains other F-Droid apps, or event app. First commit 2026-11-17; current date 2026-11-21 | Earliest normal submission: 2027-02-17 |
| F-Droid recipe | `metadata/in.rahulja.vision202020.yml`: `commit: v1.1.5` (tag, not full SHA); `Categories: Health & Fitness`, `Time`; `AutoUpdateMode: Version v%v` with `UpdateCheckMode: Tags`; no `UpdateCheckData`; includes `Summary`/`Description` that belong in Fastlane; local file stale at 1.1.5 | Multiple schema/policy defects |
| F-Droid categories | `config/categories.yml` has `Digital Wellbeing`, `Timer`, `Time Tracker`; does **not** have `Health & Fitness` or `Time` | Invalid categories |
| F-Droid auto-update | Docs: with `UpdateCheckMode: Tags`, `AutoUpdateMode` must be `Version` with no pattern; `UpdateCheckData` may read `version.properties` | Current recipe will not auto-update correctly |
| F-Droid build commit | Reviewer requirement: full 40-char hash, not tag/branch | Current recipe fails |
| IzzyOnDroid | `apt.izzysoft.de/fdroid/index/apk/in.rahulja.vision202020` returns 404; shield reports package not found; no request issue found in Codeberg tracker | Not listed; no request exists |

### Fastlane and APK contents

| Item | Evidence | Verdict |
|---|---|---|
| Title | 15 chars | Good |
| Short description | 79 chars including trailing newline; limit 80 | Valid but tight |
| Full description | 2,512 chars | Good |
| Icon | 512x512 RGBA PNG | Good |
| Feature graphic | 1024x500 RGBA PNG | Good |
| Changelogs | `fastlane/metadata/android/en-US/changelogs/` missing | Gap; F-Droid/Izzy release notes absent |
| `dependenciesInfo` | `includeInApk = false`, `includeInBundle = false` already set | Already fixed |
| Dependency metadata | APK has `META-INF/com/android/build/gradle/app-metadata.properties`; no `DependencyInfoBlock` binary | Acceptable |
| Native libs | universal APK contains `libandroidx.graphics.path.so`, `libdatastore_shared_counter.so` for 4 ABIs; total 73,584 B uncompressed | Universal is fine; ABI splits not worth it |
| Network | manifest has no `INTERNET`; `ACCESS_NETWORK_STATE` removed with `tools:node="remove"` | Strong privacy claim is true |
| Backup | `android:allowBackup="true"` | Contradicts "100% offline & private" expectations; decision needed |
| Permissions | APK: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `SYSTEM_ALERT_WINDOW`, `POST_NOTIFICATIONS`, `USE_FULL_SCREEN_INTENT`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE`, plus `WAKE_LOCK` added by `androidx.work:work-runtime:2.7.1` (Glance widget worker) | Need a README permissions table; document `WAKE_LOCK` as Glance-owned |

### Roadmap suggestions: verdicts

| Suggestion | Verdict | Reason |
|---|---|---|
| Strip `DependencyInfoBlock` | Already done | `dependenciesInfo` disabled; no binary block in APK |
| Fastlane listing | Mostly done | Add `changelogs/`; keep copy in Fastlane, remove from fdroiddata recipe |
| F-Droid reproducible builds | Phase 2, not first submission | Signature switch after first inclusion forces user reinstall; verify before promising |
| Publish signing cert fingerprint | Do now | Verified value available |
| OpenSSF Scorecard | Do | Free, weekly, exposes a real trust score |
| Pin Actions to SHAs | Do | Renovate `helpers:pinGitHubActionDigests` |
| AboutLibraries | Defer | Adds dependency/UI for a small app; not a trust blocker |
| CycloneDX SBOM | Defer | Attestation + checksum already prove artifact provenance; SBOM adds plugin risk |
| Weblate | Defer | Single locale today; external service overhead |
| git-cliff/release-please | Minimal alternative | GitHub release notes already work; generate Fastlane changelog from commits |
| Per-ABI split APKs | Reject | Universal APK is 1.9 MB; transitive native libs total 73 KB |
| Roborazzi/Paparazzi | Defer | Manual screenshots exist; not a trust blocker |
| Self-hosted F-Droid repo | Defer | High maintenance; official inclusion first |
| CodeRabbit / SonarCloud | Optional | Third-party code access and account setup; not required for trust |
| Central `xRahul/.github` | Separate task | Useful across 3 repos; this repo gets its own files first |

---

## Implementation Status (2026-11-21)

Implemented in this pass:

- `SECURITY.md`, `CONTRIBUTING.md`, issue templates, PR template, README trust/downloads/privacy/permissions sections.
- CI pinned to commit SHAs, job timeout, SBOM generation, SARIF lint upload.
- CodeQL, dependency graph submission, dependency review, and OpenSSF Scorecard workflows.
- Release workflow: full-SHA F-Droid pin, Fastlane changelog, tag/version guards, checksum, SBOM, cosign signature, build provenance attestation, signing-cert assertion, debug APK removed from releases.
- `renovate.json` with grouped updates and action SHA pinning.
- F-Droid recipe fixed for categories, commit pin, auto-update, and Fastlane-owned copy; `update_fdroid_metadata.py` validates a full SHA.
- Fastlane changelog for version 43.
- CycloneDX SBOM plugin, AboutLibraries license screen, local crash log, and an update link that does not add network permission.
- CodeRabbit config and translation/Izzy request docs.

Still manual or blocked:

- Dependabot alerts, private vulnerability reporting, Discussions, ruleset, Renovate app, CodeRabbit app, keystore backup confirmation.
- IzzyOnDroid issue (Codeberg account) and F-Droid MR (blocked until `2027-02-17` or an accepted exception).
- Weblate project, screenshot regression plugin, central `xRahul/.github` repository, self-hosted F-Droid repo.

Release proof (v1.1.8, 2026-11-21):

- Workflow run `44866962785` completed successfully.
- Assets: APK, `.sha256`, CycloneDX 1.6 SBOM, cosign bundle, provenance bundle.
- `sha256sum -c` passed; cert SHA-256 matches `9ec141...2511`.
- `gh attestation verify` passed; cosign `verify-blob` passed against the `.sha256` sidecar, not the APK.
- F-Droid recipe pinned to full commit `106b47335f573c6974ef018211dc2bb9befd6648`, version 1.1.8 / code 49.
- Scorecard `Signed-Releases` rose to 2/10: 2 signed artifacts in 1 of the last 5 releases. It reaches 10 when 5 releases are signed.

---

## Target State

1. `main` pushes run CI; PRs run CI + CodeQL + dependency review.
2. Dependency graph is populated on every `main` push; Dependabot alerts can fire.
3. Weekly CodeQL and OpenSSF Scorecard runs publish SARIF and a score.
4. Every action is pinned to a full SHA; every job has a timeout and least-privilege permissions.
5. A release publishes only the signed release APK plus `.sha256`, with a GitHub build-provenance attestation.
6. The release workflow refuses to publish if the signing cert, tag, or version identity is wrong.
7. F-Droid recipe is schema-valid, uses a full commit SHA, valid categories, `AutoUpdateMode: Version`, and `UpdateCheckData` from `version.properties`.
8. Fastlane carries title, short/full description, icon, feature graphic, screenshots, and per-version changelogs.
9. IzzyOnDroid has a real inclusion request; F-Droid has a resubmitted MR.
10. README badges and claims all resolve; no dead F-Droid/Izzy badge before listing.
11. `SECURITY.md`, private vulnerability reporting, issue templates, and Discussions are live.
12. Branch `main` is protected against deletion and force-push without blocking the release bot.

---

## Global Constraints

- Do not change `applicationId` (`in.rahulja.vision202020`), signing key, or version scheme.
- Do not add `android.permission.INTERNET` or any network SDK.
- Release output stays universal (no ABI/density splits).
- Never commit a keystore, password, token, or `.jks`.
- Never hand-create a tag/release outside the release workflow.
- All new workflow actions use full commit SHAs in the final state.
- All user-facing claims must be live-checked; a 404 badge is worse than no badge.
- F-Droid metadata lives in the app repo as a mirror; the fdroiddata MR is the actual submission.
- Keep the solo-maintainer direct-push model unless the user explicitly chooses PR-only.

---

## Review Focus

1. **F-Droid build environment:** AGP 9.2.1, Gradle 9.7.1, compileSdk 36, and JDK 21 may not be available on the F-Droid buildserver. The first MR pipeline is the real test; have a fallback recipe/toolchain plan.
2. **Signature continuity:** If F-Droid signs the first accepted build with its own key, enabling `Binaries:` later changes the signing key and forces users to uninstall/reinstall. Decide before submission.
3. **Release bot vs branch protection:** A strict "require PR" ruleset blocks `git push origin main` from the release workflow. Either keep direct push or add a bypass actor.
4. **Tag reruns:** `workflow_dispatch` rerun after a partial failure may find an existing tag. The workflow must fail with a clear message, not force-move a published tag.
5. **Version identity drift:** A manually created tag pointing at a commit with an older `version.properties` would publish a duplicate/regressed `versionCode`. Guard this.
6. **Dependency submission on forks:** `contents: write` is required; fork PRs cannot submit. Run submission only on `main`.
7. **CodeQL Kotlin extraction:** Manual build is required for reliable Kotlin coverage; if the extractor fails, fall back to `build-mode: none` and document reduced coverage.
8. **Third-party reviewers:** Renovate/CodeRabbit/SonarCloud receive repository access. Treat installation as an explicit user decision.

---

## Phase 0 — Preparation

### Task 0.1: Sync, baseline, and freeze

**Files:** none.

- [ ] **Step 1: Sync local `main`**

```bash
cd /home/rahul/projects/vision202020
git pull --ff-only
git status -sb
cat version.properties
```

Expected: clean; `versionCode=41`, `versionName=1.1.6`.

- [ ] **Step 2: Confirm release key backup**

User confirms the release keystore and passwords are backed up outside GitHub Secrets. Do not proceed with release-workflow changes until confirmed.

- [ ] **Step 3: Record current release assets**

```bash
gh release view v1.1.6 --json tagName,assets \
  --jq '{tag:.tagName,assets:[.assets[]|{name,size}]}'
```

Expected: debug + release APKs, no `.sha256`.

- [ ] **Step 4: Decide no releases during this work**

Do not trigger the release workflow until Phase 3 is merged; otherwise v1.1.7 is published without attestation/checksum.

---

## Phase 1 — Repo Hygiene, Security Policy, and README Truth

### Task 1.1: `SECURITY.md` and private vulnerability reporting

**Files:**
- Create: `SECURITY.md`

**Content requirements:**
- Supported versions: latest release only.
- Reporting: GitHub private vulnerability advisory; no public zero-day issue.
- Scope: Android app, release workflow, F-Droid recipe.
- Response target: acknowledge within 7 days; no bounty.
- Explicit note: app has no `INTERNET` permission and no telemetry.

- [ ] **Step 1: Write `SECURITY.md`**

- [ ] **Step 2: Commit**

```bash
git add SECURITY.md
git commit -m "docs: add security policy and private reporting guidance"
```

- [ ] **Step 3: Enable private vulnerability reporting**

User action: Repository **Settings → Code security → Private vulnerability reporting → Enable**. If the token has admin scope, API alternative:

```bash
gh api -X PUT repos/xRahul/vision202020/private-vulnerability-reporting
```

- [ ] **Step 4: Verify**

```bash
gh api repos/xRahul/vision202020/private-vulnerability-reporting --jq '.enabled'
```

Expected: `true`.

---

### Task 1.2: Issue templates, PR template, and contributing guide

**Files:**
- Create: `.github/ISSUE_TEMPLATE/bug_report.yml`
- Create: `.github/ISSUE_TEMPLATE/feature_request.yml`
- Create: `.github/ISSUE_TEMPLATE/config.yml`
- Create: `.github/PULL_REQUEST_TEMPLATE.md`
- Create: `CONTRIBUTING.md`

**Bug report fields:** Android version, device model, app version (from Settings/About), install source (GitHub/F-Droid/Izzy/Obtainium), steps, expected/actual, logcat excerpt, screenshots.

**Feature request fields:** problem, proposed behavior, alternatives, scope.

**`config.yml`:** disable blank issues; link to Discussions.

**PR template:** what changed, why, test commands run, screenshots for UI, checklist (tests, lint, no new permissions, no network).

**CONTRIBUTING:** JDK 21, `./gradlew testDebugUnitTest lintDebug assembleDebug`, Conventional Commits, no new permissions without discussion, release process is workflow-only.

- [ ] **Step 1: Create the files**

- [ ] **Step 2: Validate YAML**

```bash
yamllint .github/ISSUE_TEMPLATE/*.yml
```

Expected: exit 0.

- [ ] **Step 3: Commit**

```bash
git add .github/ISSUE_TEMPLATE .github/PULL_REQUEST_TEMPLATE.md CONTRIBUTING.md
git commit -m "docs: add issue, PR, and contribution templates"
```

- [ ] **Step 4: Enable Discussions**

User action: **Settings → General → Features → Discussions → Enable**.

- [ ] **Step 5: Verify community profile**

```bash
gh api repos/xRahul/vision202020/community/profile \
  --jq '{health:.health_percentage, files:.files}'
```

Expected: health rises from 42%; `contributing`, `issue_template`, `pull_request_template` non-null.

---

### Task 1.3: Repository metadata

**Files:** none.

- [ ] **Step 1: Set description, homepage, and topics**

```bash
gh repo edit xRahul/vision202020 \
  --description "Offline 20-20-20 eye-rest timer for Android with no network permission" \
  --add-topic android,kotlin,jetpack-compose,eye-health,20-20-20,foss,offline,privacy,timer,glance-widget
```

- [ ] **Step 2: Disable unused Wiki/Projects**

User action: **Settings → General → Features**. Disable Wiki and Projects unless actively used.

- [ ] **Step 3: Verify**

```bash
gh api repos/xRahul/vision202020 --jq '{topics,has_discussions,has_wiki,has_projects}'
```

Expected: topics populated; `has_discussions=true`; `has_wiki=false`; `has_projects=false`.

---

### Task 1.4: README trust section

**Files:**
- Modify: `README.md`

**Add:**
1. Badge row: CI, latest release, total downloads, license, API 26+. Add CodeQL/Scorecard only after those workflows exist.
2. **Install and verify**:
   - Download `in.rahulja.vision202020-vX.Y.Z-release.apk` and `.sha256`.
   - `sha256sum -c in.rahulja.vision202020-vX.Y.Z-release.apk.sha256`
   - `gh attestation verify in.rahulja.vision202020-vX.Y.Z-release.apk -R xRahul/vision202020`
   - Signing cert SHA-256: `9ec14141874c3a8f2e3843f52582c311880fe5417eb6f097fdf8515db92f2511`; verify with `apksigner verify --print-certs`.
3. **Permissions table**: one row per manifest permission, why it exists.
4. **Update channels**: GitHub Releases, `releases.atom`, Obtainium deep link `obtainium://add/https://github.com/xRahul/vision202020`.
5. **Privacy proof**: no `INTERNET`, no `ACCESS_NETWORK_STATE`, no ads/trackers/telemetry.
6. **Do not add** F-Droid or IzzyOnDroid badges until the listing URLs return 200.

- [ ] **Step 1: Edit README**

- [ ] **Step 2: Verify every badge URL returns 200**

```bash
for u in \
  "https://github.com/xRahul/vision202020/actions/workflows/ci.yml/badge.svg" \
  "https://img.shields.io/github/v/release/xRahul/vision202020" \
  "https://img.shields.io/github/downloads/xRahul/vision202020/total" \
  "https://img.shields.io/github/license/xRahul/vision202020"; do
  printf '%s -> ' "$u"; curl -s -o /dev/null -L -w '%{http_code}\n' "$u"
done
```

Expected: all `200`.

- [ ] **Step 3: Verify no dead store links**

```bash
grep -n -iE 'f-droid|izzy|apt\.izzy' README.md || true
```

Expected: no live listing links until Phase 5/6 complete.

- [ ] **Step 4: Commit**

```bash
git add README.md
git commit -m "docs: document verification, updates, and permissions"
```

---

## Phase 2 — CI Supply Chain and Static Analysis

### Task 2.1: CodeQL for Kotlin/Java

**Files:**
- Create: `.github/workflows/codeql.yml`

**Required workflow shape:**

```yaml
name: CodeQL

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]
  schedule:
    - cron: '23 4 * * 1'

permissions:
  contents: read
  security-events: write
  actions: read

concurrency:
  group: codeql-${{ github.ref }}
  cancel-in-progress: true

jobs:
  analyze:
    runs-on: ubuntu-24.04
    timeout-minutes: 30
    strategy:
      fail-fast: false
      matrix:
        language: [java-kotlin]
    steps:
      - uses: actions/checkout@<SHA> # v6
      - uses: actions/setup-java@<SHA> # v6
        with:
          distribution: temurin
          java-version: '21'
      - uses: github/codeql-action/init@<SHA> # v4
        with:
          languages: ${{ matrix.language }}
          build-mode: manual
      - uses: gradle/actions/setup-gradle@<SHA> # v5
      - run: ./gradlew assembleDebug -x lint -x test
      - uses: github/codeql-action/analyze@<SHA> # v4
        with:
          category: "/language:${{ matrix.language }}"
```

- [ ] **Step 1: Create the workflow with pinned SHAs**

- [ ] **Step 2: Validate YAML**

```bash
yamllint .github/workflows/codeql.yml
```

- [ ] **Step 3: Commit and push to a branch, open PR**

- [ ] **Step 4: Verify the run**

```bash
gh run list --workflow=codeql.yml -L 3
gh api 'repos/xRahul/vision202020/code-scanning/alerts?state=open' --jq 'length'
```

Expected: run green; alert count is a number (0 is acceptable only after a successful scan).

- [ ] **Step 5: Fallback if manual extraction fails**

Change `build-mode: manual` to `build-mode: none` and remove the Gradle build step; document reduced Kotlin coverage. If `none` also fails, use `autobuild` and record the limitation.

---

### Task 2.2: Dependency graph submission

**Files:**
- Create: `.github/workflows/dependency-graph.yml`

**Why a separate workflow:** submission needs `contents: write`; keep that permission off the main CI job.

```yaml
name: Dependency Graph

on:
  push:
    branches: [main]
  workflow_dispatch:

permissions:
  contents: write

jobs:
  submit:
    runs-on: ubuntu-24.04
    timeout-minutes: 15
    steps:
      - uses: actions/checkout@<SHA> # v6
      - uses: actions/setup-java@<SHA> # v6
        with:
          distribution: temurin
          java-version: '21'
      - uses: gradle/actions/dependency-submission@<SHA> # v5
```

- [ ] **Step 1: Create the workflow**
- [ ] **Step 2: Commit, push, verify the run**
- [ ] **Step 3: Verify the graph is populated**

```bash
gh api repos/xRahul/vision202020/dependency-graph/sbom --jq '.sbom.packages | length'
```

Expected: a positive package count.

- [ ] **Step 4: Enable Dependabot alerts**

User action: **Settings → Code security → Dependabot alerts → Enable**. API alternative if admin:

```bash
gh api -X PATCH repos/xRahul/vision202020 \
  -f 'security_and_analysis[dependabot_security_updates][status]=enabled'
```

- [ ] **Step 5: Verify**

```bash
gh api repos/xRahul/vision202020 --jq '.security_and_analysis'
```

Expected: `dependabot_security_updates.status = enabled`.

---

### Task 2.3: Dependency review on pull requests

**Files:**
- Create: `.github/workflows/dependency-review.yml`

```yaml
name: Dependency Review

on:
  pull_request:
    branches: [main]

permissions:
  contents: read
  pull-requests: write

jobs:
  review:
    runs-on: ubuntu-24.04
    timeout-minutes: 10
    steps:
      - uses: actions/checkout@<SHA> # v6
      - uses: actions/dependency-review-action@<SHA> # v5
        with:
          fail-on-severity: high
```

- [ ] **Step 1: Create the workflow**
- [ ] **Step 2: Commit and open a trivial PR**
- [ ] **Step 3: Verify the check appears and passes**
- [ ] **Step 4: Merge**

---

### Task 2.4: Pin actions, add timeouts, and remove mutable tags

**Files:**
- Modify: `.github/workflows/ci.yml`
- Modify: `.github/workflows/release.yml`
- Modify: `.github/workflows/store-publish.yml`
- Modify: `.github/workflows/codeql.yml`
- Modify: `.github/workflows/dependency-graph.yml`
- Modify: `.github/workflows/dependency-review.yml`

**Method:** Let Renovate pin (Task 4.1) or resolve SHAs manually:

```bash
gh api repos/actions/checkout/commits/v6 --jq .sha
gh api repos/actions/setup-java/commits/v6 --jq .sha
gh api repos/actions/upload-artifact/commits/v6 --jq .sha
gh api repos/actions/setup-python/commits/v6 --jq .sha
gh api repos/gradle/actions/commits/v5 --jq .sha
gh api repos/github/codeql-action/commits/v4 --jq .sha
gh api repos/actions/attest-build-provenance/commits/v3 --jq .sha
gh api repos/actions/dependency-review-action/commits/v5 --jq .sha
gh api repos/softprops/action-gh-release/commits/v3 --jq .sha
gh api repos/ossf/scorecard-action/commits/v2.4.3 --jq .sha
```

- [ ] **Step 1: Replace every `uses: owner/action@vN` with `uses: owner/action@<40-char-sha> # vN`**
- [ ] **Step 2: Add `timeout-minutes` to every job**
- [ ] **Step 3: Verify no mutable tags remain**

```bash
grep -rnE 'uses: [^ ]+@v[0-9]' .github/workflows/ || echo "all pinned"
```

Expected: `all pinned`.

- [ ] **Step 4: Verify workflows parse**

```bash
for f in .github/workflows/*.yml; do yamllint "$f" || exit 1; done
```

- [ ] **Step 5: Commit**

```bash
git add .github/workflows
git commit -m "ci: pin actions to commit SHAs and cap job runtime"
```

---

### Task 2.5: Lint SARIF upload (investigate, likely defer)

**Files:**
- Modify: `app/build.gradle.kts` (only if supported)
- Modify: `.github/workflows/ci.yml`

**Decision:** AGP 9.2.1 may support `lint { sarifReport = true }`, but this must be verified against the actual AGP DSL before committing. If it is not a stable one-line change, skip SARIF; `lintDebug` already gates errors and CodeQL covers SAST. Do not add a custom XML-to-SARIF converter.

- [ ] **Step 1: Check the AGP DSL**

```bash
grep -R "sarifReport" ~/.gradle/caches/modules-2/files-2.1/com.android.tools.build/gradle/9.2.1 2>/dev/null | head
```

- [ ] **Step 2: If supported, enable and upload**

```yaml
- name: Upload Lint SARIF
  if: always()
  uses: github/codeql-action/upload-sarif@<SHA> # v4
  with:
    sarif_file: app/build/reports/lint-results-debug.sarif
```

- [ ] **Step 3: If not supported, record the decision in the commit message and move on**

---

### Task 2.6: OpenSSF Scorecard

**Files:**
- Create: `.github/workflows/scorecard.yml`

```yaml
name: OpenSSF Scorecard

on:
  branch_protection_rule:
  schedule:
    - cron: '27 2 * * 6'
  push:
    branches: [main]

permissions:
  security-events: write
  id-token: write
  contents: read
  actions: read

jobs:
  analysis:
    runs-on: ubuntu-24.04
    timeout-minutes: 20
    steps:
      - uses: actions/checkout@<SHA> # v6
        with:
          persist-credentials: false
      - uses: ossf/scorecard-action@<SHA> # v2.4.3
        with:
          results_file: results.sarif
          results_format: sarif
          publish_results: true
      - uses: actions/upload-artifact@<SHA> # v6
        with:
          name: scorecard-sarif
          path: results.sarif
          retention-days: 5
      - uses: github/codeql-action/upload-sarif@<SHA> # v4
        with:
          sarif_file: results.sarif
```

- [ ] **Step 1: Create the workflow**
- [ ] **Step 2: Commit and run once**
- [ ] **Step 3: Verify the score endpoint**

```bash
curl -sL -o /dev/null -w '%{http_code}\n' \
  "https://api.securityscorecards.dev/projects/github.com/xRahul/vision202020"
```

Expected: `200` after the first successful run.

- [ ] **Step 4: Add the badge to README only after 200**

---

## Phase 3 — Release Hardening

### Task 3.1: Attestation, checksum, and permissions

**Files:**
- Modify: `.github/workflows/release.yml`

**Permissions:**

```yaml
permissions:
  contents: write
  id-token: write
  attestations: write
```

**After `Prepare Release Artifacts`:**

```yaml
- name: Write SHA-256 Checksums
  run: |
    cd artifacts
    sha256sum in.rahulja.vision202020-v${{ steps.versioning.outputs.version }}-release.apk \
      > in.rahulja.vision202020-v${{ steps.versioning.outputs.version }}-release.apk.sha256

- name: Attest Release APK
  uses: actions/attest-build-provenance@<SHA> # v3
  with:
    subject-path: artifacts/in.rahulja.vision202020-*-release.apk
```

**Release upload:**

```yaml
files: |
  artifacts/in.rahulja.vision202020-*-release.apk
  artifacts/in.rahulja.vision202020-*.apk.sha256
```

- [ ] **Step 1: Edit workflow**
- [ ] **Step 2: Verify locally that the snippet is syntactically valid**

```bash
yamllint .github/workflows/release.yml
```

- [ ] **Step 3: Commit**
- [ ] **Step 4: Run the workflow after Phase 3 is fully merged**
- [ ] **Step 5: Verify assets**

```bash
gh release view vX.Y.Z --json assets --jq '[.assets[]|{name,size}]'
```

Expected: release APK + `.sha256`; no debug APK.

- [ ] **Step 6: Verify attestation**

```bash
gh release download vX.Y.Z -p '*.apk' -O /tmp/opencode/v2020/release.apk
gh attestation verify /tmp/opencode/v2020/release.apk -R xRahul/vision202020
```

Expected: `Verification succeeded`.

---

### Task 3.2: Remove debug APK from public releases

**Files:**
- Modify: `.github/workflows/release.yml`

- [ ] **Step 1: Delete the debug copy from `Prepare Release Artifacts`**
- [ ] **Step 2: Optionally upload the debug APK as a workflow artifact**

```yaml
- name: Upload Debug APK
  uses: actions/upload-artifact@<SHA> # v6
  with:
    name: debug-apk
    path: app/build/outputs/apk/debug/app-debug.apk
    retention-days: 14
```

- [ ] **Step 3: Commit**
- [ ] **Step 4: Verify next release has one APK only**

---

### Task 3.3: Assert the release signing certificate

**Files:**
- Modify: `.github/workflows/release.yml`

**Expected fingerprint:** `9ec14141874c3a8f2e3843f52582c311880fe5417eb6f097fdf8515db92f2511`

**Step shape:**

```yaml
- name: Verify Release APK Is Signed
  run: |
    APK=app/build/outputs/apk/release/app-release.apk
    test -f "$APK" || { echo "ERROR: $APK missing"; exit 1; }
    APKSIGNER=$(ls "$ANDROID_HOME"/build-tools/*/apksigner | sort -V | tail -1)
    "$APKSIGNER" verify --print-certs "$APK"
    ACTUAL=$("$APKSIGNER" verify --print-certs "$APK" | sed -n 's/.*SHA-256 digest: //p' | head -1)
    EXPECTED=9ec14141874c3a8f2e3843f52582c311880fe5417eb6f097fdf8515db92f2511
    [ "$ACTUAL" = "$EXPECTED" ] || { echo "ERROR: signing key changed: $ACTUAL"; exit 1; }
```

- [ ] **Step 1: Add the assertion**
- [ ] **Step 2: Commit**
- [ ] **Step 3: Verify on the next release**
- [ ] **Step 4: If the key ever changes intentionally, update README + workflow in the same commit and announce it**

---

### Task 3.4: Tag/version guards, changelog, and ordering

**Files:**
- Modify: `.github/workflows/release.yml`
- Modify: `scripts/update_fdroid_metadata.py` (also Task 5.1)

**Guards to add before creating a tag:**

```bash
if git rev-parse -q --verify "refs/tags/$NEW_TAG" >/dev/null; then
  echo "ERROR: tag $NEW_TAG already exists; refusing to move it." >&2
  exit 1
fi
```

```bash
FILE_VER=$(sed -n 's/^versionName=//p' version.properties | tr -d '\r')
[ "$FILE_VER" = "$VERSION" ] || { echo "ERROR: version.properties=$FILE_VER, tag=$VERSION"; exit 1; }
```

**Changelog generation before the bump commit:**

```bash
PREV_TAG=$(git describe --tags --abbrev=0 --match 'v[0-9]*.[0-9]*.[0-9]*' 2>/dev/null || true)
mkdir -p fastlane/metadata/android/en-US/changelogs
if [ -n "$PREV_TAG" ]; then
  git log --no-merges --pretty='- %s' "$PREV_TAG..HEAD" \
    | head -c 480 > "fastlane/metadata/android/en-US/changelogs/$VERSION_CODE.txt"
else
  printf -- '- Initial release\n' > "fastlane/metadata/android/en-US/changelogs/$VERSION_CODE.txt"
fi
```

**Ordering to avoid the metadata fixed-point problem:**
1. Update `version.properties`.
2. Generate Fastlane changelog.
3. Commit both (`[skip ci]`).
4. `RELEASE_SHA=$(git rev-parse HEAD)`.
5. Run metadata script with `$RELEASE_SHA` (full hash).
6. Commit metadata mirror (`[skip ci]`).
7. Push `main`.
8. Tag `$RELEASE_SHA` (not the metadata commit).
9. Push tag; build from the tag.

- [ ] **Step 1: Add guards and changelog**
- [ ] **Step 2: Verify the script accepts a full SHA** (Task 5.1)
- [ ] **Step 3: Commit**
- [ ] **Step 4: Dry-run on a throwaway branch/tag in a fork if possible**
- [ ] **Step 5: Confirm `git tag -fa` and `--force` tag pushes are gone**

```bash
grep -n 'tag -f\|push origin.*--force' .github/workflows/release.yml || echo "no forced tags"
```

Expected: `no forced tags`.

---

### Task 3.5: Provenance documentation

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Add the exact verify commands** (Task 1.4 already lists them)
- [ ] **Step 2: Verify the commands against the latest release**

```bash
cd /tmp/opencode/v2020
gh release download v1.1.6 -p '*.apk' --clobber
gh attestation verify v1.1.6-release.apk -R xRahul/vision202020
```

Expected: succeeds once a release with attestation exists; document that older releases predate attestation.

---

## Phase 4 — Dependency Automation and Branch Protection

### Task 4.1: Renovate

**Files:**
- Create: `renovate.json`

```json
{
  "$schema": "https://docs.renovatebot.com/renovate-schema.json",
  "extends": [
    "config:recommended",
    ":dependencyDashboard",
    "helpers:pinGitHubActionDigests"
  ],
  "packageRules": [
    {
      "groupName": "AndroidX",
      "matchPackageNames": ["androidx.**"]
    },
    {
      "groupName": "Kotlin, Compose, AGP",
      "matchPackageNames": ["org.jetbrains.kotlin**", "com.android.**"]
    },
    {
      "groupName": "Testing",
      "matchPackageNames": ["junit**", "io.mockk**", "org.robolectric**", "androidx.test**"]
    }
  ]
}
```

- [ ] **Step 1: Create `renovate.json`**
- [ ] **Step 2: Commit**
- [ ] **Step 3: Install the Renovate GitHub App** (user action; grant only this repository)
- [ ] **Step 4: Verify Renovate opens the onboarding PR**
- [ ] **Step 5: Merge onboarding; verify grouped update PRs**
- [ ] **Step 6: Verify actions are pinned by the first Renovate PR**

---

### Task 4.2: Dependabot fallback

**Files:**
- Create: `.github/dependabot.yml` only if Renovate is not installed.

**Decision rule:** Do not run Dependabot version updates and Renovate together; duplicate PRs. Dependabot **alerts** stay enabled regardless.

- [ ] **Step 1: If Renovate is rejected, add `dependabot.yml` for `gradle` and `github-actions`**

- [ ] **Step 2: Verify alerts**

```bash
gh api 'repos/xRahul/vision202020/dependabot/alerts?state=open' --jq 'length'
```

Expected: a number (403 means alerts still disabled).

---

### Task 4.3: Branch ruleset

**Files:** none.

**Ruleset on `main`:**
- Block deletion.
- Block non-fast-forward (force) pushes.
- Do **not** require PRs yet; the release workflow pushes directly to `main`.
- Optional later: require `CI` and `CodeQL` status checks with a bypass for the release bot.

- [ ] **Step 1: Create the ruleset in GitHub UI or API**

UI: **Settings → Rules → Rulesets → New branch ruleset → target `main` → enable "Restrict deletions" and "Block force pushes" → Active.**

- [ ] **Step 2: Verify**

```bash
gh api repos/xRahul/vision202020/rulesets --jq '.[] | {name,target,enforcement}'
```

- [ ] **Step 3: Test the release workflow still pushes `main`** (workflow_dispatch patch; confirm green)

**If the user chooses PR-only later:** add a bypass actor for the release bot or switch the release workflow to a bot PAT; do not silently break releases.

---

## Phase 5 — F-Droid (prepare now, submit after eligibility gate)

### Task 5.0: Eligibility gate

**Rule:** F-Droid Inclusion Policy requires the app to be actively developed for at least 3 months, unless:
- the app has proven users;
- the author maintains other apps on F-Droid;
- the app is developed only for an event.

**Facts:** first commit `2026-11-17`; earliest normal submission `2027-02-17`. The previous MR was closed on this basis.

- [ ] **Step 1: Do not open a new MR before `2027-02-17` unless an exception is proven**
- [ ] **Step 2: Gather exception evidence if claiming one**
  - Downloads/users: GitHub release download counts, Obtainium installs, or store analytics.
  - Author's other F-Droid apps: links to accepted packages.
  - Event: event name and dates.
- [ ] **Step 3: Set a reminder for `2027-02-17`**
- [ ] **Step 4: Re-run this gate before Task 5.3**

### Task 5.1: Fix the recipe and the metadata script

**Files:**
- Modify: `metadata/in.rahulja.vision202020.yml`
- Modify: `scripts/update_fdroid_metadata.py`

**Canonical recipe (for v1.1.7 / code 43):**

```yaml
Categories:
  - Digital Wellbeing
  - Timer
License: GPL-3.0-or-later
AuthorName: Rahul Jain
AuthorWebSite: https://github.com/xRahul
SourceCode: https://github.com/xRahul/vision202020
IssueTracker: https://github.com/xRahul/vision202020/issues
AutoName: Vision 20-20-20
RepoType: git
Repo: https://github.com/xRahul/vision202020.git
Builds:
  - versionName: 1.1.7
    versionCode: 43
    commit: 3e1576c44670e90889738b05eb9076c10a7bdd0b
    subdir: app
    gradle:
      - yes
      - -PversionCode=43
      - -PversionName=1.1.7
AutoUpdateMode: Version
UpdateCheckMode: Tags ^v[0-9]+\.[0-9]+\.[0-9]+$
UpdateCheckData: version.properties|versionCode=(\d+)|.|versionName=(.+)
CurrentVersion: 1.1.7
CurrentVersionCode: 43
```

**Why each change:**
- `commit` must be a full 40-char hash, not `v1.1.7`.
- `Digital Wellbeing` + `Timer` are valid categories; `Health & Fitness` and `Time` are not.
- With `UpdateCheckMode: Tags`, `AutoUpdateMode` must be `Version` with no pattern.
- `UpdateCheckData` reads the next `versionCode`/`versionName` from `version.properties` at each tag.
- `Summary`/`Description` belong in Fastlane; the fdroiddata reviewer explicitly rejects them in metadata.

**Script changes:**
- Rename the third argument from `tag` to `commit_sha`.
- Validate `^[0-9a-f]{40}$`.
- Write `commit: {commit_sha}`.
- Write `AutoUpdateMode: Version`.
- Add/refresh `UpdateCheckData`.
- Keep `CurrentVersion`/`CurrentVersionCode`.
- Preserve LF endings.

- [ ] **Step 1: Edit the script**
- [ ] **Step 2: Update the local recipe with the v1.1.7 full SHA**
- [ ] **Step 3: Validate YAML and schema**

```bash
yamllint metadata/in.rahulja.vision202020.yml
python3 - <<'PY'
import yaml
p='metadata/in.rahulja.vision202020.yml'
d=yaml.safe_load(open(p))
assert d['Categories'] == ['Digital Wellbeing','Timer']
assert d['AutoUpdateMode'] == 'Version'
assert len(d['Builds'][0]['commit']) == 40
assert 'Summary' not in d and 'Description' not in d
assert d['UpdateCheckData'].startswith('version.properties|versionCode=')
print('recipe ok')
PY
```

- [ ] **Step 4: Run `fdroid lint`**

```bash
fdroid lint in.rahulja.vision202020
```

Expected: no errors. If it needs a full fdroiddata checkout, copy the metadata into a temporary clone and run there.

- [ ] **Step 5: Commit**

```bash
git add metadata/in.rahulja.vision202020.yml scripts/update_fdroid_metadata.py
git commit -m "fix(fdroid): valid categories, full commit pin, and tag auto-update"
```

---

### Task 5.2: Fastlane changelog

**Files:**
- Create: `fastlane/metadata/android/en-US/changelogs/43.txt`

- [ ] **Step 1: Write the v1.1.7 changelog** (≤500 chars, plain text, user-facing)

- [ ] **Step 2: Verify length**

```bash
wc -c fastlane/metadata/android/en-US/changelogs/43.txt
```

Expected: ≤500.

- [ ] **Step 3: Commit**

```bash
git add fastlane/metadata/android/en-US/changelogs/43.txt
git commit -m "docs: add Fastlane changelog for version 43"
```

---

### Task 5.3: Resubmit the fdroiddata merge request (after eligibility gate)

**Files:** external `fdroiddata` fork.

**Blocked until:** `2027-02-17`, or an exception in Task 5.0 is accepted by reviewers.

- [ ] **Step 0: Pass Task 5.0**
- [ ] **Step 1: Fork `fdroid/fdroiddata` and create branch `in.rahulja.vision202020`**
- [ ] **Step 2: Add only `metadata/in.rahulja.vision202020.yml`** (no images, no summary/description; Fastlane provides them)
- [ ] **Step 3: Use this MR description structure**
  - Summary of app and usage.
  - Why it exists / unique: zero overlay clutter, offline, no INTERNET permission, 20-second guided break.
  - Screenshots: raw GitHub URLs to the four Fastlane PNGs.
  - Checklist answers: author, Fastlane present, valid YAML/LF, latest version only, full commit hash, no unrelated files, tags + auto-update, AuthorName present.
  - Reproducible builds answer: state the chosen strategy (Task 5.4).
- [ ] **Step 4: Open MR; watch pipeline**
- [ ] **Step 5: Address reviewer comments; do not force-push unnecessarily**
- [ ] **Step 6: Verify listing**

```bash
curl -sL -o /dev/null -w '%{http_code}\n' \
  "https://f-droid.org/packages/in.rahulja.vision202020/"
```

Expected: `200` after inclusion.

---

### Task 5.4: Reproducible-build decision (locked: try RB first)

**Decision:** Add `Binaries` before the first accepted submission and let the F-Droid MR pipeline verify it. If verification fails, keep the recipe without `Binaries` and document that F-Droid signs with its own key.

**Recipe addition:**

```yaml
Binaries: https://github.com/xRahul/vision202020/releases/download/v%v/in.rahulja.vision202020-v%v-release.apk
```

**Risk:** The F-Droid buildserver must match AGP 9.2.1, Gradle 9.7.1, JDK 21, and SDK 36 closely enough to reproduce the APK byte-for-byte. The MR pipeline is the only reliable test.

- [ ] **Step 1: Add `Binaries` when the eligibility gate passes**
- [ ] **Step 2: Let the MR pipeline run the RB check**
- [ ] **Step 3: If it passes, keep `Binaries`; if it fails, remove it and record the failure reason in the MR**
- [ ] **Step 4: Document the signature policy in README**

---

### Task 5.5: Make the store workflow truthful

**Files:**
- Modify: `.github/workflows/store-publish.yml`

**Problems:**
- Name says "Sync" but it only prints a summary.
- Summary claims "Ready for Automated Crawl Ingestion" and "Zero Trackers: Verified" without checking the live listing.
- No `concurrency`, no timeout.

**Changes:**
- Rename to `Store Metadata Validation`.
- Keep Fastlane/F-Droid YAML validation.
- Replace "ready/verified" claims with "metadata validated; submission is manual".
- Add `concurrency` and `timeout-minutes`.
- Optionally check whether the F-Droid/Izzy URLs are live and print the result, without failing the build.

- [ ] **Step 1: Edit the workflow**
- [ ] **Step 2: Commit**
- [ ] **Step 3: Trigger via `workflow_dispatch` and verify the summary is accurate**

---

## Phase 6 — IzzyOnDroid

### Task 6.0: Eligibility preflight

**Rule:** IzzyOnDroid has no 3-month age rule. It rejects games, webview-only apps, apps with proprietary trackers, APKs over 30 MB, APKs signed with a debug key, and apps without tagged releases.

- [ ] **Step 1: Confirm the latest release APK is <30 MB** (v1.1.7 release is 2.0 MB)
- [ ] **Step 2: Confirm the tag matches `versionName`** (`v1.1.7`)
- [ ] **Step 3: Confirm Fastlane has title, short/full description, icon, feature graphic, screenshots, and a changelog**
- [ ] **Step 4: Confirm no proprietary dependencies/trackers** (`aapt2 dump permissions` shows no `INTERNET`)
- [ ] **Step 5: Confirm the APK is release-signed** (`apksigner verify --print-certs`)
- [ ] **Step 6: Only then open the request**

### Task 6.1: Open the inclusion request

**Files:** external Codeberg issue.

- [ ] **Step 1: Pass Task 6.0**
- [ ] **Step 2: Open an issue at `https://codeberg.org/IzzyOnDroid/repodata/issues`** with app name, package, source repo, license, summary, screenshots, and release link.
- [ ] **Step 3: Answer maintainer questions; provide test APK if requested**
- [ ] **Step 4: Verify listing**

```bash
curl -sL -o /dev/null -w '%{http_code}\n' \
  "https://apt.izzysoft.de/fdroid/index/apk/in.rahulja.vision202020"
```

Expected: `200` after inclusion.

- [ ] **Step 5: Add the Izzy badge to README only after 200**

---

### Task 6.2: Obtainium and RSS update channels

**Files:**
- Modify: `README.md` (already in Task 1.4)

- [ ] **Step 1: Verify the Obtainium deep link**

Link: `obtainium://add/https://github.com/xRahul/vision202020`

- [ ] **Step 2: Optionally submit to the Obtainium Apps index** (`apps.obtainium.imranr.dev`) after the release pattern is stable.
- [ ] **Step 3: Add the RSS feed** `https://github.com/xRahul/vision202020/releases.atom`.

---

## Phase 7 — Added Trust and Distribution Extras

These are in scope by user decision. Keep them behind Phases 1–4 so the trust baseline lands first.

### Task 7.1: CycloneDX SBOM

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `.github/workflows/release.yml`

- [ ] **Step 1: Add the plugin** `id("org.cyclonedx.bom")` with the current version resolved by Renovate
- [ ] **Step 2: Generate locally**

```bash
./gradlew cyclonedxBom
ls -l app/build/reports/bom.json
```

- [ ] **Step 3: Attach `app/build/reports/bom.json` to the GitHub release**
- [ ] **Step 4: Commit**

### Task 7.2: CodeRabbit

**Files:**
- Create: `.coderabbit.yaml`
- Modify: `README.md` if adding a review badge

- [ ] **Step 1: Add `.coderabbit.yaml`** with `language: en-US`, path filters for `app/src/main/java/**`, `app/src/test/**`, and `.github/workflows/**`, and `reviews.profile: chill`
- [ ] **Step 2: Commit and push the config**
- [ ] **Step 3: Install the CodeRabbit GitHub App** (manual, repository-scoped)
- [ ] **Step 4: Verify a review appears on a test PR**
- [ ] **Step 5: Do not add a badge unless the app is actually installed and reviewing**

### Task 7.3: AboutLibraries license screen

**Files:**
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/in/rahulja/vision202020/ui/about/AboutLibrariesScreen.kt`
- Modify: the Settings/About navigation entry

- [ ] **Step 1: Add `com.mikepenz.aboutlibraries.plugin` and `aboutlibraries-compose-m3`**
- [ ] **Step 2: Generate the licenses metadata** (`./gradlew exportLibraryDefinitions`)
- [ ] **Step 3: Add a Compose screen using `LibrariesContainer`**
- [ ] **Step 4: Add a test that the screen composes and lists at least one library**
- [ ] **Step 5: Run `./gradlew testDebugUnitTest lintDebug assembleDebug`**
- [ ] **Step 6: Commit**

### Task 7.4: Weblate

**Files:**
- Create: `docs/TRANSLATING.md`

- [ ] **Step 1: Create a Weblate project for `xRahul/vision202020`** (manual account)
- [ ] **Step 2: Add components for `app/src/main/res/values/strings.xml` and `fastlane/metadata/android/en-US/`**
- [ ] **Step 3: Add `docs/TRANSLATING.md` with the Weblate link and rules**
- [ ] **Step 4: Add the translation badge only after the project is live**
- [ ] **Step 5: Do not add locales until translations exist**

### Task 7.5: Screenshot regression tests

**Files:**
- Modify: `app/build.gradle.kts`
- Create: `app/src/test/java/in/rahulja/vision202020/ui/ScreenshotTest.kt`

- [ ] **Step 1: Add Roborazzi or Paparazzi (pick one, not both)**
- [ ] **Step 2: Record light and dark screenshots of the main screen**
- [ ] **Step 3: Run the screenshot test in CI on PRs**
- [ ] **Step 4: Store reference images under `app/src/test/screenshots/`**
- [ ] **Step 5: Commit**

### Task 7.6: Local-only crash log

**Files:**
- Create: `app/src/main/java/in/rahulja/vision202020/crash/CrashLogWriter.kt`
- Modify: `VisionApplication`

- [ ] **Step 1: Install a `Thread.UncaughtExceptionHandler` that appends the stack trace to app-private storage**
- [ ] **Step 2: Add a "Copy crash log" / "Share crash log" action in About**
- [ ] **Step 3: Add no network code, no Firebase, no Bugsnag**
- [ ] **Step 4: Test with a debug-only crash trigger**
- [ ] **Step 5: Commit**

### Task 7.7: Sigstore/cosign signatures

**Files:**
- Modify: `.github/workflows/release.yml`

- [ ] **Step 1: Install cosign with `sigstore/cosign-installer`**
- [ ] **Step 2: Sign the `.sha256` file keylessly**

```bash
cosign sign-blob --yes --bundle "${CHECKSUM}.sigstore.json" "$CHECKSUM"
```

- [ ] **Step 3: Upload the `.sigstore.json` bundle with the release**
- [ ] **Step 4: Document `cosign verify-blob` in README**
- [ ] **Step 5: Commit**

### Task 7.8: Central `xRahul/.github`

**Files:** separate public repository.

- [ ] **Step 1: Create `xRahul/.github`**
- [ ] **Step 2: Add `SECURITY.md`, `CONTRIBUTING.md`, `ISSUE_TEMPLATE/*.yml`, and `PULL_REQUEST_TEMPLATE.md`**
- [ ] **Step 3: Keep app-specific overrides in each repo**
- [ ] **Step 4: Verify GitHub applies the community files to all three apps**

### Task 7.9: Self-hosted F-Droid repo (optional, after official listing)

**Files:**
- Create: `.github/workflows/fdroid-repo.yml`

- [ ] **Step 1: Decide whether official F-Droid latency justifies a second repo**
- [ ] **Step 2: Generate a repository signing key and back it up**
- [ ] **Step 3: Publish to GitHub Pages with `fdroidserver`**
- [ ] **Step 4: Add the repo URL to README only after a client can add it**

### Task 7.10: In-app update link (no network permission)

**Files:**
- Modify: the About/Settings screen

- [ ] **Step 1: Add a "Check for updates" button that opens `https://github.com/xRahul/vision202020/releases/latest` with an `Intent`**
- [ ] **Step 2: Do not add `INTERNET`, an HTTP client, or a version-check API**
- [ ] **Step 3: Test that the button opens a browser and the app still has no network permission**
- [ ] **Step 4: Commit**

### Rejected or blocked

| Item | Status | Reason |
|---|---|---|
| Official F-Droid submission | Blocked until `2027-02-17` | 3-month active-development rule; prior MR closed |
| Per-ABI split APKs | Rejected | Universal APK is 2.0 MB; native libs total 73 KB |
| In-app update API | Rejected | Would require `INTERNET`; browser link is enough |
| SonarCloud | Deferred | CodeQL + lint + tests cover the baseline |
| Self-hosted F-Droid repo | Optional | Only if official latency becomes a complaint |

---

## Release Runbook (after this plan)

1. Merge the phase PRs; confirm CI, CodeQL, dependency review, and Scorecard are green.
2. Trigger **Actions → Build & Release → Run workflow → bump=patch**.
3. Confirm the workflow:
   - commits `version.properties` + Fastlane changelog;
   - pins the F-Droid recipe to the release commit SHA;
   - tags the release commit;
   - builds signed release APK;
   - asserts the signing cert;
   - writes `.sha256`;
   - attests the APK;
   - publishes only the release APK + checksum.
4. Verify:

```bash
gh release view vX.Y.Z --json assets --jq '[.assets[].name]'
gh attestation verify /tmp/opencode/v2020/release.apk -R xRahul/vision202020
```

5. Wait for F-Droid auto-update and Izzy mirroring; then update README store badges.

---

## Rollback

| Change | Rollback |
|---|---|
| Workflow edits | `git revert <commit>`; workflows are declarative |
| Repo settings | GitHub UI toggle back |
| Branch ruleset | Delete/deactivate ruleset |
| F-Droid MR | Close MR; no repo change |
| Izzy request | Comment/close issue |
| Release with bad asset | Do not delete a published tag; publish a corrected patch release |
| Signing-key assertion failure | Stop; restore the correct keystore; do not bypass |

---

## Decisions Locked

1. **F-Droid signature strategy:** Try reproducible build first with `Binaries`; submit only after the 3-month eligibility gate (`2027-02-17`) or an accepted exception.
2. **Dependency automation:** Renovate, with `helpers:pinGitHubActionDigests`.
3. **Branch protection:** Block deletion and force-push on `main`; keep direct pushes for the release bot.
4. **Backup:** Keep `android:allowBackup="true"` and disclose it in README.
5. **Extras:** Include CodeRabbit, CycloneDX SBOM, AboutLibraries, Weblate, screenshot regression tests, local crash log, cosign signatures, and the central `.github` repo. Self-hosted F-Droid repo stays optional.
6. **Accounts:** The user submits F-Droid and Izzy requests; the agent cannot authenticate on GitLab or Codeberg.
7. **Keystore backup:** User must confirm the release keystore and passwords are backed up outside GitHub Secrets.

---

## Manual Steps (only you can do these)

Do these in order after the code changes are pushed. Each step has a verification command.

### 1. Enable Dependabot alerts

GitHub → repository → **Settings → Code security and analysis → Dependabot alerts → Enable**.

Do **not** enable Dependabot security updates if Renovate is installed; it creates duplicate PRs. Alerts alone are what feed the dependency graph.

Verify:

```bash
gh api repos/xRahul/vision202020/dependabot/alerts?state=open --jq 'length'
```

Expected: a number, not `403`.

### 2. Enable private vulnerability reporting

GitHub → repository → **Settings → Code security and analysis → Private vulnerability reporting → Enable**.

Verify:

```bash
gh api repos/xRahul/vision202020/private-vulnerability-reporting --jq '.enabled'
```

Expected: `true`.

### 3. Turn on Discussions, turn off Wiki and Projects

GitHub → repository → **Settings → General → Features**:
- Enable **Discussions**.
- Disable **Wiki**.
- Disable **Projects**.

Verify:

```bash
gh api repos/xRahul/vision202020 --jq '{has_discussions,has_wiki,has_projects}'
```

Expected: `has_discussions=true`, `has_wiki=false`, `has_projects=false`.

### 4. Create the `main` branch ruleset

GitHub → repository → **Settings → Rules → Rulesets → New branch ruleset**:
- Name: `main-protection`
- Enforcement: **Active**
- Target branches: **Include default branch** (`main`)
- Enable **Restrict deletions**
- Enable **Block force pushes**
- Leave **Require a pull request before merging** off, because the release workflow pushes directly to `main`.

Verify:

```bash
gh api repos/xRahul/vision202020/rulesets --jq '.[] | {name,target,enforcement}'
```

Expected: `main-protection`, `branch`, `active`.

### 5. Install Renovate

Open `https://github.com/apps/renovate/installations/new`, select **Only select repositories → xRahul/vision202020**, install.

Then:
1. Wait for Renovate's onboarding PR.
2. Review it; it should read `renovate.json`.
3. Merge it.
4. Confirm Renovate opens grouped AndroidX/Kotlin/Compose PRs.

Verify:

```bash
gh pr list --state open --limit 10
```

### 6. Install CodeRabbit

Open `https://github.com/apps/coderabbitai`, install for **xRahul/vision202020** only.

Then open a small test PR. Confirm CodeRabbit posts a summary and line comments.

Do not add a CodeRabbit badge until the app is installed and reviewing.

### 7. Confirm the release keystore is backed up

Confirm these exist outside GitHub Secrets:
- the `.jks` / `.keystore` file;
- `KEYSTORE_PASSWORD`;
- `KEY_ALIAS`;
- `KEY_PASSWORD`;
- the certificate SHA-256 fingerprint `9ec14141874c3a8f2e3843f52582c311880fe5417eb6f097fdf8515db92f2511`.

Do not paste the keystore or passwords into chat. Verify only that the backup exists and can be restored.

### 8. Prepare the IzzyOnDroid request

Codeberg account required.

1. Check the latest release is under 30 MB and release-signed.
2. Open `https://codeberg.org/IzzyOnDroid/repodata/issues`.
3. Choose the app-request template.
4. Include:
   - app name: `Vision 20-20-20`
   - package: `in.rahulja.vision202020`
   - source: `https://github.com/xRahul/vision202020`
   - license: `GPL-3.0-or-later`
   - summary: the Fastlane `short_description.txt`
   - release: `https://github.com/xRahul/vision202020/releases/latest`
   - screenshots: the Fastlane PNG URLs
5. Answer maintainer questions. They may ask for a test APK; attach the release APK, not a debug APK.

After inclusion:

```bash
curl -sL -o /dev/null -w '%{http_code}\n' \
  "https://apt.izzysoft.de/fdroid/index/apk/in.rahulja.vision202020"
```

Expected: `200`. Add the Izzy badge only after this returns 200.

### 9. Prepare the F-Droid submission for 2027-02-17

Do not open a new MR before `2027-02-17` unless you have exception evidence:
- proven users;
- other apps you maintain on F-Droid;
- an event app.

On or after `2027-02-17`:

```bash
# Fork fdroiddata in GitLab UI, then:
git clone https://gitlab.com/<your-user>/fdroiddata.git
cd fdroiddata
git checkout -b in.rahulja.vision202020
cp /home/rahul/projects/vision202020/metadata/in.rahulja.vision202020.yml metadata/
git add metadata/in.rahulja.vision202020.yml
git commit -m "New app: Vision 20-20-20"
git push origin in.rahulja.vision202020
```

Then open a merge request against `fdroid/fdroiddata` and answer:
- AuthorName present.
- Fastlane present.
- Full 40-char commit hash.
- Only the latest version.
- Valid categories.
- `AutoUpdateMode: Version`.
- Reproducible-build answer: `Binaries` added and pipeline-tested, or removed with the failure reason.

After inclusion:

```bash
curl -sL -o /dev/null -w '%{http_code}\n' \
  "https://f-droid.org/packages/in.rahulja.vision202020/"
```

Expected: `200`. Add the F-Droid badge only after this returns 200.

### 10. Create the central `xRahul/.github` repository

GitHub → **New repository** → name `.github` → Public → create.

Add:
- `SECURITY.md`
- `CONTRIBUTING.md`
- `ISSUE_TEMPLATE/bug_report.yml`
- `ISSUE_TEMPLATE/feature_request.yml`
- `ISSUE_TEMPLATE/config.yml`
- `PULL_REQUEST_TEMPLATE.md`

Keep repo-specific overrides in each app repository. Verify GitHub shows the community files on all three repositories.

### 11. Create the Weblate project

Hosted Weblate account required.

1. Open `https://hosted.weblate.org/`.
2. Add a project named `Vision 20-20-20`.
3. Connect the GitHub repository `xRahul/vision202020`.
4. Add components:
   - Android string resources: `app/src/main/res/values/strings.xml`
   - Fastlane metadata: `fastlane/metadata/android/en-US/`
5. Do not add locales until a translator asks.
6. Add the Weblate badge only after the project is live.

### 12. Optional: Obtainium Apps index

Open `https://apps.obtainium.imranr.dev/` and submit the repository after the release asset names are stable:
- release APK: `in.rahulja.vision202020-vX.Y.Z-release.apk`
- no debug APK after the release hardening lands.

### 13. Optional: self-hosted F-Droid repo

Only if official F-Droid latency becomes a real complaint.

1. Generate a repository signing key and back it up.
2. Enable GitHub Pages (Actions source).
3. Add the `fdroidserver` workflow.
4. Add the repo URL to README only after a client can add it.

### 14. Final verification after pushing

```bash
gh run list -L 10
gh api repos/xRahul/vision202020/community/profile --jq '.health_percentage'
gh release view v1.1.8 --json assets --jq '[.assets[].name]'
gh attestation verify in.rahulja.vision202020-vX.Y.Z-release.apk -R xRahul/vision202020
```

Expected:
- CI, CodeQL, Dependency Graph, Dependency Review, and Scorecard green.
- Community health above 42%.
- Release has the APK, `.sha256`, `bom.json`, `.sigstore.json`, and `.intoto.jsonl`.
- Attestation verification succeeds.

---

## OpenSSF Scorecard Improvement Plan (2026-11-21)

Current live result: **6.3 / 10**. Use either API host; the viewer is the human-readable page.

- API: `https://api.securityscorecards.dev/projects/github.com/xRahul/vision202020` (alias: `https://api.scorecard.dev/projects/github.com/xRahul/vision202020`)
- Viewer: `https://securityscorecards.dev/viewer/?uri=github.com/xRahul/vision202020`
- Badge: `https://api.securityscorecards.dev/projects/github.com/xRahul/vision202020/badge`

### Current checks

| Check | Score | Cause | Action |
|---|---:|---|---|
| Code-Review | 0 | 0/30 approved changesets | Blocked by solo maintainer; needs a trusted second reviewer |
| CI-Tests | 10 | 1/1 merged PRs checked | Done in PR #1 |
| Packaging | -1 | No package-registry publish workflow | Not applicable to an Android APK; leave as N/A |
| Maintained | 0 | Repo under 90 days old | Time-bound; re-evaluates automatically |
| CII-Best-Practices | 0 | No OpenSSF badge | Apply for the self-certified badge |
| Signed-Releases | 2 | 1 of 5 releases; 2 signed artifacts in that release | Sign every release; 5 consecutive signed releases reach 5/5 |
| Branch-Protection | 3 | Only delete and force-push are blocked | Choose a stronger ruleset tier below |
| Fuzzing | 0 | No fuzz target or OSS-Fuzz integration | Defer; no high-value native parser to fuzz |
| Contributors | 0 | No second organization contributes | Not worth chasing for a solo project |
| Binary-Artifacts | 9 | `gradle/wrapper/gradle-wrapper.jar` is committed | Keep the wrapper; add wrapper validation to CI |
| Security-Policy | 10 | `SECURITY.md` present | None |
| Dependency-Update-Tool | 10 | `renovate.json` detected | Install the Renovate app |
| SAST | 10 | CodeQL detected | None |
| Token-Permissions | 10 | Writes are job-scoped | Keep it that way |
| Dangerous-Workflow | 10 | No unsafe patterns | None |
| Pinned-Dependencies | 10 | Actions and PyYAML pinned | Keep SHA pins |
| Vulnerabilities | 10 | 0 known vulnerable dependencies | Keep alerts on |
| License | 10 | GPL-3.0 detected | None |

### Actions in priority order

1. **CI-Tests: done in PR #1.**
   - PR #1 added this plan and passed CI, CodeQL, and Dependency Review.

2. **CII-Best-Practices: obtain the badge.**
   - Create the project at `https://bestpractices.coreinfrastructure.org/en/projects/new`.
   - Answer the criteria honestly; the app has no network, no telemetry, and a published security policy, which covers many criteria.
   - Add the badge to `README.md` only after the project page is public.
   - This is a one-time task and does not require code changes.

3. **Signed-Releases: accumulate five signed releases.**
   - The release workflow already attaches `.sha256`, `.sigstore.json`, and `.intoto.jsonl`.
   - `v1.1.8` is release 1 of 5. Releases `v1.1.9` through `v1.1.12` complete the window.
   - Optional: backfill `.sha256` and cosign signatures for older APKs and label them **post-hoc signatures, not build provenance**. Do not claim provenance that was never generated.
   - Do not re-sign old APKs with a different key.

4. **Branch-Protection: choose a tier deliberately.**
   - **Tier A (current):** block deletion and force-push only. Score stays about 3; direct pushes and the release bot keep working.
   - **Tier B (recommended if PR flow is acceptable):** require a pull request and the CI status check before merge. This improves the score, but the release workflow currently pushes the version bump to `main`; it would need a bypass actor or a PR-based bump.
   - **Tier C (maximal):** Tier B plus one approval, stale-review dismissal, linear history, signed commits, and conversation resolution. Not practical for a solo maintainer without a second reviewer.
   - Record the chosen tier in the ruleset description so future changes do not silently weaken it.

5. **Binary-Artifacts: add wrapper validation, keep the jar.**
   - Add `gradle/actions/wrapper-validation` to CI.
   - Removing `gradle-wrapper.jar` would hurt reproducibility and the F-Droid build; accept score 9.

6. **Packaging, Fuzzing, Contributors, Maintained: accept or defer.**
   - Packaging expects a package registry; an Android APK is not one. Do not add a fake package.
   - Fuzzing would need OSS-Fuzz or ClusterFuzzLite plus a meaningful target; the app has no parser or native code worth fuzzing.
   - Contributors needs a second organization; not a code task.
   - Maintained clears automatically after the 90-day window.

### Guardrails

- Do not create empty PRs, self-approve, or add a bot reviewer just to move Code-Review. That is score gaming, not a trust signal.
- Do not relax SHA pinning or job-scoped permissions for a higher score.
- Re-run Scorecard manually with `gh workflow run "OpenSSF Scorecard" --ref main` after each change and record the date, score, and changed checks.

### Expected outcome

- Realistic near-term: **7.0-7.5** after the CII badge and two more signed releases.
- After five signed releases and a stronger branch ruleset: **8+**.
- Remaining ceiling: Code-Review, Contributors, Packaging, and Fuzzing need people or infrastructure the project does not have.

---

## Success Criteria

- [ ] CI, CodeQL, dependency review, and Scorecard are green and pinned.
- [ ] Dependency graph has packages; Dependabot alerts are enabled.
- [ ] `main` blocks deletion and force-push.
- [ ] Every release has one signed APK, one `.sha256`, `bom.json`, and a verified attestation.
- [ ] Signing fingerprint is published and asserted in CI.
- [ ] F-Droid submission waits for `2027-02-17` or an accepted exception; recipe passes `fdroid lint`.
- [ ] Izzy preflight passes; request is tracked; listing returns 200.
- [ ] Fastlane has a changelog for the current version and every future release.
- [ ] README badges all return 200; no dead store badges.
- [ ] `SECURITY.md`, private reporting, issue templates, and Discussions are live.
- [ ] No new network permission, no debug APK in releases, no mutable action tags.
- [ ] CodeRabbit is installed and reviewing PRs.
- [ ] SBOM is generated and attached to releases.
- [ ] AboutLibraries, Weblate, screenshot tests, local crash log, and cosign are either implemented or explicitly tracked as follow-up.

---

## Self-Review Notes

- Spec coverage: each roadmap suggestion is a task, an explicit defer, or a rejection with a reason.
- Type/name consistency: `update_fdroid_metadata.py` takes `(version, version_code, commit_sha)`; release workflow passes the full SHA; recipe field is `commit`.
- No placeholders: version 1.1.7 / code 43 / commit `3e1576c44670e90889738b05eb9076c10a7bdd0b` are concrete; future releases substitute their own values.
- Review Focus items each map to a task: F-Droid eligibility (5.0), signature continuity (5.4), branch bot (4.3), tag rerun (3.4), version drift (3.4), fork submission (2.2), CodeQL extraction (2.1), third-party access (7.2/7.4).
- Scope: one plan, one release pipeline; the central `.github` repo is a separate task.

# eWikiReader project instructions

## Branches and delivery

- Repository: `ekwoodrich/eWikiReader`; `origin` points to that fork.
- Start work on a local `feature/<topic>` or `fix/<topic>` branch from current `dev`. Integrate completed work into `dev`. Keep work in progress off `main`.
- Use conventional commit subjects (`fix:`, `feat:`, `docs:`, `ci:`, etc.). Stable release notes filter non-merge commits by those prefixes.
- `main` is the stable release branch. Release through a PR from `dev` into `main`, with a release label; do not push routine feature changes directly to `main`.
- Preserve development history when promoting `dev` to `main`: use a merge commit, not squash/rebase. After release, merge `origin/main` back into `dev` to incorporate the bot's changelog commit; never force-push either branch.
- Commit only task changes, preserve unrelated work, and report the branch and remote status. Follow the user's request about pushing and merging; skills do not independently authorize a publication or merge.

## Actions triggers

| Action | Trigger | Result |
| --- | --- | --- |
| Android CI (`android.yml`) | Push or PR | Debug APK artifact and JVM tests; no signed release |
| Signed dev build (`release.yml`) | PR **merged into `dev`**, labeled `dev-build` before merge | Signed GitHub prerelease `dev-<short merge SHA>`, APK `eWikiReader-dev-<SHA>.apk` |
| Stable patch (`release.yml`) | PR **merged into `main`**, labeled `release:patch` or `release` | Next `vX.Y.Z` release, signed APK, committed `CHANGELOG.md` |
| Stable minor/major | Same, labeled `release:minor` / `release:major` | Corresponding version bump |
| Stable replacement | PR **merged into `main`**, labeled `release:replace` or `release:<patch|minor|major>:replace` | Rebuild the latest stable version number, replace its changelog section and GitHub Release, and upload a newly signed APK |

Opening a PR, adding a label after merge, closing it without merge, or pushing `dev` alone does not publish a signed build. Apply one intended build label before merging. Dev builds use the exact PR merge commit hash, have versionName `dev-<SHA>`, do not change `CHANGELOG.md`, and do not replace the latest stable release. Stable and dev publication share one signing/build pipeline. See [workflow details](docs/workflow-plan-v1.md).

Stable Git tags use `vX.Y.Z-release` to distinguish this fork's tags from upstream. Historical unsuffixed and `-eink` tags remain recognized for version lookup. Replacement releases keep the latest stable version number, migrate its tag to `vX.Y.Z-release`, and use the clean `vX.Y.Z` release title and APK filename. The workflow builds and verifies the signed APK before updating the changelog/tag and deleting/recreating the associated GitHub Release. Use this for corrective rebuilds; do not use it for ordinary feature releases.

## Project skills

- `.agents/skills/ewikireader-merge-dev/SKILL.md`: integrate a working branch into `dev`.
- `.agents/skills/ewikireader-release/SKILL.md`: promote `dev` to a stable signed release.
- `.agents/skills/ewikireader-dev-build/SKILL.md`: push a feature branch, open a `dev-build` PR into `dev`, and verify the resulting signed prerelease.

## Identity, signing, and device checks

- Release and signed dev builds: `io.github.ekwoodrich.ewikireader`; debug: `io.github.ekwoodrich.ewikireader.debug`. Source namespace `org.nsh07.wikireader` is intentional.
- Stable/dev APKs share the release key and installed app data; they are not separate apps. `versionCode` is the full-history commit count. Preserve ancestry so subsequent stable builds have increasing codes; avoid downgrade installs or uninstalling user data to bypass a mismatch.
- Repository secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`. Current alias is `release`. Never commit or print secret values. Local key: `/Users/eliotwoodrich/Projects/.and/ekw.jks`; CI decodes its secret into a temporary file. Release builds must not fall back to debug signing. Details: [identity/signing](docs/application-identity.md).
- Android SDK: `$HOME/Library/Android/sdk`; usable JDK: `/Applications/Android Studio.app/Contents/jbr/Contents/Home`. Use the `android-cli` skill when doing device work. Check current ADB devices instead of assuming connectivity.
- Boox Page: `10.0.0.72:5555` (TCP/IP); emulator: `emulator-5554`. Install explicitly with `adb -s SERIAL install --user 0 -r APK`; plain `adb install` previously installed into the work profile too.
- For reader behavior changes, verify relevant behavior on emulator and physical Boox. For workflow changes, validate triggers, version resolution and signing/publication behavior. Do not imply that parsing YAML proves GitHub publication works.
- Preserve scrolling mode, paged reading, stable loading, centered page controls, fullscreen reading, conditional volume controls, and the global reduced-motion setting. Icon imagery is the user's supplied open-book SVG; no added device frame.

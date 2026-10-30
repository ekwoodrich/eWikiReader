# Development and signed-build workflow

## Branch flow

Use `feature/<topic>` / `fix/<topic>` branches from `dev` for work. Integrate WIP and completed task changes into `dev`; use a `dev` → `main` PR when a stable release is ready. Preserve conventional commit subjects for generated stable release notes. Promote `dev` with merge commits and merge the bot's post-release `main` history back into `dev`; do not squash the release PR or force-push shared branches.

Project instructions are in `AGENTS.md`; discoverable project skills live in `.agents/skills/`:

- `ewikireader-merge-dev`: ordinary development integration.
- `ewikireader-release`: stable release PR and verification.
- `ewikireader-dev-build`: signed dev prerelease PR and verification.

## Triggers

`.github/workflows/android.yml` runs on pushes and PRs, builds a debug APK artifact and runs JVM tests. It does not publish a signed release.

`.github/workflows/release.yml` (`Signed Build & Publish`) handles PR `closed` events against `main` and `dev`. It only builds when the PR was **merged** and has a matching label **at merge time**:

| PR base | Label | Publication |
| --- | --- | --- |
| `main` | `release` or `release:patch` | Stable patch |
| `main` | `release:minor` | Stable minor |
| `main` | `release:major` | Stable major |
| `dev` | `dev-build` | Signed dev prerelease |

Apply exactly one intended label before merging. Opening a PR, a label added after merge, an unmerged closed PR, an unlabeled merge, or a direct push does not publish a signed build. The workflow serializes signed publishing runs and does not cancel a running publication.

## Shared build and signing

Both paths check out the exact PR merge commit with full history/tags. This prevents a later branch push from changing which source commit is built. Both run `assembleRelease` with explicit `versionName` and `versionCode`, verify the APK signature before publication, and publish the signed release-variant APK. Neither uses debug signing.

Required repository secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`. Alias is currently `release`, from the local `ekw.jks`. The workflow decodes the keystore into a restricted temporary file, supplies credentials via environment, disables Gradle configuration caching for signing, and deletes the key with `if: always()`. Never log secret values or commit private signing files.

Both APK types have package `io.github.ekwoodrich.ewikireader` and the same signing certificate; installing dev updates the same app and user data as stable. Version code is `git rev-list --count HEAD`. Preserving branch ancestry and incorporating `main` into `dev` keeps later builds increasing. Android rejects downgrades; do not delete app data as a workaround. Debug builds remain separate at `io.github.ekwoodrich.ewikireader.debug`.

## Stable release details

Stable tags, versionNames, and release names use `vX.Y.Z` / `X.Y.Z`; stable APK names are `eWikiReader-vX.Y.Z.apk`. Version selection accepts exact `vMAJOR.MINOR.PATCH` tags and the historical `vMAJOR.MINOR.PATCH-eink` tags, but ignores `dev-*` tags and other tag formats. The initial fallback baseline is `v0.99.0-eink`; the first successful fork release was `v0.99.1-eink`; the next stable release will be `v0.99.2`. Bump precedence for accidentally combined labels is major, minor, then patch; select one label intentionally.

Stable versionName is `X.Y.Z`, GitHub tag and title include `vX.Y.Z`, and APK name is `eWikiReader-vX.Y.Z.apk`. Notes collect non-merge conventional commit subjects since the last stable tag, falling back to all non-merge subjects when necessary. The workflow builds first, then prepends `CHANGELOG.md`, commits it as `chore(release): ... [skip ci]`, pushes `HEAD:main`, tags that changelog commit, and publishes a normal GitHub Release as latest. The built source is the PR merge commit; the tag additionally includes the generated changelog commit. Bot contents-write permission and permission to push `main` are required.

## Dev build details

For a feature PR merged into `dev` with `dev-build`, the workflow uses `git rev-parse --short=7 HEAD`; Git may lengthen the abbreviation for uniqueness. For example:

- Source PR merge commit: `9cfe8eb...`
- Tag and versionName: `dev-9cfe8eb`
- APK filename: `eWikiReader-dev-9cfe8eb.apk`
- Release name: `eWikiReader dev-9cfe8eb`

The tag points directly to the merge commit. Notes identify the full source SHA and changes since the last stable tag. Dev publication is a GitHub **prerelease**, explicitly not latest, and does not bump a stable version or modify `CHANGELOG.md`. The `dev-build` label belongs on the feature PR into `dev`, not the eventual stable PR into `main`.

## Verification and retries

After merge, monitor the run associated with the PR. Confirm signature verification, tag creation, asset upload, and temporary-key cleanup. Download the actual published APK and inspect signature, package/version and SHA-256 asset digest. Confirm a dev tag targets the merge SHA, prerelease status is set, latest stable is unchanged, and no changelog commit was created; for stable builds verify version bump and conventional changelog entries.

A successful YAML parse or local test-key build does not validate real GitHub secrets. If a job fails, inspect the failed step without exposing credentials. Before retrying a partially published run, check whether it already pushed a changelog commit/tag or created a release; do not force-replace published artifacts under a different source SHA. Resolve stale branch/push conflicts through normal merges.

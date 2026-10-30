---
name: ewikireader-dev-build
description: Publish and verify a signed eWikiReader development APK by pushing a feature branch and merging its dev-build-labeled pull request into dev.
---

Read root `AGENTS.md` and `docs/workflow-plan-v1.md`. A request to create a dev build includes the normal branch/PR/merge/prerelease steps; a request only to prepare a PR stops before merge.

1. Fetch and inspect local work and `origin/dev`. Work on a feature/fix branch, incorporate current `dev` as needed without rewriting shared history, and commit the intended changes using conventional subjects. Preserve unrelated work.
2. Push the working branch (`git push -u origin BRANCH`). Create/reuse a PR with **base `dev`**, the feature branch as head, and label **`dev-build`**. Ensure the label exists using `gh label list`; create it if missing as part of requested setup. Describe changes and checks via a body file. A label added after merge does not trigger publication.
3. Review the PR and relevant CI results, then merge within the user's authorization, normally with `gh pr merge --merge`. Do not use a direct push to `dev` for signed-build requests. Do not target `main` or add stable release labels.
4. Obtain `mergeCommit.oid` from `gh pr view --json mergeCommit`; the workflow checks out that exact commit. Expected tag/version is `dev-<git short SHA>` (at least seven characters; Git can lengthen for uniqueness), APK name `eWikiReader-dev-<SHA>.apk`. Monitor the `Signed Build & Publish` run for the PR.
5. Verify the release is a **prerelease**, its tag points to the PR merge commit, the APK exists and has a valid release-key signature, package ID is `io.github.ekwoodrich.ewikireader`, versionName contains the expected hash, and the asset checksum matches GitHub's digest. Stable latest-release selection and `CHANGELOG.md` must stay unchanged. Dev builds intentionally update the same app/data as stable builds; do not uninstall user data to bypass a version mismatch.
6. If the job fails, inspect the failed step without logging secrets. Before rerunning a partially published job, inspect existing tag/release state; never overwrite an existing published build under a different commit. Once successful, update local `dev` without discarding feature work and report PR/run/prerelease/download URLs and validation results. No stable release is implied.

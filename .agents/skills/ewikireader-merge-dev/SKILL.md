---
name: ewikireader-merge-dev
description: Integrate an eWikiReader feature or working branch into dev, preserving work and checking branch state. Use for ordinary development integration, without publishing a signed build unless requested.
---

Read root `AGENTS.md` for branch, device, and signing conventions.

1. Inspect working tree, current branch, remotes, and `origin/dev` after fetch. Keep unrelated changes intact. Commit the requested work on its feature/fix branch with a conventional subject. If already on `main` or `dev`, move task work to a feature branch before committing.
2. Run checks appropriate to the changes and inspect the diff against `origin/dev`. Incorporate current `dev` without rewriting shared history. Resolve conflicts before integrating.
3. When integration is requested, merge the working branch into local `dev`; push `dev` when remote integration is requested. If a PR is requested or branch protection requires it, push the feature branch and open a PR targeting `dev`, then merge within the user's authorization. Use a body file with `gh pr create --body-file`.
4. A routine integration has no `dev-build` label. If the user requests an installable signed dev build, use the sibling `ewikireader-dev-build` skill **before** merging so the label is present on the PR merge event. A direct push into `dev` cannot trigger that publication.
5. Verify local/remote commit state and clean task changes. Report what was integrated and any remaining unrelated work. Do not promote to `main` as part of ordinary dev integration.

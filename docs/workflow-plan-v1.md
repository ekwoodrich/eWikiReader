## Fork Release & Build Implementation Plan (v1)

### 1. Trigger & Scope

* **Event:** Pull request merged into `main` (`pull_request: closed` with `merged == true`).
* **Condition:** PR contains one of the following labels:
* `release` or `release:patch` $\rightarrow$ increments Patch.
* `release:minor` $\rightarrow$ increments Minor.
* `release:major` $\rightarrow$ increments Major.


* **Skip Condition:** If no release label is present, the workflow terminates immediately.

---

### 2. Version Resolution & Tag Strategy

* **Tag Format:** `vX.Y.Z-eink` (e.g., `v0.99.0-eink`).
* **Baseline Fallback:** If no prior tag matching `*eink*` exists, the workflow initializes the baseline as `v0.99.0-eink`.
* **Tag Discovery:** Query Git tags using a pattern filter (`git tag -l "v*-eink" --sort=-v:refname`) to ignore upstream tags like `v1.2.2`.
* **SemVer Logic:** Extract the base `X.Y.Z` from the latest tag, increment the target field per the applied PR label, and append `-eink`.

---

### 3. Step-by-Step Execution Sequence

#### Step 1: Full History Checkout

* Check out the repository using `actions/checkout@v4` with `fetch-depth: 0` to ensure complete commit history and tags are available for version calculations and changelog parsing.

#### Step 2: Version & Changelog Resolution

* Parse the label on the merged PR to determine the increment type.
* Calculate the new version string (`vX.Y.Z-eink`).
* Generate changelog entries by inspecting commit messages between the previous `*eink*` tag and `HEAD` (`git log <last-tag>..HEAD --pretty=format:"%s"`), filtering for conventional patterns (`feat:`, `fix:`, `chore:`, etc.).

#### Step 3: Compile & Package the APK (Build First)

* Run the Gradle build task (e.g., `./gradlew assembleRelease`).
* Supply Android properties dynamically to keep `versionCode` monotonically increasing:
* `versionName` $\rightarrow$ `X.Y.Z-eink`
* `versionCode` $\rightarrow$ Git commit count (`git rev-list --count HEAD`)


* Rename the output APK to `<AppName>-v<X.Y.Z>-eink.apk`.
* *Fail-Safe:* Building before tagging prevents polluting the repository with new tags or commits if compilation fails.

#### Step 4: Update Repository `CHANGELOG.md`

* Prepend the newly generated release notes and current date to the top of the root `CHANGELOG.md`.
* Leave the upstream metadata directories (e.g., `metadata/en-US/changelogs/`) untouched to prevent future merge conflicts when syncing upstream.
* Commit the modified `CHANGELOG.md` directly to `main` using `[skip ci]` in the commit message to prevent recursive workflow triggers:
```bash
git commit -m "chore(release): update CHANGELOG.md for vX.Y.Z-eink [skip ci]"
git push origin main

```



#### Step 5: Tag & Publish

* Tag the newly pushed commit containing the updated `CHANGELOG.md`:
```bash
git tag -a "vX.Y.Z-eink" -m "Release vX.Y.Z-eink"
git push origin "vX.Y.Z-eink"

```


* Create a GitHub Release using `softprops/action-gh-release@v2`:
* **Target Tag:** `vX.Y.Z-eink`
* **Release Body:** The filtered conventional commit notes.
* **Assets:** The renamed `.apk` binary.



---

### 4. Critical v1 Setup Requirements

1. **Workflow Permissions:** The workflow job must have `contents: write` and `pull-requests: read` declared.
2. **Branch Protection:** Ensure the GitHub Actions bot has permission to push commits directly to `main` (either bypass branch protections for workflows or permit the CI bot).

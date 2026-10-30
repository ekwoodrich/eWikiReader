# Application identity and signing

The fork uses these Android application IDs:

| Build | Application ID |
| --- | --- |
| Release | `io.github.ekwoodrich.ewikireader` |
| Debug | `io.github.ekwoodrich.ewikireader.debug` |
| Debug instrumentation | `io.github.ekwoodrich.ewikireader.debug.test` |

Upstream WikiReader uses `org.nsh07.wikireader`. Earlier fork builds used `org.nsh07.wikireader.eink.debug` for development and, subsequently, `org.nsh07.wikireader.eink` for release. Existing installs of those IDs are separate apps: their saved data does not migrate automatically, and the new ID can coexist with them. Do not uninstall a previous copy before preserving any data you need.

The Kotlin source namespace remains `org.nsh07.wikireader`, including the activity and application classes. This is independent of the installed app ID and does not cause signing or installation collisions. Android's build tooling derives manifest identity, instrumentation identity, and AndroidX provider authorities from the application ID. The baseline-profile module already derives its target ID from the built APK.

A different application ID establishes a separate app identity; it does not permit changing the signing key for updates of that identity. Keep the release ID and signing key stable after publishing. An error about a different signing key can also mean an earlier eWikiReader APK was signed using a different certificate, rather than a conflict with upstream.

Release signing now accepts `ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD` from the environment. All four must be supplied together. Without them, command-line release builds are unsigned; there is no debug-signing fallback. Android Studio's Generate Signed Bundle / APK wizard can supply its own signing configuration.

The current local release keystore is `/Users/eliotwoodrich/Projects/.and/ekw.jks`, alias `release`. It replaces the earlier `ewr_keys.jks` / `key0`. Use the new file and alias when signing in Android Studio; no absolute local key path or password is embedded in Gradle. Keystores and generated Android Studio release output are ignored by Git.

GitHub's release workflow reads the repository secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` (set to `release`), and `ANDROID_KEY_PASSWORD`. The base64 secret must contain the new `ekw.jks` file. It validates that all secrets exist, decodes the keystore to a restricted temporary file, passes signing credentials only through the build environment, disables Gradle configuration caching for the signing build, verifies the resulting APK signature, and deletes the temporary keystore even on failure. Debug CI builds remain independently debug-signed. Do not reuse a debug APK as a release update.

Keep a secure backup of the new key. If an APK has already been distributed with the old signing key, a new certificate is not an automatic update replacement; device installs and Google Play's signing configuration must be handled according to their key-rotation rules.

For local generated signed APKs, select the release variant and the intended eWikiReader keystore in Android Studio. Confirm the final APK's application ID and signing certificate before distributing it. An unsigned APK cannot be installed. Do not use upstream WikiReader's published certificate fingerprints to verify this fork.

Official references: [application ID versus namespace](https://developer.android.com/build/configure-app-module), [Android app signing and update identity](https://developer.android.com/studio/publish/app-signing).

Validation for this change: `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, and `:app:processReleaseMainManifest` passed. Both merged manifests use the new application IDs, fully qualified source application/activity classes, and provider authorities derived from the new IDs. The emulator accepted the new debug and instrumentation APKs using `--user 0`; the app-context identity test and live full-screen article/page-navigation test both passed (2 tests). No production-signed release was built or published as part of this identity change.

Release-signing validation: a disposable keystore using alias `release` successfully built the release and debug APKs; `apksigner verify` passed and the release APK's SHA-256 signing-certificate digest matched the test keystore's exported certificate. The disposable key and its generated release APK were removed after validation, preserving any previous build output. With signing environment variables absent, Gradle's signing report confirms `release` has no signing config while debug retains its debug config. A partially supplied signing environment fails with the required-variable message. Workflow YAML and all embedded shell scripts passed parsing/syntax checks. All four expected repository secret names were confirmed through GitHub's secret-name API; their values are not readable through that API and were not validated with the real key. A real signed GitHub release remains to be exercised by the next release-labeled merged PR.

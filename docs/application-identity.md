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

The current Gradle release type still explicitly selects the debug signing configuration, and the GitHub release workflow does not yet load a private release keystore. This ID change does not configure production signing. Before publishing production APKs, configure both local and CI release signing to use the same release key and remove that debug-signing fallback. The locally generated keystore at `/Users/eliotwoodrich/Projects/.and/ewr_keys.jks` is outside this repository and was not modified or committed. Keystore passwords and key aliases are not inferred or exposed by this change. GitHub builds cannot access that local path; they need separately configured signing secrets.

For local generated signed APKs, select the release variant and the intended eWikiReader keystore in Android Studio. Confirm the final APK's application ID and signing certificate before distributing it. An unsigned APK cannot be installed. Do not use upstream WikiReader's published certificate fingerprints to verify this fork.

Official references: [application ID versus namespace](https://developer.android.com/build/configure-app-module), [Android app signing and update identity](https://developer.android.com/studio/publish/app-signing).

Validation for this change: `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, and `:app:processReleaseMainManifest` passed. Both merged manifests use the new application IDs, fully qualified source application/activity classes, and provider authorities derived from the new IDs. The emulator accepted the new debug and instrumentation APKs using `--user 0`; the app-context identity test and live full-screen article/page-navigation test both passed (2 tests). No production-signed release was built or published as part of this identity change.

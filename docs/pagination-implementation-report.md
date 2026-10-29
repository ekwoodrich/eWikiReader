# eWikiReader paged reading implementation and verification

Implementation date: 2026-10-29. Branch: `feature/paged-reading-loading-fix`.

## Delivered behavior

Settings → **Paged reading** switches article reading between the upstream scrolling presentation and a native Compose paged presentation. Default is scrolling; the choice persists in the existing Room boolean-preference table and Reset Settings restores scrolling. There is no database migration or new web renderer. The launcher name, About name, localized app labels, project name and store title metadata use **eWikiReader**. The source link points to the fork; the license and upstream contribution history remain intact. The debug application ID remains `org.nsh07.wikireader.eink.debug`, alongside the original installed app, so physical-device testing has not cleared or overwritten production app data.

Paged articles have stationary back/forward arrow controls with accessible Previous/Next labels, a centered `current / total` counter, disabled first/last controls, rich selectable text and links, fixed-size images, captions and fully expanded sections. Page Up/Down and volume Up/Down map to previous/next on key release. Long key holds do not repeatedly advance. Keys are scoped to reader focus and disabled for reference/language sheets and the text selection toolbar. Normal Back navigation, search, save, language switching, image viewing and section navigation remain available.

Page turns do not scroll, slide, fade or animate. With chrome visible, the reader reserves content space above the existing bottom action toolbar and respects actual Scaffold/system-bar padding. The expand icon beside the back arrow enters full-screen reading: app search/navigation/toolbars and system bars disappear, and the reserved toolbar space is reclaimed. Only a 48 dp control row remains. Its collapse icon or Back restores chrome; Back from a citation sheet closes the sheet first. The numeric counter is centered independently of button widths; previous/next icons sit symmetrically at the outer edges and follow RTL direction. Full-screen state belongs to the article visit and survives linked-image/back navigation; it is not a global saved preference. Font size/style, font scale, window width/height, language direction, navigation-rail expansion and content preferences affect measurement and page count. The same saved article content goes through the same page pipeline as online articles.

## Implementation decisions

- `reader/ArticleBlocks.kt` adapts the existing parsed article into ordered title, description, image, prose, heading and structured-content blocks. Existing rich text is retained; links and citation listeners are not flattened into strings. Online/saved loading share this adapter.
- Tables become labeled rows and numbered columns in reading order rather than a horizontally scrolling grid. Infoboxes become labeled fields, including their media/captions. This intentionally adjusts the original plan's grid/row continuation proposal: the same text paginator handles oversized cells and fields, and every parsed cell remains reachable without vertical scrolling inside a page. The existing parser's limits for complex nested tables/rowspan/colspan remain; this feature does not introduce a full MediaWiki renderer.
- Galleries become sequential image/caption blocks. Images occupy fixed, bounded slots, so late loads and failures do not change page boundaries. Tapping a paged image opens the existing full-screen image screen. Data saver retains structural image blocks as hidden entries so text anchor indices do not shift just because images are hidden.
- Display math uses a bounded equation image when rendering is enabled; otherwise it uses the existing LaTeX-to-Unicode conversion with raw-LaTeX fallback. A previously empty citation callback before display math was wired to the real reference handler.
- `reader/ArticlePagination.kt` measures annotated text using the same Compose styles/width as rendering, splits at complete line boundaries, rechecks the actual substring after shaping, and records contiguous UTF-16 source ranges. Consumed terminal newlines remain in source ranges without adding an empty visual line at the page foot. Shaping batches are bounded to roughly 4,000 characters, prefer whitespace/grapheme boundaries, yield between batches and cancel when the specification changes. TextMeasurer caches are bounded; the reader composes only the current page, not an entire offscreen article.
- An exceptional line taller than the entire available viewport gets a bounded, scaled rendering on its own page rather than an infinite pagination loop or clipped text. Normal pages retain the requested text size. This matters for very small windows or extreme font scale.
- `reader/PagedArticleContent.kt` supplies the viewport, controls, static images, source-anchor restoration and input guards. Each back-stack article owns an `ArticleReaderState`; current page numbers are derived from source block/offset anchors after reflow. Image loading does not trigger repagination. Stale pagination coroutines are canceled when inputs change.
- Settings toggles bridge the existing scrolling item's index/offset and the paged source location. Switching modes without turning preserves the original scroll snapshot. After page navigation, returning to scrolling restores the containing section and an estimated text offset and expands that section. The estimate is approximate for reflowed tables, infoboxes and media because the scrolling renderer has different/collapsed geometry. Exact source anchors are preserved within paged reading, font/window reflow and linked-article/image Back navigation. This is not a new cross-session article-resume feature; a fresh task starts according to existing app behavior.
- Article navigation uses a stable, unique visit ID as the Navigation3 content key. Using the entire changing article object/string as a saved-state key caused a duplicate-key crash under save/load/navigation testing. Save-status copies retain the visit/reader state; a separately opened copy of the same article gets a new visit ID.
- Canceling a network load no longer presents a canceled feed request as a feed error or appends an obsolete error article. A real article fetch error clears the loading indicator. This is separate from the original bitmap crash.
- Image requests in feed, hero, full-screen image and equation paths are constrained to the display, capped at 2048 pixels per dimension and use exact FIT decoding. The shared loader also caps bitmap dimensions. The prior original-size requests could draw ~121 MB bitmaps and crash Android RecordingCanvas. Original-size zoom detail beyond the cap remains a deliberate tradeoff; tiled zoom would be a separate feature.
- New English UI strings deliberately fall back to English pending translation; MissingTranslation is scoped only on these new strings. Existing unrelated lint errors are not globally suppressed.

## Reproducible checks

Use the installed Android SDK and Studio JBR:

```sh
export ANDROID_HOME=/Users/eliotwoodrich/Library/Android/sdk
export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest
./gradlew :app:lintDebug
```

Run one instrumentation process per device, without a concurrent `android layout` instrumentation server. The Android CLI inspection helper can be stopped before tests:

```sh
adb -s SERIAL shell am force-stop com.android.cli.interact.instrumentation
adb -s SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
adb -s SERIAL install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s SERIAL shell am instrument -w -r \
  -e notClass org.nsh07.wikireader.OfflineSavedJourneyTest \
  org.nsh07.wikireader.eink.debug.test/androidx.test.runner.AndroidJUnitRunner
```

The separate offline test must run with host-controlled Wi-Fi/mobile data disabled, then both radio states restored to their prior values:

```sh
adb -s SERIAL shell svc wifi disable
adb -s SERIAL shell svc data disable
adb -s SERIAL shell am instrument -w -r -e offline true \
  -e class org.nsh07.wikireader.OfflineSavedJourneyTest \
  org.nsh07.wikireader.eink.debug.test/androidx.test.runner.AndroidJUnitRunner
# Restore radios to their recorded prior states after the test, including on failure.
```

Offline testing is separate because obtaining another UiAutomation connection inside the Compose test conflicted with Android's registered automation service on API 30. That failed experiment crashed the *test harness* at instrumentation shutdown; it was removed. Host-side ADB avoids that conflict. Live tests depend on Wikipedia/network availability; deterministic pagination and huge-image tests do not.

## Verification coverage

- `PaginationTest`: source coverage with no gaps/duplicate ranges; actual measured fragment/page fit; multiple widths/font sizes; emoji, Arabic, Hebrew, Japanese, bold spans and explicit newlines; full forward/reverse traversal, boundaries, section jump, font reflow and key navigation. Fixture turn timings include test synchronization and are upper bounds, not raw rendering latency.
- `ArticleJourneyTest`: real API/raw-wikitext fetch of E Ink, paging, save without losing the anchor, full forward traversal of Electronic paper, Back restoration, font change, scrolling/paged toggle and reopening saved content.
- `LanguageAndMediaJourneyTest`: live English Pythagorean theorem (math), Japanese 電子ペーパー, Arabic ويكيبيديا, English E Ink; paging each, activity recreation, image viewer/Back and a real citation listener. Sending Page Down while the reference sheet is open must preserve the underlying article's anchor.
- `FullscreenReaderTest`: larger reading area, hidden search/share controls, retained source anchor, centered numeric progress, symmetric arrow positions, normal/full-screen paging, full-screen exit toggle, Back behavior and reference-sheet Back behavior.
- `SettingsPersistenceTest`: persisted on/off setting, activity recreation, Reset defaults and restoring paged mode for further inspection.
- `ImageLoadingTest`: a deterministic 6000 × 6000 RGB fixture successfully decodes within display bounds rather than allocating its original ~144 MB ARGB size.
- `OfflineSavedJourneyTest`: the saved E Ink article loads and pages with device radios disabled.
- Manual ADB input/hierarchy/screenshot checks: actual Settings switch, live article pagination, inline Electronic paper link and Back, section drawer jump, image viewer/Back, Page Down and volume Down on the Boox. Screenshot pixels were inspected. Boox's screenshot prefix workaround remains documented in the plan.

## Device-specific limits of evidence

The emulator is the existing API-30 arm64 `Onyx_Boox_Page`, 1264 × 1680, with a runtime density override to 300 dpi. The physical device is Page / Android 11 / API 30, serial `6C7B6863`, reported physical 1264 × 1680 at 300 dpi. Boox app optimizations/font settings can produce a different content viewport and therefore a different page count; the reader measures the actual viewport.

ADB key injection exercises real device-side input routing, but does not itself prove mechanical button actuation. The physical device reports its `gpio-keys` input device with Volume Up/Down. Framebuffer captures do not measure e-ink ghosting/waveform behavior. Physical panel observations and literal cable-unplug testing require a person at the device; neither should be claimed solely from screenshots or wireless ADB while the USB cable remains connected.

## Final results

Acceptance checks completed on both devices. The results below distinguish software verification from observations that require a person at the physical panel.

Verified implementation commit: `a9d308b` (with earlier image-loading fix `ea71940`).

| Check | Emulator | Physical Boox Page |
| --- | --- | --- |
| Final online instrumentation suite | **9 passed**, 26.579 s | **9 passed**, 53.325 s |
| Final separate offline saved-article journey | **1 passed**, 2.325 s; active default network was `none` | **1 passed**, 5.952 s; Wi-Fi confirmed disabled (this device has no phone service) |
| Source coverage, typography/viewport fit and complete traversal | Passed | Passed |
| Live E Ink / Electronic paper / Pythagorean theorem / Japanese / Arabic | Passed | Passed |
| Save, second article, Back, font change, mode switch, saved load | Passed | Passed |
| Media viewer, references, reference key guard and recreation | Passed | Passed |
| Full-screen growth, hidden chrome, exact counter centering and symmetric icons | Passed | Passed |
| Reference Back retains full screen; reader Back exits full screen | Passed | Passed |
| 6000 × 6000 bitmap fixture | Passed | Passed |
| Rotation/reflow manual check | Landscape paged navigation passed, restored portrait/sensor settings | Portrait passed; requested system landscape rotation was overridden by the device/app display policy and the app remained portrait. Original sensor/rotation settings restored. |

The final standard suites are preserved in [emulator-tests.txt](verification/emulator-tests.txt) and [boox-tests.txt](verification/boox-tests.txt); separate offline results are in [emulator-offline-test.txt](verification/emulator-offline-test.txt) and [boox-offline-test.txt](verification/boox-offline-test.txt). No tests failed or were skipped in these final runs. Earlier failures were used to fix the newline boundary, Navigation3 key, modal key, and test-harness issues described above.

All **14 existing JVM unit tests passed** with zero failures/errors/skips. APK and test APK assembly passed. `git diff --check` passed. Lint still fails with the prior **168 errors and 43 warnings** (including existing missing translations and Compose resource-call findings); there are no findings in the new reader or full-screen test files. No blanket lint baseline/suppression was added. The final lint report remains at `app/build/reports/lint-results-debug.html`.

The final instrumented complete Electronic paper traversal covered 36 pages on the emulator and 45 on the Boox under each device's current layout. The navigation fixture measured median/p95 of **104/140 ms** on the emulator and **114/133 ms** on the Boox, including test synchronization. These measurements exclude physical e-ink panel refresh and are not a waveform/ghosting benchmark. No new runtime crashes appeared in either device's crash buffer during final acceptance runs.

Offline testing restored prior Wi-Fi/mobile-data states. Android turns wireless debugging off when Wi-Fi is disabled; after Wi-Fi reconnected, wireless debugging was explicitly restored, the already paired Boox TLS transport reappeared, and a wireless shell/model query and page-input/hierarchy check succeeded. USB remained physically connected, so literal cable-unplug continuity and mechanical button/physical-panel observations remain unclaimed. The ordinary production WikiReader package and its storage were left intact. The development app is left available as **eWikiReader**, with paged reading enabled and the Page showing the full-screen reader.

Final APK: `app/build/outputs/apk/debug/app-debug.apk` (local debug signing, application ID `org.nsh07.wikireader.eink.debug`). SHA-256:

```text
2ab28f8f8fdd62b9d138d500f080cbd4f3bc24257bf166d28dc83c5cb0288025
```

### Visually checked final screenshots

- [Boox full screen](verification/boox-fullscreen.png): app/system chrome hidden; only the compact navigation row remains.
- [Boox normal controls](verification/boox-controls.png): independent centered counter and symmetric previous/next icons.
- [Emulator full screen](verification/emulator-fullscreen.png).
- [Emulator normal controls](verification/emulator-controls.png).

Screenshots show the [Wikipedia E Ink article](https://en.wikipedia.org/wiki/E_Ink). Article/diagram attribution and media-license details are available from that article and its linked file-description pages. Screenshots are unmodified framebuffer captures except for removal of the Boox capture command's non-PNG byte prefix.

# Paged reading for WikiReader: requirements and implementation handoff

Prepared 2026-10-29 against commit `fc5c017` (WikiReader 2.5.5). This document plans implementation; pagination and the stability fix have not yet been implemented. Device readiness was exercised separately from feature acceptance testing.

## Intended result

Add a persistent **Paged reading** switch in Settings. Off preserves the current scrolling article reader; on presents complete, stationary reading pages with explicit Previous/Next controls and instantaneous page replacement. Optimize for a 7-inch Onyx Boox Page running Android 11, primarily portrait at 1264 × 1680. Reuse this repository as the Android Studio project.

Also fix the existing oversized-bitmap crash and prevent a crash/relaunch loop without requiring storage clearing. The user reports that WikiReader worked previously, crashed opening an article, and worked again after clearing storage before this task. Reproducing the precise article/image trigger remains necessary; do not infer that the app is generally incompatible with the device.

## Requirements and acceptance criteria

| ID | Requirement | Acceptance evidence |
| --- | --- | --- |
| R1 | Paged reading defaults off, persists across restart, resets to off with Reset Settings. Existing installations require no destructive migration. | Settings tests and cold launch on both devices. |
| R2 | Article pages fit the actual available content rectangle. No clipped text lines, lost content, duplicate content, blank trailing pages, or controls covering text. | Deterministic boundary tests and complete forward/back traversal on both devices. |
| R3 | Previous/Next buttons have at least 48 dp targets, accessible labels, and disabled first/last states. Turns replace the page without scroll, slide, fade, crossfade, or curl animations. | Screen recordings and UI assertions; physical panel inspection. |
| R4 | Users can reach all article sections, links, citations, images, equations, galleries, and table content. Paged mode reads sections continuously expanded; explain that the Expand sections preference applies to scrolling mode. | Content matrix below; section jumps and return journeys. |
| R5 | Preserve the logical reading location when changing modes, typography, orientation, window size, data-saver state, or returning from a linked article/image/Settings. | Anchor round-trip tests and on-device journeys. |
| R6 | Feed, search results, history, saved-article lists, and settings keep their current navigation behavior. Saved articles use the same article pagination pipeline as online articles. | Regression matrix on both devices. |
| R7 | Boox side buttons turn one page when delivered as supported page/volume keys. Do not intercept keys while searching, typing, selecting text, or displaying another screen/dialog. | Synthetic input plus actual physical button presses; mapping recorded. |
| R8 | Late image loads do not move text between pages. Memory stays bounded on long articles and image-heavy content; oversized images do not crash launch or article reading. | Fixed-size placeholders, large-image fixtures, profiling and relaunch tests. |
| R9 | Full verification includes development iterations on the configured emulator and final acceptance on the physical Page. | Separate results for each device, APK hash, commit, and remaining failures. |

Position means a semantic source location, not a page number. Page numbers change with font and viewport. Preserve during navigation/recreation; use saved instance state for process recreation where Android restores the task. Full cross-session article resume after a fresh launch is a separate product extension unless needed to fix the observed startup loop.

## Verified environment and current gaps

| Item | Observed state |
| --- | --- |
| Host | macOS arm64; project `/Users/eliotwoodrich/Projects/eWikiReader`. |
| Android CLI | `1.1.16717390`; installed in SDK cmdline-tools; layout instrumentation installed successfully on both devices. |
| Android Studio | Rabbit 2, 2026.2.2. Opened this repository; `android studio check` subsequently reports `READY`. This indicates CLI/project availability, not proof of a successful Gradle sync. |
| IDE error reported by user | Desktop Compose/Skiko coroutine exception. Matching log entries include `AbstractClickableNode`, `ConfigurationPageKt`, and `DispatchedContinuation.takeState` assertion failures in AVD UI at approximately 00:10, before this project was opened at approximately 14:05. Separate from Android runtime bitmap crash. |
| Java | Default JDK 22; Studio bundled JBR 25.0.4. Installed list also includes JDK 11 and 8; JDK 17 was not listed. Baseline command uses Studio JBR explicitly. |
| SDK | `/Users/eliotwoodrich/Library/Android/sdk`; initially platforms 30 and 37.0, build-tools 36.0.0. Baseline build automatically installed platform 36 revision 2 and build-tools 35.0.0 using existing accepted licenses. |
| ADB / scrcpy | ADB 1.0.41 / 36.0.0, scrcpy 5.0.1. Keep scrcpy; no replacement is required for this workflow. User already verified its mirroring command. |
| Emulator | Existing `Onyx_Boox_Page`, API 30 Google APIs arm64, portrait 1264 × 1680, 3 GB RAM, four cores. Started headless successfully as `emulator-5554`; boot complete, HOME input, layout inspection, and screenshot verified. |
| Density | AVD config is 320 dpi; physical reports 300 dpi. Applied reversible runtime override `adb -s emulator-5554 shell wm density 300` for closer layout parity. Config file remains 320; verify override after restart, or set AVD config to 300 while stopped in future. `wm density reset` restores original. |
| Physical USB | Serial `6C7B6863`, model Page, Android 11 / API 30, physical 1264 × 1680, density 300, stay-awake value 7. Shell, HOME/BACK input, layout, screenshot, and existing-app launch exercised. |
| Physical wireless | `adb-6C7B6863-hzXbqu._adb-tls-connect._tcp` answered model query independently. Same device as USB. Unplugged continuity has not been exercised; endpoint/port may change. |
| Existing installed app | `org.nsh07.wikireader`, versionName 2.5.5 / versionCode 53. Launch during readiness crashed with `Canvas: trying to draw too large(121152000bytes) bitmap`, via Coil/Compose BitmapPainter. Existing installation was not overwritten or cleared. |

Only target the Boox serials or the selected emulator. Other mDNS-discovered devices are unrelated. Always pass a serial: USB and wireless entries refer to the same physical device, and unqualified commands are ambiguous.

The emulator emulates Android/layout/input, not e-ink waveforms, ghosting, actual side-button firmware mappings, or Boox app optimizations. Those need physical testing and a person observing the panel. ADB screenshots show framebuffer content rather than panel ghosting.

## Codebase findings

Paths below are relative to `app/src/main/java/org/nsh07/wikireader/` unless otherwise noted.

| Code | Current behavior and planned integration |
| --- | --- |
| `ui/homeScreen/PageContent.kt` | Native Compose `LazyColumn`, title/image item, intro item, alternate section heading/body items, 156 dp bottom spacer; wrapped in pull-to-refresh. Choose scrolling vs paged renderer here; avoid treating each LazyColumn item as a page. Sections may span many screens. |
| `ui/homeScreen/ParsedBodyText.kt` | Iterates `AnnotatedString` blocks and dispatches text, file images, galleries, math, tables, and infoboxes. Extract shared block classification and text style construction so measurement/rendering agree. |
| `ui/homeScreen/ExpandableSection.kt` | Local `rememberSaveable` expansion and AnimatedVisibility. Paged renderer uses expanded content with static headings. Preserve scrolling behavior. |
| `parser/wikitextToAnnotatedString.kt` | Generates rich annotations including `LinkAnnotation.Url` and listeners. Preserve annotations when splitting text; never paginate flattened strings. Avoid changing parsing semantics for this feature. |
| `ui/homeScreen/AsyncWikitable.kt`, `AsyncInfobox.kt` | Asynchronous structured renderers; table has horizontal scroll and custom row measurement. Need row/cell data and heights available for paged layout. Large table handling cannot simply reuse one unlimited-height composable. |
| `ui/homeScreen/ImageWithCaption.kt`, `Gallery.kt`, `EquationImage.kt`, `ui/image/*` | Async media and multiple animation sites; equations can horizontally scroll, galleries animate. Introduce constrained paged variants and a motion flag where shared. Audit decode sizes on ALL image entry points for stability. |
| `ui/homeScreen/viewModel/HomeScreenState.kt` | `HomeSubscreen.Article` owns `extract`, section index/title pairs, and `LazyListState`. Add logical article identity and reader anchor/state without using LazyList indices as page indices. |
| `ui/homeScreen/viewModel/HomeScreenViewModel.kt` | Parses online and saved articles separately, constructs section indices using current LazyColumn structure, controls top navigation, reload and backstack. Both load paths must use the same block/section adapter. |
| `ui/AppNavigationDrawer.kt` | Article section clicks directly call `listState.scrollToItem(section.first)` and selection derives from visible list indices. Route article section navigation through a reader action and mode-aware current section instead. Leave feed path as-is. |
| `ui/homeScreen/AppHomeScreen.kt` | Shared transitions, progress animations, bottom floating toolbar and scroll-to-top. Reserve space for controls, provide static article motion in paged mode, route Top through mode-aware action. |
| `ui/AppScreen.kt` | Scaffold insets, search-bar and toolbar nested-scroll/immersive behavior, NavHost transitions and navigation rail. In paged articles keep stable chrome dimensions; no auto-hide dependent on scrolling. Check expanded rail at this device's dp width. |
| `ui/settingsScreen/viewModel/{PreferencesState,SettingsAction,SettingsViewModel}.kt` | Add `pagedReading: Boolean = false`, `SavePagedReading`, load/save boolean key `paged-reading`, and reset default. Follow existing state flow conventions. |
| `ui/settingsScreen/SettingsScreen.kt` | Switch-item list provides existing settings pattern. Add label/description and explanatory interaction with immersive/expanded-sections settings. |
| `data/{PreferencesRepository,PreferenceDao,AppDatabase}.kt` | Room key/value preferences; boolean table already exists. New boolean key needs no Room schema version change. |
| `app/src/main/res/values/strings.xml` | Add setting label/help, Previous/Next, progress, start/end, media/table controls and accessibility descriptions. English fallback for untranslated locales; do not fabricate translations. |
| `app/src/test`, `app/src/androidTest` | Existing unit test file and a sample context instrumentation test; Compose test dependencies already present. Add meaningful pagination/state tests and end-to-end reader journeys. |

## Architecture decision

Implement a native Compose block paginator with measured text fragments. A WebView/CSS-column conversion would duplicate the existing parser/rendering, change rich content and selection behavior, and add a second browser-based rendering system. A fixed `scrollBy(viewportHeight)` button is insufficient for the requested ebook-style experience: it cuts lines, changes with async media, and lacks reversible semantic boundaries.

Proposed new package: `ui/homeScreen/reader/` with `ArticleBlock`, `ReadingAnchor`, `PaginationSpec`, `PageFragment`, `ArticlePage`, `ArticlePaginator`, `ArticleReaderState`, `PagedArticleContent`, `PageTurnControls`, and `ReaderKeyHandler`. Names are suggestions; keep responsibilities distinct and testable.

### Content and position model

1. Build ordered blocks from title/description/media, intro, then every heading and body. Use stable IDs based on article language/page ID, section ordinal and source block ordinal, with fallback title identity for error/search-result articles without page IDs. Do not use `preferencesState.lang` in place of the article's actual `currentLang`.
2. Text fragments retain original `AnnotatedString` source ranges `[start, end)`. Anchor includes article identity, block ID and UTF-16 offset (or table row/cell fragment identity). Page start anchors form the pagination index.
3. Each backstack article owns its anchor so links and Back restore the previous article. Keep saveable minimal IDs/ranges, not rendered pages, bitmap data, or callback-bearing AnnotatedStrings in a Bundle. Rebuild pages after recreation.
4. Scrolling-to-paged conversion must translate visible item/local text coordinates to the nearest visible source line. Existing LazyListState only identifies large intro/section items; add block position reporting/measurement in the scrolling renderer. Reverse conversion expands the containing section if needed, then scrolls to its measured block/line offset. Acceptance tolerance: the previously visible source line remains on screen; no reset to article start.
5. Repagination keeps the source anchor visible, never the old page number. Cache key includes article content revision, actual width/height, density, font scale/family/size/line height, layout direction, and content-affecting preferences. Theme changes must update draw state; invalidate measurement if any text metrics change.

### Page layout algorithm

1. Measure the remaining content area after system bars, Scaffold padding, navigation rail, fixed article toolbar and page controls. Use actual bounded constraints in pixels, not 1264/1680 constants. Remove scrolling reader's 156 dp spacer from page content.
2. Measure text using Compose `TextMeasurer` with the EXACT renderer text style, width, density, font resolver, direction, paragraph rules, and padding. Pack full blocks where possible; split long text only at complete line boundaries from `TextLayoutResult`. Preserve explicit newlines, spaces, links and spans through `AnnotatedString.subSequence`; require source-range coverage tests.
3. Re-measure the candidate fragment because substring shaping, paragraph styles, hyphenation and bidirectional text can change its height. Back off to an earlier valid boundary until it fits. Do not split surrogate pairs or grapheme clusters. Avoid orphan headings where possible; correctness and forward progress take priority over balanced whitespace.
4. Every iteration consumes source content or emits an explicitly supported oversized-block fragment. Handle empty input, tiny/temporarily zero viewport, a line taller than the viewport, and very long unbroken tokens without infinite loops. Defer until valid constraints, and provide a readable constrained fallback for extreme font/viewport cases.
5. Images get known bounded slots with aspect-ratio-aware fit and bounded decode requests. Placeholders/errors occupy the same size; late loading does not repaginate text. Captions may split as text. Gallery becomes static paged thumbnails/items; full-screen image viewer remains accessible.
6. Tables/infoboxes need structured row pagination with repeated headers and explicit continuation. Split a single oversized row into measured cell-text continuations rather than losing content. Permit deliberate horizontal table panning with a visible affordance; avoid intercepting it for article turns. Equations fit where legible, otherwise retain explicit horizontal pan. Full-screen media/detail views may scroll, but ordinary article reading must not require continuous vertical scrolling. Implementing these adapters is part of completion, not an undocumented fallback that drops complex content.
7. Compute/retain page boundaries and lightweight text fragments, not whole-article composed screens or bitmaps. Render current page only (optionally prepare neighbors). Chunk large measurements across frames; cancel stale work when content/spec changes and reject obsolete results. Bound caches and avoid a huge offscreen Column for measurement. During recompute keep the old readable page or one static loading state; avoid animated progress redraws.

### Navigation and e-ink behavior

Previous/Next buttons are the reliable primary input. Optional short swipes commit one turn on release with no live drag/transition. Avoid a blanket tap overlay: it would break links, long-press selection, tables and accessibility. Display “Page X of Y” when count is known; no fabricated total while building the index. Expose accessible actions/progress without continuous announcements.

Use a unified `ReaderAction` for Next, Previous, GoToSection, GoToStart and RestoreAnchor. Disable turn actions at boundaries and serialize rapid input to prevent double turns. Page keys and volume mappings must be scoped to the active article and excluded during modal/search/selection interactions. Begin with PAGE_UP/PAGE_DOWN; verify actual Boox side-key mapping before consuming volume keys. Handle key-down/up/repeat deliberately so one press produces one turn. Back retains normal Android navigation.

Keep stable top/bottom reader chrome in paged mode irrespective of the immersive scrolling preference; retain that preference for scrolling. Suppress article shared transitions, content-size animations, gallery animations and Coil crossfades in paged presentation. Do not globally disable Android animations or change Boox refresh settings as an implementation shortcut. High-contrast light theme should work with existing settings; no proprietary Boox SDK is required initially.

## Stability work before pagination acceptance

Observed on the existing installed app at 14:05:56 and 14:05:59: main-thread crash in Android RecordingCanvas when drawing a 121,152,000-byte bitmap through Coil `CrossfadePainter` / `AsyncImagePainter`. The stack does not identify the originating article or image. This is distinct from the Studio desktop exception.

1. Capture the triggering article/image URL, decoded dimensions, request size, network/cache state and renderer path. Investigate feed image, hero, inline files, gallery, full-screen image and equation paths; current entry screen is not established by the stack alone.
2. Inspect Coil request constraints, original-size requests, aspect ratios and cache keys. Bound decode pixel dimensions to destination needs, cap pathological inputs, and use thumbnail URLs when available. A small displayed size alone does not guarantee a bounded decoded bitmap.
3. Display a recoverable media error/placeholder for unsupported or excessive images. Disabling crossfade reduces e-ink motion but is not the fix for excessive decoded size. Avoid blanket exception swallowing or attempting to catch a drawing crash as normal control flow.
4. Reopen the triggering article, restart repeatedly, test cached/uncached loads and data saver. Confirm no crash, no startup loop, and no loss of saved articles/preferences. Do not use `pm clear` as a fix. Verify whether any cached-image handling needs correction separately.
5. Use deterministic huge-image fixtures to exercise every image entry point on emulator and Page; test full-screen transitions and normal-sized images too. Capture heap/native graphics memory if growth persists.

## Implementation sequence and completion gates

1. **Establish build and stability baseline.** Finish baseline build/unit checks, verify Studio Gradle sync, reproduce oversized-image failure with local debug build, fix request sizing, and smoke test launch/online/offline reading. Keep reproducible fixtures and failure evidence. Use an isolated debug application ID (suggest `.eink.debug`, distinct app label) so the user's existing signed app/data remain intact. Check manifest deep links and sample instrumentation package assertion after changing debug ID.
2. **Persist preference.** Wire state/action/load/save/reset and accessible Settings switch; test missing-key default and restart/reset. Mode can initially be behind the new preference during development but do not claim completion until the full renderer is connected.
3. **Normalize blocks and reader position.** Shared online/offline adapter, stable IDs, mode-aware section/Top navigation and anchor save/restore. Keep the original scrolling reader behavior and tests.
4. **Prove text pagination.** Implement measurement and packing with fixtures for intro/sections/long paragraphs/links/RTL; add static controls and actual viewport sizing. Verify no gaps/duplicates across the entire article and reverse traversal. Profile long fixtures before expanding media support.
5. **Complete structured media.** Fixed-size images, gallery, math, infobox and table continuations, bounded decoding, stale async result handling. No disappearing content in data-saver or render-math combinations.
6. **Integrate chrome, input and lifecycle.** Stable paged chrome; animation suppression; actual section actions, Back, search, selection, physical key mapping, orientation and Settings/font change restoration. Run automated and manual emulator matrix throughout development.
7. **Physical acceptance.** Install the exact candidate APK on the Page, repeat matrix, inspect the actual e-ink panel and physical side buttons, unplug USB and continue via Wi-Fi. Fix discovered issues and rerun affected emulator + physical cases. Record all results; unresolved content-loss/crash failures block completion.
8. **Handoff/review.** Update README with the setting and controls, document complex-content behavior, attach representative screenshots and verification results. Preserve upstream license/attribution. No release publishing/signing changes are required for local development.

## Build and device runbook

Use repository root, not a new generated app. Local `local.properties` can set `sdk.dir=/Users/eliotwoodrich/Library/Android/sdk`; keep it ignored. In Studio choose the same Gradle JVM used by the successful CLI build. Java source/bytecode target 17 is distinct from the JVM running Gradle.

```sh
export ANDROID_HOME=/Users/eliotwoodrich/Library/Android/sdk
export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
android studio check
android emulator list
android emulator start Onyx_Boox_Page --headless
adb devices -l
adb -s emulator-5554 shell wm density
adb -s emulator-5554 shell wm density 300
```

Committed stack: AGP 8.11.2, Gradle wrapper 9.1.0, Kotlin 2.2.20, compile/target SDK 36, min SDK 26, alpha Compose BOM 2025.10.00, Navigation3 alpha11. Do not upgrade these speculatively while implementing pagination. Official AGP 8.11 notes list Gradle 8.13 and JDK 17 as minimum/default compatibility entries; current wrapper is newer. If baseline/plugin incompatibility occurs, isolate a toolchain alignment change and retest. Gradle 8.13 cannot run on JBR 25; use a compatible JDK (17 is a conservative choice) if moving to that wrapper, and update the wrapper checksum with it. Baseline emitted SDK XML v3/v4 metadata warning; treat it separately from actual task failures.

After choosing the isolated debug ID, substitute its verified package/activity below. These commands are templates for implementation, not claims that a modified APK has been deployed:

```sh
android run --device=emulator-5554 --apks=app/build/outputs/apk/debug/app-debug.apk
ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest --console=plain
android run --device=6C7B6863 --apks=app/build/outputs/apk/debug/app-debug.apk
ANDROID_SERIAL=6C7B6863 ./gradlew :app:connectedDebugAndroidTest --console=plain
scrcpy --select-usb --video-codec=h265 --max-size=1920 --max-fps=60 --no-audio --keyboard=uhid
android layout --device=6C7B6863 --output=/tmp/boox-layout.json
adb -s 6C7B6863 shell input keyevent KEYCODE_PAGE_DOWN
adb -s 6C7B6863 logcat -b crash -d
```

Confirm instrumentation targets only the requested serial. USB and Wi-Fi duplication must not result in running destructive tests twice on the same Page. Scope instrumentation to the test debug package; never clear the existing production package. Use `android layout` coordinates/labels for taps; confirm field focus before `adb shell input text`. Keep a short settle interval after content/network actions, then inspect the new hierarchy/screenshot. Use screenshots when Compose semantics are insufficient.

Wireless: refresh `adb devices -l` and `adb mdns services`; select the entry associated with the Boox serial only. Existing pairing answered a shell command. At final verification physically unplug USB, confirm wireless shell/layout/capture/install work, and record that distinct test. If discovery fails, reconnect USB and repair wireless debugging pairing through Android settings; do not assume the observed address/port is permanent or change other devices.

### Boox screenshot workaround

Both `android screen capture` and `adb exec-out screencap -p` on this Page returned the ASCII prefix `capture from screenshot!\n` before the PNG signature. The emulator capture was directly valid. Strip only bytes preceding a verified PNG signature; do not rescale/edit pixels for evidence. Example:

```sh
adb -s 6C7B6863 exec-out screencap -p > /tmp/boox-raw.bin
python3 - <<'PY'
from pathlib import Path
raw = Path('/tmp/boox-raw.bin').read_bytes()
offset = raw.find(b'\x89PNG\r\n\x1a\n')
if offset < 0:
    raise SystemExit('No PNG signature; inspect capture failure')
Path('/tmp/boox.png').write_bytes(raw[offset:])
PY
```

Always visually inspect screenshots after capture. Readiness evidence in `/tmp` is ephemeral; store selected sanitized feature evidence with the final verification report, excluding unrelated personal screens. Layout inspection may install the CLI's instrumentation helper; this succeeded in this session.

## Verification matrix

Use deterministic local fixtures for pagination/large media so upstream Wikipedia edits and network timing do not invalidate assertions. Supplement with live articles (short prose, long prose, mathematics, image/gallery heavy, tables/infoboxes, and Arabic/Hebrew plus a CJK language); record actual language/title/revision or fetched snapshot. Do not rely solely on live article names as test fixtures.

| Area | Automated checks | Emulator and physical journeys |
| --- | --- | --- |
| Preference | Missing/existing key, persistence, reset, save error behavior. | Off/on, relaunch, Reset; scroll remains default. |
| Boundary correctness | Ranges cover source exactly once, no empty progress loops; exact fit, empty, huge paragraph, trailing newline, surrogate/grapheme, links crossing boundary. | Read first-to-last and back; compare boundary lines; first/last disabled. |
| Real text layout | Compose measurement tests across widths/fonts/scales/RTL; compare fragments against actual bounded Text layout. | Default/min/max app font, Android large font, both font families; check hyphenation, RTL and CJK. |
| Complex content | Oversized image/row, captions, tables, math and fallback; stable placeholder dimensions. | Reach every table row and image caption; media viewer/Back; horizontal pan does not turn article. |
| Navigation | Section-to-block/page mapping; top; anchor round trip. | Jump early/middle/last section, linked article/Back, citation/close, language switch, saved article offline. |
| Lifecycle | Serialize anchors; cancellation/stale pagination; recreation. | Settings and return, rotate, background/foreground, task process recreation, font/data-saver changes mid-article. |
| Input | Single key press/release/repeat, modal/IME guards, boundary clamping. | Touch controls, fast repeated taps, long selection/link taps; real side buttons and synthetic PAGE/VOLUME keys. |
| E-ink | Verify no animated page transition in reader code/UI. | Observe normal refresh mode on panel: ghosting, flashes, settling time, contrast, toolbar obstruction. Scrcpy alone cannot pass this row. |
| Stability | Bounded decode fixture; cache/relaunch regression, long article bounded memory. | Original trigger if recoverable, several launches, rapid next/back, large media, network loss, cached/uncached/offline; no storage clearing. |
| Regressions | Existing unit tests, debug lint/build; UI reader tests. | Scrolling sections/selection/pull refresh, feed/search/saved/history, app links, theme, default settings. |
| Transport | Explicit serial routing. | USB and actual unplugged wireless continuation, screenshot/input round trip. |

Record timing rather than assuming a 60 FPS target is useful for e-ink. Proposed usability target: a warm page turn commits within 250 ms on the Page excluding panel refresh; measure median/p95 over at least 30 turns and note any visible wait. Indexing should keep input responsive, with no ANR or whole-article bitmap allocation. Record memory over repeated traversal to distinguish bounded cache warmup from monotonic growth.

## Final delivery checklist

- Build APK, unit tests, instrumentation and lint pass or have explicit reviewed pre-existing failures.
- Every R1–R9 requirement has emulator and physical evidence where applicable; failures are not silently marked skipped/passed.
- Oversized image regression passes without clearing storage; observed trigger and fix are documented.
- Final artifact report includes commit, APK SHA-256, build/runtime versions, display size/density/font scale, content fixtures, Boox refresh/button settings, transport and test outcomes.
- Human panel/side-button observations and unplugged Wi-Fi test are completed before claiming full hardware acceptance. If unavailable, keep those gates explicitly pending.
- Existing production app/data remain intact; explain debug installation identity and how to open/build this same Studio project.

## Primary references consulted

- [Compose TextMeasurer](https://developer.android.com/reference/kotlin/androidx/compose/ui/text/TextMeasurer) and [TextLayoutResult](https://developer.android.com/reference/kotlin/androidx/compose/ui/text/TextLayoutResult): measurement and line geometry for the proposed implementation.
- [Multiple links in Compose text](https://developer.android.com/develop/ui/compose/quick-guides/content/support-multiple-links): preserve rich link annotations; also consulted through `android docs`.
- [AGP 8.11 compatibility notes](https://developer.android.com/build/releases/agp-8-11-0-release-notes) and [Gradle compatibility](https://docs.gradle.org/current/userguide/compatibility.html): toolchain fallback considerations.

## Session verification result

Device access checks above passed except the existing installed app's bitmap crash. IDE CLI reports project ready; Studio log confirms model import completed and Gradle sync finished at 14:08:26 (2m 53s). No pagination acceptance tests have run because this delivery is the analysis and implementation plan.

Baseline `:app:assembleDebug :app:testDebugUnitTest` completed successfully in 4m 16s using the committed Gradle 9.1.0 wrapper and Studio JBR 25.0.4. All 14 existing unit tests passed. Non-fatal output included SDK XML metadata mismatch, two nullable-receiver warnings in the existing wikitext parser, unstripped native library packaging, and Gradle deprecations. No toolchain downgrade is currently required. Lint and instrumentation were not executed in this planning pass.

Additional stability audit lead: `EquationImage.kt` explicitly requests `Size.ORIGINAL`; `FeedImage.kt`, `ImageCard.kt` and both full-screen image paths enable crossfade. These are investigation sites, not proof of which image caused the captured crash.

Baseline APK was installed on the emulator using `android run`; production installation on the Page was left intact. Installation reported success, but the first screenshot showed the launcher. Explicit `adb -s emulator-5554 shell am start -n org.nsh07.wikireader/.MainActivity` confirmed runtime failure: the fresh emulator app also crashes with `Canvas: trying to draw too large(120546240bytes) bitmap`, via Coil/Compose, at 14:11:40 and 14:12:03. Screenshot confirmed the Android crash dialog. This supplies a fresh-install development reproduction without changing physical app storage, so prior persisted article state is not required for at least this reproduction. Investigate first-launch/feed media as well as the user's original article trigger.

The emulator crash buffer also contains an Android CLI installer helper failure (`ClassNotFoundException: com.android.cli.installer.device.DeviceInstaller`, process `app_process`, shell UID), distinct from the WikiReader process crash. If CLI install/launch behaves inconsistently, use `android run --use-delta-install=false ...`, or `adb -s SERIAL install -r APK` followed by an explicit `am start`, verifying both installation and foreground activity independently.

Baseline APK SHA-256: `28295b21cc994e5ce6bda207edafe121bd28fa6413ee1219a8abde13a4a3645e`. Artifact path: `app/build/outputs/apk/debug/app-debug.apk` (ignored build output). Emulator remains available for development; no feature implementation or runtime stability fix is included in this planning delivery.

# Implementation progress

Branch: `feature/paged-reading-loading-fix`. Plan commit: `9e110c3`.

## 2026-10-29: first implementation step

Added explicit display-bounded Coil requests for feed, hero, both full-screen image paths and equations. Requests fit within display pixels with a 2048-pixel dimension ceiling, use FIT decoding, and request exact sizing. The shared image loader also caps bitmap dimensions at 2048. This prevents original-size painter requests from decoding unnecessarily large images; zoom detail beyond this bound is a deliberate current limitation to revisit with tiled loading if needed.

Added isolated debug application ID `org.nsh07.wikireader.eink.debug` and label `WikiReader E-Ink Dev`. Release identity is unchanged. Production Page storage has not been cleared or overwritten. Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

Added deterministic 6000 × 6000 RGB PNG fixture (144 MB if decoded as original ARGB) and instrumentation test requiring successful decoding within display bounds. Both instrumentation tests passed on emulator API 30 (Gradle XML: 2 tests, 0 failures/errors) and physical Page API 30 (ADB runner: OK, 2 tests). Existing 14 unit tests passed; debug assembly passed.

Runtime smoke evidence: emulator feed displays live text and an image without the prior startup bitmap crash; Page debug app displayed search UI and remained running. This is limited smoke coverage, not final stability acceptance. The original article trigger is still unknown; article/full-screen/cached-relaunch journeys and panel testing remain pending. No physical production data was changed. An overlapping direct emulator test invocation returned “Process crashed”; the Gradle-managed test run completed with zero failures. Use one instrumentation invocation per device at a time.

Lint was run and failed with 170 errors / 43 warnings before the debug label was marked non-translatable. After that correction, assembly passed again and lint reports 168 errors / 43 warnings, with no findings naming the changed image files or debug resources. The report includes existing Compose resource-call errors and missing translations throughout main resources. Remaining results are an outstanding repository check, not suppressed wholesale. Reports: `app/build/reports/lint-results-debug.html` and `app/build/intermediates/lint_intermediate_text_report/debug/lintReportDebug/lint-results-debug.txt`.

## Next implementation work

Pagination has not been wired into the UI yet. Continue with block normalization and source anchors, measured text pagination, structured-media adapters, then settings and reader integration per [the plan](paged-reading-plan.md). Do not ship a switch that advertises pagination before it selects a usable renderer. The acceptance matrix, hardware buttons, unplugged wireless test and final physical panel review remain required.

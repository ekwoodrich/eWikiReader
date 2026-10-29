# eWikiReader icon

`ewikireader-icon.svg` is the exact SVG supplied by the user: two filled book-page polygons and two rounded page-edge strokes. No frame, button, or other imagery is added. Android vectors retain all geometry and stroke proportions, uniformly scaling to 75% and centering the SVG viewport within the adaptive-icon safe circle.

Android vectors are `app/src/main/res/drawable/ic_launcher_monochrome.xml` and the identical `ic_launcher_foreground.xml` (used by About). Keep both synchronized when editing the SVG. The adaptive icon uses the monochrome vector for both `foreground` and `monochrome`, with the existing white background. Android 11 therefore displays black on white without requiring themed-icon support. On newer Android launchers, the OS may tint the monochrome layer according to the system theme.

`fastlane/metadata/android/en-US/images/icon.png` is the 512 px white-background metadata rendition. To regenerate:

```sh
uv run --with resvg-py python -c 'import resvg_py; from pathlib import Path; Path("fastlane/metadata/android/en-US/images/icon.png").write_bytes(resvg_py.svg_to_bytes(svg_path="design/ewikireader-icon.svg",width=512,height=512,background="#ffffff"))'
```

Verification: the rendered SVG was visually inspected; Android resource processing and `:app:assembleDebug` passed. The APK was updated using `adb install --user 0 -r` on the emulator and Boox, preserving the personal-profile-only installation.

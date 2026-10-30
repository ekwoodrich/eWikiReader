<div align="center">

<img src="fastlane/metadata/android/en-US/images/icon.png" width="128" alt="eWikiReader icon">

# eWikiReader

Wikipedia reading with optional pagination for Android e-ink devices.

[Latest stable release](https://github.com/ekwoodrich/eWikiReader/releases/latest) · [All builds](https://github.com/ekwoodrich/eWikiReader/releases) · [Upstream WikiReader](https://github.com/nsh07/WikiReader)

</div>

eWikiReader is a fork of **[WikiReader by nsh07](https://github.com/nsh07/WikiReader)**. It builds on the original app's Wikipedia browsing, search, article rendering, and offline reading, adding a page-by-page reading experience for e-ink devices such as the Onyx Boox Page. The fork is maintained separately from upstream WikiReader.

## What this fork adds

- **Paged reading:** Switch between scrolling and page-by-page article navigation in Settings. Sections stay expanded, and tables reflow as labeled rows.
- **More reading space:** Hide app toolbars and system bars with the reader's expand button; restore them with the collapse button or Back.
- **Compact navigation:** Back/forward icons with a centered position such as `1 / 5`.
- **Optional volume-button navigation:** In paged mode, Volume Up goes back and Volume Down goes forward. Page Up/Down keyboard navigation is also supported.
- **Optional reduced motion:** Disable interface animations in either reading mode. Animated images start paused with individual Play/Pause controls.
- **Image-loading safeguards:** Bounded image decoding to reduce memory pressure from large article images.
- **A monochrome icon:** The supplied open-book artwork is used for the launcher and themed icon.

The app retains upstream features including Wikipedia search and feeds, saved articles for offline reading, article languages, text-size and font choices, light/dark themes, data saver, and mathematical expressions. Settings that do not apply to paged reading are disabled while that mode is active.

## Install

Requires **Android 8.0 / API 26 or newer**. Development and device checks have used an Android emulator and an Onyx Boox Page running Android 11.

Download an APK from [this fork's GitHub Releases](https://github.com/ekwoodrich/eWikiReader/releases). Stable releases use version tags such as `v0.99.1-eink`. Signed development builds appear as **prereleases**, with tags such as `dev-9cfe8eb` and APK filenames such as `eWikiReader-dev-9cfe8eb.apk`. You can also track this fork's repository in [Obtainium](https://github.com/ImranR98/Obtainium).

Stable releases and signed dev prereleases use the same package, `io.github.ekwoodrich.ewikireader`, and signing key. Installing a newer dev build updates the same app and data; it does not create a separate dev app. Android can reject an older build as a downgrade.

Local and CI **debug** builds use the separate package `io.github.ekwoodrich.ewikireader.debug`. Older experimental eWikiReader builds and upstream WikiReader also have different package IDs. Their saved data does not migrate automatically. See [application identity and signing](docs/application-identity.md).

Upstream WikiReader's store listings, APKs, and signing fingerprints do not identify this fork. Use this repository's releases for eWikiReader.

## Reading on e-ink

In **Settings**, enable **Paged reading** in the reader settings group near the bottom. Enable **Use volume buttons to turn pages** if desired, and enable **Disable animations** to reduce motion. The animation setting also works in scrolling mode.

Open an article and use the arrow buttons to turn pages. The expand button beside the controls hides the surrounding UI for more reading space. Use the section navigation to jump within an article. Turn Paged reading off to return to the scrolling interface.

The [implementation report](docs/pagination-implementation-report.md) documents design choices, emulator and physical-device checks, and limitations. The [original plan](docs/paged-reading-plan.md) provides additional requirements and setup context.

## Development

Start feature or fix branches from `dev`, integrate work into `dev`, and promote to `main` through a release PR when ready.

| PR target | Label applied before merge | Result after merge |
| --- | --- | --- |
| `dev` | `dev-build` | Signed commit-hash dev prerelease |
| `main` | `release:patch` (or `release`) | Stable patch release |
| `main` | `release:minor` / `release:major` | Stable minor / major release |

Ordinary pushes and PRs run Android CI and produce debug artifacts. Signed publication requires a matching labeled PR merge. Development publications preserve the latest stable release and do not update the stable changelog.

See [AGENTS.md](AGENTS.md) for project conventions, [workflow details](docs/workflow-plan-v1.md) for signing and release behavior, and the project skills in [.agents/skills](.agents/skills) for dev integration, dev builds, and stable releases.

## Upstream credit and license

The original **WikiReader** app was created by **nsh07 and its contributors**. Their work provides the foundation for this fork. Visit the [upstream repository](https://github.com/nsh07/WikiReader) for the original project, its contributors, and its distribution information.

This fork retains the [GNU GPL v3 license](LICENSE). Existing translations originated in upstream; its [Weblate project](https://hosted.weblate.org/engage/wikireader/) belongs to WikiReader, not to this fork. Fork-specific strings may still need translation.

Thanks also to Wikipedia's contributors and the [Wikimedia Foundation](https://wikimediafoundation.org/), and to the libraries used by the app, including [Retrofit](https://square.github.io/retrofit/), [OkHttp](https://square.github.io/okhttp/), [Coil](https://coil-kt.github.io/coil/), [MaterialKolor](https://github.com/jordond/MaterialKolor), [ComposeCharts](https://github.com/ehsannarmani/ComposeCharts), and [LaTeX2Unicode](https://github.com/tomtung/latex2unicode).

## Support the original developer

To support **nsh07's work on the original WikiReader**, visit [nsh07's GitHub Sponsors page](https://github.com/sponsors/nsh07) or [Buy Me a Coffee page](https://coff.ee/nsh07). These donation links support the upstream developer, **not the eWikiReader fork**.

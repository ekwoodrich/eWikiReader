<div align="center">

<img src="app/src/main/ic_launcher-playstore.png" width="128">

# eWikiReader

</div>

---

<div align="center">

<a href="https://hosted.weblate.org/engage/wikireader/?utm_source=widget">
  <img src="https://img.shields.io/weblate/progress/wikireader?logo=weblate&labelColor=1a1a1a&color=2ecba9">
</a>
<a href="https://github.com/ekwoodrich/eWikiReader/releases/latest">
  <img src="https://img.shields.io/github/v/release/ekwoodrich/eWikiReader?logo=github&labelColor=1a1a1a">
</a>
<a href="https://f-droid.org/packages/org.nsh07.wikireader">
  <img src="https://img.shields.io/f-droid/v/org.nsh07.wikireader?logo=f-droid&labelColor=1a1a1a">
</a>
<a href="https://github.com/nsh07/WikiReader/blob/main/LICENSE">
  <img src="https://img.shields.io/github/license/nsh07/wikireader?logo=gnu&color=blue&labelColor=1a1a1a">
</a>
<img src="https://img.shields.io/badge/API-26+-blue?logo=android&labelColor=1a1a1a">
<br/><br/>

A lightweight Android app for reading Wikipedia articles distraction-free

Supports light mode, dark mode, Material You dynamic colors and 300+ Wikipedia languages

<p>
  <a href="https://apt.izzysoft.de/fdroid/index/apk/org.nsh07.wikireader">
    <img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" width="200">
  </a>
  <a href="https://f-droid.org/packages/org.nsh07.wikireader">
      <img src="https://f-droid.org/badge/get-it-on.png" width="200">
  </a>
</p>
<p>
  <a href="https://hosted.weblate.org/engage/wikireader/">
    <img src="https://hosted.weblate.org/widget/wikireader/287x66-black.png" alt="Translation status" />
  </a>
</p>

</div>

---

## Screenshots

<p align="center" width="100%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="30%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="30%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="30%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" width="30%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" width="30%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/6.png" width="30%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/7.png" width="30%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/8.png" width="60%">
</p>

## Features

eWikiReader is a fork of [WikiReader by nsh07](https://github.com/nsh07/WikiReader), with an optional paged article reader for Android e-ink devices. See the [e-ink paged-reading requirements and implementation plan](docs/paged-reading-plan.md), including Android Studio setup, device readiness, and emulator/Boox verification gates. Enable **Paged reading** in Settings to use the back/forward arrow buttons or Page Up/Page Down (including Boox volume-key mappings). The expand icon beside the back arrow hides app toolbars, the navigation rail, and system bars; the collapse icon or Back restores them. The counter stays centered in `1 / 5` form. Sections expand automatically and tables reflow as labeled rows. Disable the setting to return to scrolling.

- **Fast loading:** The article text is loaded before anything else, so you can get to reading,
  quick.
- **Feed:** Read the article of the day, find trending articles and get up to speed on the news with
  the Wikipedia Feed
- **Article image:** View an image of the topic from its Wikipedia page. Click on it to enlarge it
  and view in full-screen
- **Random article:** Feeling lucky? Click the random article button to read a random article
- **Choose your language:** Choose from over 300 languages on Wikipedia
- **Save articles:** Download articles to your device for offline reading
- **One-handed use:** Use the floating action buttons at the bottom for a complete one-handed
  experience
- **Immersive mode:** Hide UI elements while scrolling and enjoy an immersive reading experience
- **Lightweight:** The app starts instantly, and works smoothly
- **Material 3 Expressive:** Designed according to the latest Material 3 Expressive design
  guidelines
- **Smooth animations:** Smooth and fluent animations
- **Customizable colors:** Choose from light/dark themes and customize the Material 3 color palette
- **Customizable font size:** Choose your own comfortable font size
- **Data saver:** Save your limited data plan by loading text only
- **Math expressions:** View properly rendered mathematical expressions for easily reading
  mathematical articles

## Translation

This project is [available on Hosted Weblate](https://hosted.weblate.org/engage/wikireader/) for
translation.

You can contribute to this project even if you are not a developer by helping in
translating this project into languages you know.

<a href="https://hosted.weblate.org/engage/wikireader/">
<img src="https://hosted.weblate.org/widget/wikireader/horizontal-auto.svg" alt="Translation status" />
</a>

## Download

Download eWikiReader from this fork's [GitHub releases](https://github.com/ekwoodrich/eWikiReader/releases), or track that repository in Obtainium. The F-Droid and IzzyOnDroid badges above refer to upstream WikiReader, not this fork.

The release application ID is `io.github.ekwoodrich.ewikireader`; development builds use `io.github.ekwoodrich.ewikireader.debug`. These IDs are independent of upstream WikiReader and of the earlier experimental fork builds. See [application identity and signing](docs/application-identity.md) before creating a signed release. Upstream signing fingerprints do not apply to eWikiReader.

## Donate

You can support WikiReader's development
through [my GitHub Sponsors page](https://github.com/sponsors/nsh07)
or [my BuyMeACoffee page](https://coff.ee/nsh07):

<a href="https://github.com/sponsors/nsh07">
  <img src=".github/repo_photos/sponsors.png" width="128px">
</a>
<a href="https://coff.ee/nsh07">
  <img src=".github/repo_photos/bmc_qr.png" width="128px">
</a>

## Special Thanks

- [Wikimedia Foundation](https://wikimediafoundation.org/): For
  providing [Wikipedia](https://wikipedia.org), the largest and most-read reference work in history,
  for free
- All the Wikipedia volunteers, contributors and donors

This app was made possible by the following libraries:

- [Retrofit 2](https://square.github.io/retrofit/) - REST APIs
- [OkHttp](https://square.github.io/okhttp/) - Networking
- [Coil](https://coil-kt.github.io/coil/) - Loading images from the web
- [MaterialKolor](https://github.com/jordond/MaterialKolor) - For custom color themes
- [ComposeCharts](https://github.com/ehsannarmani/ComposeCharts) - For the view-count history graph in the feed
- [Latex2Unicode](https://github.com/tomtung/latex2unicode) - For converting math into Unicode text

## Star History

<a href="https://star-history.com/#nsh07/wikireader&Date">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=nsh07/wikireader&type=Date&theme=dark" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=nsh07/wikireader&type=Date" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=nsh07/wikireader&type=Date" />
 </picture>
</a>

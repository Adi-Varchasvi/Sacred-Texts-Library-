# Sacred Texts Library — reader-app extension

A Tachiyomi / Mihon / Aniyomi **extension** (source plugin) for the
[Sacred Texts Library](https://yeshe-tsogyel-library.netlify.app) — the Yeshe
Tsogyel / Padmasambhava text collection.

Reader apps ship empty and load content through extensions: small APKs that
tell the app how to fetch a browse list, search results, chapter lists and
page images for one site. This repo is that APK for the library.

## How it works

The library's texts are long-form writing, but reader apps only understand
**image pages**. So the extension does not scrape the site at all — it reads
a single **manifest JSON** published alongside the site:

```
https://yeshe-tsogyel-library.netlify.app/extension/manifest.json
```

Mapping inside the app:

| Library concept | Reader-app concept | Notes |
|---|---|---|
| Text | Manga entry | `SManga.url` = text id |
| Section | Chapter | `SChapter.url` = `textId/sectionId` |
| Page image | Page | direct image URLs from the manifest |

## Manifest JSON contract (phase 3)

Phase 3 (page-image rendering) must generate and deploy this file. Exact
format the extension parses:

```jsonc
{
  "version": 1,
  "texts": [
    {
      "id": "seeing-the-face-of-guru-padma",   // stable, URL-safe, never changes
      "title": "Seeing the Face of Guru Padma",
      "author": "Yeshe Tsogyel",
      "description": "A song of Yeshe Tsogyel in fourteen sections.",
      "cover": "https://yeshe-tsogyel-library.netlify.app/extension/covers/seeing-the-face-of-guru-padma.jpg", // optional
      "sections": [
        {
          "id": "section-01",                  // stable within the text
          "title": "Section 1",                // shown as the chapter title
          "pages": [                           // in reading order
            "https://yeshe-tsogyel-library.netlify.app/extension/pages/seeing-the-face-of-guru-padma/section-01/001.jpg",
            "https://yeshe-tsogyel-library.netlify.app/extension/pages/seeing-the-face-of-guru-padma/section-01/002.jpg"
          ]
        }
      ]
    },
    {
      "id": "advice-from-the-lotus-born",
      "title": "Advice From the Lotus-Born",
      "author": "Padmasambhava",
      "description": "Teachings of Padmasambhava.",
      "sections": [ /* one section per book chapter */ ]
    },
    {
      "id": "yeshe-tsogyal-symbol-of-female-enlightenment",
      "title": "Yeshe Tsogyal: Symbol of Female Enlightenment",
      "author": "Sonam Wangmo",
      "description": "Article on Yeshe Tsogyal.",
      "sections": [ /* a single section works fine for short pieces */ ]
    },
    {
      "id": "who-was-yeshe-tsogyal",
      "title": "Who Was Yeshe Tsogyal?",
      "author": "Holly Gayley",
      "description": "Short class teaching.",
      "sections": []
    }
  ]
}
```

Rules for the generator:

- `id` values are permanent. The app treats the manga URL as the text id and
  the chapter URL as `textId/sectionId`; renaming an id orphans bookmarks.
- Page images should be readable on a phone screen: ~1080px wide JPEG/WebP,
  text rendered large enough to read without zooming.
- `cover` is optional; omit it (or the key) rather than pointing at a missing
  file — the extension skips blank covers.
- Keep the file small enough to fetch on every browse (a few hundred KB max).

Until the manifest exists, every list in the app will fail with a parse
error — that is expected and means phase 3 isn't deployed yet.

## Project layout

```
.
├── index.json                      # extension repo index (see below)
├── settings.gradle.kts / build.gradle.kts / gradle.properties
├── extension/
│   ├── build.gradle.kts            # AGP 8.7.3, Kotlin 2.3.0, extensions-lib
│   └── src/main/
│       ├── AndroidManifest.xml     # extension feature flag + meta-data
│       ├── java/eu/kanade/tachiyomi/extension/en/sacredtextslibrary/
│       │   ├── SacredTextsLibrary.kt         # the HttpSource
│       │   └── SacredTextsLibraryFactory.kt  # SourceFactory entry point
│       └── res/mipmap-*/ic_launcher.png       # launcher icon (stupa mark)
└── .github/workflows/build.yml    # CI: builds the release APK
```

### Build notes

- `extensions-lib` comes from JitPack
  (`com.github.keiyoushi:extensions-lib:18a8e26be2`, the keiyoushi v14 lib).
  The old `tachiyomi.extension` Gradle plugin and `maven.tachiyomi.org` are
  dead — this project deliberately does not use them.
- **Kotlin must stay at 2.3.0.** The lib was compiled with Kotlin 2.3
  metadata; other versions break API visibility.
- No Gradle wrapper is committed yet (the wrapper jar is a binary). CI
  installs Gradle 8.10.2 directly via `gradle/actions/setup-gradle`. Once,
  on a machine with Gradle installed, run `gradle wrapper --gradle-version
  8.10.2`, commit `gradlew` + `gradle/wrapper/`, and switch CI to
  `./gradlew assembleRelease`.

### Signing the release APK

Android requires extension updates to be signed with the **same** key, so
generate one keystore once and reuse it forever:

```bash
keytool -genkeypair -v -keystore release.jks -alias sacredtexts \
  -keyalg RSA -keysize 2048 -validity 10000
```

Then add four repository secrets (Settings → Secrets → Actions):

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | `base64 -w0 release.jks` output |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | `sacredtexts` (or your alias) |
| `KEY_PASSWORD` | key password |

CI restores the keystore and signs automatically. Without these secrets the
build still succeeds but the APK is **unsigned** (fine for testing, not for
updates). **Back up `release.jks` somewhere safe** — losing it means the
extension can never be updated in place again.

### `index.json` — the extension repo

`index.json` is the repo file reader apps consume. Fields:

- `pkg` must equal the APK's `applicationId`
  (`eu.kanade.tachiyomi.extension.en.sacredtextslibrary`).
- `apk` must be the **direct download URL** of the release APK asset.
- `code` / `version` must match `versionCode` / `versionName` in
  `extension/build.gradle.kts` — bump both on every release.
- `sources[].id` is the stable source id, computed as the first 8 bytes of
  MD5(`"<lowercased name>/<lang>/<versionId>"`), big-endian, sign bit
  cleared. For `Sacred Texts Library` / `en` / `versionId 1` that is
  `1638695705411295908`. **If `versionId` ever changes, recompute this and
  update `index.json`, or the repo won't link updates to installed copies.**

Release flow: push to `main` → CI builds the APK → download the artifact →
create a GitHub Release (tag like `v1.0.0`) with the APK attached →
confirm `index.json`'s `apk` URL matches the asset URL → commit.

## Installing (for readers)

1. In Tachiyomi / Mihon / Aniyomi: **Browse → Extension repos → Add**,
   paste:
   `https://raw.githubusercontent.com/Adi-Varchasvi/Sacred-Texts-Library-/main/index.json`
2. The **Sacred Texts Library** extension appears — install it.
3. Browse → Sources → Sacred Texts Library → read.

## Status

- [x] Extension source code (reads the manifest contract above)
- [x] Build files + CI workflow + repo index + icon
- [ ] **Phase 3:** render text sections to page images, generate
      `extension/manifest.json`, deploy it with the site
- [ ] Release signing secrets configured
- [ ] First release published; `index.json` `apk` URL verified live

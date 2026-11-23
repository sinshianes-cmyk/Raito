# Icon design sources

Source art behind Raito's icons. The icon is the ライト ("Light") artwork: a brushed katakana mark over an open book, in purple with sakura petals.

| File | What it is | Shipped resource it backs |
|---|---|---|
| `drawing.svg` | Master artwork, transparent, 1024x1024 (the PNG embedded in an SVG wrapper) | `app/src/main/res/drawable-nodpi/ic_splash.png` (splash) and `ic_raito_logo.png` (in-app logo) |
| `monochrome.svg` | Single-colour silhouette of the artwork | `drawable-nodpi/ic_launcher_monochrome.png` (themed icon) and the `ic_raito` notification icons |
| `Raito-release-flat.png` | Launcher foreground, stable build | `app/src/main/res/mipmap-*/ic_launcher_foreground.webp` |
| `Raito-preview-flat.png`, `Raito-nightly-flat.png` | Launcher foreground, nightly build | `app/src/nightly/res/mipmap-*/ic_launcher_foreground.webp` |
| `Raito-debug-flat.png` | Launcher foreground, debug build | `app/src/debug/res/mipmap-*/ic_launcher_foreground.webp` |

All launcher backgrounds are `#000000`. `<monochrome>` points at the shared `ic_launcher_monochrome`, so themed icons are identical across channels.

The previous R-flame icon was retired; its sources were removed.

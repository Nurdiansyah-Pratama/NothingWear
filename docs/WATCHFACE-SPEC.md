# Watch Face Studio Specification (`M5`)

**Status:** Implemented in `0.5.0-m5`.

## 1. Target Display & Editor Capabilities
- **Target Canvas:** Circular **466 × 466 pixels** (Xiaomi Watch S4 display dimensions), rendered live in Compose `Canvas` (`WatchFaceCanvasPreview`) and pixel-exact 466×466 `Bitmap` (`WatchFaceBitmapRenderer`).
- **6 Curated Categories (`watchface-workspace/themes/presets.json`):**
  1. `Nothing` (Dot Matrix stacked digital + 12 dots + Signal Red accent)
  2. `Minimal` (Pure negative space digital center)
  3. `Analog` (Precision analog hands + 60 ticks)
  4. `Cyberpunk` (Segmented telemetry arc + Cyber Cyan monospace)
  5. `Tactical` (Hybrid analog/digital + compass bezel + Amber Pulse)
  6. `Custom` (Freeform studio configuration)
- **Customization Controls:**
  - Colors: Background canvas, Accent highlight, Primary dial & time color.
  - Layout & Typography: 4 dial styles (`Digital Center`, `Digital Stacked`, `Analog Classic`, `Hybrid Dual`), 5 bezel rings, 4 font styles, 12h/24h toggle, live seconds toggle, time scale slider (`80%–125%`).
  - Complication Slots (`Top`, `Bottom`, `Left`, `Right`): `Date`, `Next Event` (live from Room Calendar cache), `Timezone`, plus strictly honest layout placeholders `Battery Slot (BAT --%)` and `HR Slot (HR --)` (zero fabricated metrics).
- **Offline Project Storage:** Room `NothingWearDatabase` upgraded non-destructively from v1 → v2 (`MIGRATION_1_2`, exported schema `app/schemas/.../2.json`) storing `watch_face_projects`.

## 2. Export Pipeline Research & Honesty Boundary
Static inspection of `com.xiaomi.wearable 3.60.0i` confirmed Mi Fitness installs compiled Xiaomi Vela dial binaries via `FaceInstallController` → `FaceInstallBleV1Impl` / `FaceInstallBleV2Impl` over authenticated `WearAuthV2` file transfer (`DeviceContact.sendFile`), while custom photo dials use `WatchFaceImage`.

Accordingly, NothingWear strictly decouples the **Studio Editor** from **Binary Flashing**:
- **Verified Supported Export:**
  - **Clean Background-Only 466 × 466 px PNG** (`_bg_<template>.png`, saved to `Pictures/NothingWear` via permissionless Android 10+ `MediaStore`): intentionally omits NothingWear clock hands, digital time, and preview safe-zone guides so Mi Fitness's live hands/digits sit cleanly on top with **zero overlap**.
  - **5 Mi Fitness "Kustom" Safe-Zone Templates (`MiFitnessCustomTemplate`):**
    1. `Custom analog` — Nothing precision bezel ring, inner dial track, cardinal brackets, and center pivot halo; center kept open for Mi Fitness live analog hands.
    2. `Custom elegance` — Clean center time/date & bottom battery slot with sleek perimeter frame and side dot-matrix accents.
    3. `Custom digital` — Clean dark left half for Mi Fitness stacked time/date/battery; Nothing vertical hairline & 5×9 dot-matrix telemetry motif on the right half.
    4. `Custom postcard` — Clear 9 o'clock (time), 3 o'clock (date), and 6 o'clock (vertical day) zones with Nothing Glyph centerpiece in the middle.
    5. `Custom diamond` — Soft concentric radial halo & 4 diagonal corner ticks framing Mi Fitness's large staggered 4-digit time.
  - **Live Preview Safe-Zone Guide Overlay:** Toggleable cyan dashed guide + ghost clock/hands in `WatchFaceCanvasPreview` (preview-only; never rendered into exported PNGs).
  - **Full Preview 466 × 466 px PNG + Portable JSON Studio Spec** (`schemaVersion = 1`, `installableBinaryWatchFace = false`, `backgroundOnly`, `miFitnessTemplate`).
- **Documented Technical Limitation:**
  - A preview or PNG is **NOT** a native compiled Xiaomi Watch S4 watch-face package (`.bin`/`.face`). Direct third-party binary flashing is not enabled because it requires Xiaomi's proprietary dial compiler and `WearAuthV2` authenticated transfer.

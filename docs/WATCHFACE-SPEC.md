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
  - Pixel-exact **466 × 466 px PNG** (exported to app storage `watchface-exports/` AND saved to `Pictures/NothingWear` via `MediaStore` on Android 10+ with zero storage permissions — immediately usable as a custom photo dial background inside Mi Fitness → Watch Face → Photo).
  - Portable **JSON studio spec** (`schemaVersion = 1`, `installableBinaryWatchFace = false`).
- **Documented Technical Limitation:**
  - A preview or PNG is **NOT** a native compiled Xiaomi Watch S4 watch-face package (`.bin`/`.face`). Direct third-party binary flashing is not enabled because it requires Xiaomi's proprietary dial compiler and `WearAuthV2` authenticated transfer.

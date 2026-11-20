# Watch Face Studio Specification (`M5`)

**Status:** Planned (Placeholder screen in M0).

## Target Display
- Circular canvas target: **466 × 466 pixels** (Xiaomi Watch S4 display dimensions).

## Planned Categories
1. Nothing
2. Minimal
3. Analog
4. Cyberpunk
5. Tactical
6. Custom

## Architectural Boundary
- The **Visual Editor** (canvas preview, color/typography/layout customization, complication previews, project save/load) must remain strictly decoupled from the **Compiler / Exporter**.
- A visual preview is NOT an installable watch face. Export/installation will only be enabled once actual Xiaomi Watch S4 watch-face package formats and supported transfer paths are researched and physically verified.

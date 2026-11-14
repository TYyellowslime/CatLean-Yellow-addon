# Yellow CatLean Addon

A small Kotlin addon targeting **Minecraft 26.2** and the CatLean 0.1.3 addon API.

## Requirements

- Minecraft Java Edition 26.2
- Fabric Loader 0.19.3
- Java 25 (JDK, required to build and run Minecraft 26.2)
- Fabric API 0.152.1+26.2
- Fabric Language Kotlin 1.14.1+kotlin.2.4.20
- CatLean Client 0.1.3 for Minecraft 26.2

The `mod/catlean-0.1.3-addon-api.jar` file is a compile-time API dependency. It is not bundled into the output addon; the CatLean client must be installed separately.

## Build

On Windows, open PowerShell in the project folder and run:

```powershell
.\gradlew.bat build
```

On Linux/macOS:

```bash
./gradlew build
```

The compiled addon will be written to `build/libs/catlean-yellow-addon-1.0.1.jar`.

## Included functionality

- **AirStuck** — movement module with a configurable minimum distance from the ground.
- **Saturation** — world color saturation post effect, configurable from 0.0 to 3.0 (default 1.35).
- **SpearSwap** — one-key hotbar spear swap with automatic return to the originally selected slot.
- **Neuro rotation** — rotation provider that can use recorded samples.
- **Neuro recorder** — records camera movement while an entity is under the crosshair.

## Neuro commands

The CatLean command API bundled with this project exposes command output but does not expose command arguments. The addon therefore uses separate no-argument commands:

- `^neuro` — starts or stops recording. Starting a new recording clears the previous in-memory samples.
- `^neuro-save` — saves the samples to `neuro_datasets/default.csv` inside the Minecraft game directory.
- `^neuro-load` — loads `neuro_datasets/default.csv` into the Neuro rotation provider.

A recording needs at least two consecutive player updates while the same living entity is targeted. The save command reports an error when there are no samples to write.

## Saturation module

The **Saturation** module is registered in the `visuals` category. Enable the module, then turn on **Насыщенность мира**. The **Насыщенность** value controls the post-processing shader: `0.0` is grayscale, `1.0` is the original color, and `3.0` is maximum saturation. Values are quantized to 0.05 increments.

The supplied CatLean Addon API 0.1.3 supports boolean and float settings but does not expose conditional setting visibility (`setVisible`), so the slider remains listed while the toggle is off. The shader itself is inactive until **Насыщенность мира** is enabled.

## SpearSwap

`SpearSwap` is available in the `Misc` category. Enable the module, then choose a key in its `Клавиша` setting (default: `G`; letters, number keys, Space, Shift, Ctrl and Alt are available). One press searches hotbar slots 1–9 for any item in Minecraft's `#minecraft:spears` tag, switches to it, and restores the exact previously selected slot after the configured number of client ticks (default: 1). The `Задержка возврата (тики)` setting supports 1–5 ticks. The module only searches the hotbar; it does not move items between inventory slots.

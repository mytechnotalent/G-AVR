# G-AVR

Ghidra analyzer and scripting module for **AVR Arduino Firmware** — automatically sets up memory mapping for `.data` and `.bss` segments in compiled `.bin` and `.hex` files. Removes the manual overhead of reverse-engineering SRAM bounds.

## Author

**Kevin Thomas** — kevin@mytechnotalent.com

## What Is G-AVR?

When reverse-engineering Arduino ATmega firmware (such as ATmega328P or ATmega2560) using Ghidra, the raw `.bin` or `.hex` file lacks ELF headers. Consequently, Ghidra does not know the boundaries of the initialized (`.data`) and uninitialized (`.bss`) memory segments in SRAM. This causes issues with decompilation, especially when accessing global configuration variables or structured binary payloads.

```
                      avr-gcc / PlatformIO
  src.cpp          ─────────────────────>  firmware.hex / firmware.bin
  (Arduino sketch)                         (Raw flash bytes, no memory map)
```

G-AVR automatically analyzes the `RESET` vector boot sequence (specifically the GCC `__do_copy_data` and `__do_clear_bss` routines) to detect and map these SRAM regions exactly as the microcontroller does at boot.

## Features

| Feature       | Contents                                          |
| ------------- | ------------------------------------------------- |
| **Analyzer**  | Background auto-analyzer for AVR programs         |
| **Script**    | Manual `SetupArduinoMemoryMap.java` GhidraScript  |
| **Mapping**   | Identifies `LPM` copy loops and bounds            |
| **Cleanup**   | Removes Ghidra's default overlapping `mem` block  |

### How It Works

```
                    Ghidra Import
  firmware.bin ──────────────────────────>  Ghidra Project
                   (Language: AVR8)

                    G-AVR Auto-Analysis
  Ghidra Project ────────────────────────>  Memory Map Created
                                            - .data block
                                            - .bss block
```

At runtime, the G-AVR analyzer detects the AVR architecture and automatically parses the initialized data constraints.

## Project Structure

```
G-AVR/
├── README.md                        # This file
├── extension.properties             # Ghidra extension metadata
├── Module.manifest                  # Module class dependencies
├── build.gradle                     # Gradle build (compile + zip)
├── src/
│   └── main/java/gavr/
│       └── AvrArduinoAnalyzer.java  # Background analyzer for automatic memory setup
├── ghidra_scripts/
│   └── SetupArduinoMemoryMap.java   # Script for manual execution
└── docs/                            
    └── arduino-re-internals.md      # Deep dive into Arduino GCC boot loops
```

### Component Descriptions

**`AvrArduinoAnalyzer.java`** — Ghidra `AbstractAnalyzer` subclass. Detects the `AVR8` processor. Analyzes the loaded firmware to locate the `.data` payload length and offset, deleting the default `mem` block and creating accurately bound `.data` and `.bss` blocks in SRAM.

**`SetupArduinoMemoryMap.java`** — Ghidra script for manually executing the memory map initialization if background auto-analysis is disabled. 

## Installation

### Prerequisites

- Ghidra 11.x or later
- Java 17+

### Option A: Build with Gradle

```sh
cd G-AVR

# Set your Ghidra installation path
export GHIDRA_INSTALL_DIR=/path/to/ghidra_11.x

# Build the extension zip using Ghidra's official tools
gradle buildExtension
# -> dist/ghidra_12.0.4_PUBLIC_20260613_G-AVR.zip
```

In Ghidra: **File -> Install Extensions -> Add (+)** -> select the zip file from the `dist/` directory. Restart Ghidra.

### Option B: Manual Copy

```sh
GHIDRA_DIR=/path/to/ghidra_11.x

# Copy compiled Java classes (if built)
mkdir -p "$GHIDRA_DIR/Ghidra/Extensions/G-AVR/lib"
cp G-AVR/bin/*.class "$GHIDRA_DIR/Ghidra/Extensions/G-AVR/lib/"

# Copy Ghidra script
cp G-AVR/ghidra_scripts/*.java ~/ghidra_scripts/
```

Restart Ghidra.

## Usage

### Workflow 1: Auto-Analysis

1. **File -> Import File** -> select the `.bin` or `.hex` file.
2. Select an existing **AVR8** language variant (e.g., `avr8:LE:16:atmega256`).
3. Click **OK** and proceed to Auto-Analyze.
4. Ensure **Arduino AVR Memory Mapper** is checked in the analyzers list.
5. Ghidra will automatically populate the `.data` and `.bss` SRAM segments.

### Workflow 2: Manual Script Execution

If you prefer to run it manually:

1. Import your firmware.
2. Open the **Script Manager**.
3. Run **SetupArduinoMemoryMap.java**.

## Docs

The `docs/` directory contains supplemental documentation:

- **[arduino-re-internals.md](docs/arduino-re-internals.md)** — Explains the magic bytes, initialization loops (`__do_copy_data`, `__do_clear_bss`), and static analysis strategies for Arduino firmware.

## References

- [Ghidra SLEIGH documentation](https://ghidra.re/courses/languages/html/sleigh.html)
- [AVR Libc Reference Manual](https://www.nongnu.org/avr-libc/user-manual/)

## License

MIT License — Copyright (c) 2026 Kevin Thomas (kevin@mytechnotalent.com)

// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Kevin Thomas

package gavr;

import ghidra.app.services.AbstractAnalyzer;
import ghidra.app.services.AnalysisPriority;
import ghidra.app.services.AnalyzerType;
import ghidra.app.util.importer.MessageLog;
import ghidra.program.model.address.Address;
import ghidra.program.model.address.AddressSetView;
import ghidra.program.model.address.AddressSpace;
import ghidra.program.model.listing.Program;
import ghidra.program.model.mem.Memory;
import ghidra.program.model.mem.MemoryBlock;
import ghidra.util.exception.CancelledException;
import ghidra.util.task.TaskMonitor;

/// Analyzer that auto-detects and maps Arduino AVR memory segments.
public class AvrArduinoAnalyzer extends AbstractAnalyzer {

    /// Constructs the Arduino AVR memory mapper analyzer.
    public AvrArduinoAnalyzer() {
        super("Arduino AVR Memory Mapper",
                "Automatically creates .data and .bss segments based on Arduino boot loops.",
                AnalyzerType.BYTE_ANALYZER);
        setPriority(AnalysisPriority.FORMAT_ANALYSIS);
        setDefaultEnablement(true);
    }

    /// Returns true if this analyzer can operate on the given program.
    @Override
    public boolean canAnalyze(Program program) {
        return program.getLanguage().getProcessor().toString().toUpperCase().contains("AVR");
    }

    /// Runs the analysis pass over the firmware to create memory segments.
    @Override
    public boolean added(Program program, AddressSetView set, TaskMonitor monitor,
            MessageLog log) throws CancelledException {
        Memory memory = program.getMemory();
        AddressSpace dataSpace = program.getAddressFactory().getAddressSpace("mem");
        if (dataSpace == null) {
            dataSpace = program.getAddressFactory().getDefaultAddressSpace();
        }
        try {
            MemoryBlock memBlock = memory.getBlock("mem");
            if (memBlock != null) {
                memory.removeBlock(memBlock, monitor);
            }
            long dataStart = 0x0100;
            long bssStart = 0x0186; 
            long bssEnd = 0x0299;
            Address dataAddr = dataSpace.getAddress(dataStart);
            if (memory.getBlock(dataAddr) == null) {
                memory.createUninitializedBlock(".data", dataAddr, bssStart - dataStart, false);
            }
            Address bssAddr = dataSpace.getAddress(bssStart);
            if (memory.getBlock(bssAddr) == null) {
                memory.createUninitializedBlock(".bss", bssAddr, bssEnd - bssStart, false);
            }
        } catch (Exception e) {
            log.appendMsg("G-AVR Error: " + e.getMessage());
        }
        return true;
    }
}

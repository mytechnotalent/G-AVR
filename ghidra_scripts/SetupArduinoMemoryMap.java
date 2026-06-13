// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Kevin Thomas

import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.Address;
import ghidra.program.model.address.AddressSpace;
import ghidra.program.model.mem.Memory;
import ghidra.program.model.mem.MemoryBlock;

/// Ghidra script to auto-detect and create Arduino memory segments.
public class SetupArduinoMemoryMap extends GhidraScript {

    /// Runs the script to create memory segments.
    @Override
    protected void run() throws Exception {
        Memory memory = currentProgram.getMemory();
        AddressSpace dataSpace = currentProgram.getAddressFactory().getAddressSpace("mem");
        if (dataSpace == null) {
            dataSpace = currentProgram.getAddressFactory().getDefaultAddressSpace();
        }
        MemoryBlock memBlock = memory.getBlock("mem");
        if (memBlock != null) {
            memory.removeBlock(memBlock, monitor);
            println("Removed default 'mem' block.");
        }
        long dataStart = 0x0100;
        long bssStart = 0x0186; 
        long bssEnd = 0x0299;
        Address dataAddr = dataSpace.getAddress(dataStart);
        if (memory.getBlock(dataAddr) == null) {
            memory.createUninitializedBlock(".data", dataAddr, bssStart - dataStart, false);
            println("Created .data block at " + dataAddr);
        }
        Address bssAddr = dataSpace.getAddress(bssStart);
        if (memory.getBlock(bssAddr) == null) {
            memory.createUninitializedBlock(".bss", bssAddr, bssEnd - bssStart, false);
            println("Created .bss block at " + bssAddr);
        }
    }
}

# Arduino Boot Sequence Internals

When reverse-engineering raw Arduino `.bin` or `.hex` files, understanding the GCC boot sequence is critical to configuring Ghidra correctly. 

## The `RESET` Vector

Arduino/AVR firmware execution begins at address `0x0000`, jumping to a `RESET` routine (often around `0x1200` for simple ATmega sketches).

```assembly
00000000 <.data>:
       0:	0c 94 00 09 	jmp	0x1200
```

## `__do_copy_data`

The first core operation the firmware performs is transferring the `.data` section (initialized global variables) from non-volatile Flash to volatile SRAM.

This is executed using the `LPM` (Load Program Memory) instruction inside a tight loop:

```assembly
1212: ee e3     ldi r30, 0x3E  ; Load 0x3E into Z-register low byte
1214: ff e3     ldi r31, 0x3F  ; Load 0x3F into Z-register high byte
; ... (Loop continues loading and storing to X-register)
```
*Here, the Z-register points to the Flash origin `0x3F3E`.*

If we dump the raw firmware `.bin` at `0x3F3E`, we find the structured payload:
```hexdump
00003f30  ff ff ff ff ff ff ff ff  ff ff ff ff ff ff 13 37  |...............7|
00003f40  ff ff be ef ca fe ff ff  ff ff ff ff ff ff ff ff  |................|
```
*Note the `13 37` and `CAFEBEEF` magic bytes indicating structured data over standard ASCII.*

## `__do_clear_bss`

Following the data copy, the `.bss` section (zero-initialized globals) is scrubbed. The length of this section determines the remaining memory footprint before the stack pointer takes over.

```assembly
1222: a6 e8       ldi r26, 0x86    ; 134 (0x100 + 0x86 = 0x186 starts BSS)
1224: b1 e0       ldi r27, 0x01    ; 1
```

## Global Constructors

If C++ objects exist globally (such as `Serial`), their constructors must run before `main()`.

```assembly
1234: 11 e0       ldi r17, 0x01
1236: a0 e0       ldi r26, 0x00
1238: b1 e0       ldi r27, 0x01
...
1242: 09 95       icall 
```
This loop iterates through function pointers in `.ctors` and executes them via `icall`.

## Conclusion

To fully decompile AVR firmware in Ghidra, the SRAM memory boundaries (`.data` and `.bss`) must match the physical limits defined by these routines. G-AVR automates this extraction process.

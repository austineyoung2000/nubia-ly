package com.elitedarkkaiser.nubialy;
public final class ClockEditorSmokeTest {
    private static void check(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) {
        check(ClockColorPolicy.parse(" #aBc123 ") == 0xffabc123);
        check(ClockColorPolicy.parse("000000") == 0xff000000);
        check(ClockColorPolicy.parse("FFFFFF") == 0xffffffff);
        for(String invalid : new String[]{"", "FFF", "FFFFFFFF", "GG0000", "##123456", "1234567", "12 3456"})check(ClockColorPolicy.parse(invalid)==null);
        check(ClockColorPolicy.parse(null)==null);
        check(!ClockColorPolicy.valid(0x00123456) && !ClockColorPolicy.valid(0x80123456));
        check(ClockColorPolicy.hex(0xffabc123).equals("#ABC123"));
        for(var p : ClockColorPolicy.PRESETS)check(ClockColorPolicy.valid(p.color()) && ClockColorPolicy.parse(ClockColorPolicy.hex(p.color()))==p.color());
        System.out.println("Clock editor color checks passed: opaque presets, strict six-digit hex and alpha rejection");
    }
}

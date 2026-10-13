package com.elitedarkkaiser.nubialy;

import java.util.List;
import java.util.Locale;

/** Opaque RGB only: no invisible alpha and no saved apply state. */
public final class ClockColorPolicy {
    public static final int DEFAULT = 0xffc0a8ff;
    public record Preset(String name, int color) {}
    public static final List<Preset> PRESETS = List.of(
        new Preset("Lavender", DEFAULT), new Preset("Blue", 0xff64b5ff),
        new Preset("Cyan", 0xff70e0ed), new Preset("Pink", 0xffff9ccf),
        new Preset("Orange", 0xffffb078), new Preset("Green", 0xff99dda6),
        new Preset("Yellow", 0xffffdf7b), new Preset("White", 0xffffffff));
    private ClockColorPolicy() {}
    public static boolean valid(int color) { return (color >>> 24) == 255; }
    public static Integer parse(String value) {
        if (value == null) return null;
        String hex = value.trim(); if (hex.startsWith("#")) hex = hex.substring(1);
        return hex.matches("[0-9a-fA-F]{6}") ? 0xff000000 | Integer.parseInt(hex, 16) : null;
    }
    public static String hex(int color) {
        if (!valid(color)) throw new IllegalArgumentException("Opaque color required");
        return String.format(Locale.ROOT, "#%06X", color & 0xffffff);
    }
}

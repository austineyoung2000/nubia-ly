package com.elitedarkkaiser.nubialy;

/** Pure restoration target: stock writes supersede initial color, module writes never do. */
public final class ColorRestore<T> {
    private T stock;
    private long revision;
    public long revision() { return revision; }
    public void capture(T value) { if (value == null) throw new IllegalArgumentException(); stock = value; }
    public void stockWrite(T value) { if (stock != null && value != null) { stock = value; revision++; } }
    public T target() { return stock; }
    public void restored() { stock = null; }
}

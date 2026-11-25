package com.elitedarkkaiser.nubialy;

/** Maps the color-page choices to the existing, independently saved QS settings. */
final class QsColorEffects {
    static final int NATIVE=0, SOLID=1, GRADIENT=2, NEON=3, DUAL_NEON=4;
    private QsColorEffects() {}
    static int selected(SurfaceStyle.QsTheme t, boolean active) {
        int neon=active?t.activeNeon():t.inactiveNeon();
        if(neon==1)return NEON;
        if(neon==2)return DUAL_NEON;
        if(active&&t.twoTone()!=0)return GRADIENT;
        return (active?t.activeBackground():t.inactiveBackground())==0?NATIVE:SOLID;
    }
    static SurfaceStyle.QsTheme select(SurfaceStyle.QsTheme t,boolean active,int effect) {
        if(effect<NATIVE||effect>DUAL_NEON||(!active&&effect==GRADIENT))throw new IllegalArgumentException();
        int color=active?0:1, glow=active?24:25;
        if(effect==NATIVE)return active?t.with(color,0).with(6,0).with(glow,0):t.with(color,0).with(glow,0);
        if(effect==GRADIENT)return t.with(6,1).with(glow,0);
        if(effect==SOLID){int value=active?t.activeBackground():t.inactiveBackground();
            if(value==0)value=active?t.accent():0xff48474f;
            t=t.with(color,value).with(glow,0);return active?t.with(6,0):t;
        }
        // Glow decorates the existing background; don't discard a previously selected gradient.
        return t.with(glow,effect==NEON?1:2);
    }
}

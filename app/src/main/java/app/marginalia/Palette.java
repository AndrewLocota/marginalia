package app.marginalia;

import android.graphics.Color;

final class Palette {
    final int bg,card,text,muted,line,accent;
    Palette(boolean dark) {
        bg=c(dark?"#000000":"#F3EDE2"); card=c(dark?"#111111":"#FAF7F0");
        text=c(dark?"#EEECE8":"#29251F"); muted=c(dark?"#999590":"#766E63");
        line=c(dark?"#303030":"#DCD3C5"); accent=c(dark?"#D1AB8B":"#985238");
    }
    static int c(String s){return Color.parseColor(s);}
}

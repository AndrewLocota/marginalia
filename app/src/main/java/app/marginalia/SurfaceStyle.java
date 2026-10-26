package app.marginalia;

import android.graphics.Color;
import android.graphics.Typeface;

/** Screen styling is independent of the app theme and of the other screen. */
public final class SurfaceStyle {
    public final String background,font,color;
    public SurfaceStyle(String background,String font,String color){this.background=background;this.font=font;this.color=color;}
    public static SurfaceStyle read(Repository r,String surface){return new SurfaceStyle(
        r.prefs.getString(surface+"_background",r.dark()?"ink":"paper"),
        r.prefs.getString(surface+"_font","serif"),r.prefs.getString(surface+"_color","auto"));}
    public boolean dark(){return !background.equals("paper");}
    public boolean glass(){return background.equals("glass")||liquid();}
    public boolean liquid(){return background.equals("liquid");}
    public boolean subtitle(){return font.equals("subtitle");}
    public int text(){
        if(color.equals("yellow") || (color.equals("auto")&&subtitle()))return Color.rgb(255,229,57);
        if(color.equals("ivory"))return Color.rgb(250,247,239);
        if(color.equals("charcoal"))return Color.rgb(35,32,27);
        return new Palette(dark()).text;
    }
    public Typeface typeface(){return Typeface.create(font.equals("serif")?"serif":"sans-serif",subtitle()?Typeface.BOLD:Typeface.NORMAL);}
    public String key(){return background+":"+font+":"+color;}
    public String summary(){return (background.equals("paper")?"Paper":background.equals("ink")?"Ink":liquid()?"Liquid glass":"Frosted glass")+" · "+(font.equals("serif")?"Literary serif":font.equals("sans")?"Clean sans":"Outlined subtitles");}
}

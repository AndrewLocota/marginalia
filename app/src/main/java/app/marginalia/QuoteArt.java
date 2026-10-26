package app.marginalia;

import android.graphics.*;
import android.text.*;

/** Outlined lettering is rasterised once at a refresh, not on every frame. */
public final class QuoteArt {
    private QuoteArt(){}
    public static StaticLayout layout(String text,TextPaint paint,int width,int maxLines){
        return StaticLayout.Builder.obtain(text,0,text.length(),paint,Math.max(1,width))
            .setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(false)
            .setBreakStrategy(android.graphics.text.LineBreaker.BREAK_STRATEGY_SIMPLE).setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            .setLineSpacing(0,1.06f).setMaxLines(maxLines).setEllipsize(TextUtils.TruncateAt.END).build();
    }
    public static Bitmap render(String text,SurfaceStyle style,int width,int height,float maxSize){
        Bitmap b=Bitmap.createBitmap(Math.max(1,width),Math.max(1,height),Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(b);TextPaint p=new TextPaint(Paint.ANTI_ALIAS_FLAG);p.setTypeface(style.typeface());
        int pad=Math.max(4,Math.round(maxSize*.12f));
        StaticLayout l=fit(text,p,width-2*pad,height-2*pad,maxSize);
        c.translate(pad,Math.max(pad,(height-l.getHeight())/2f));draw(c,l,p,style);
        return b;
    }
    static StaticLayout fit(String text,TextPaint p,int width,int height,float maxSize){
        float size=maxSize;
        StaticLayout l;
        do {p.setTextSize(size);l=layout(text,p,width,100);size-=1;}while(l.getHeight()>height&&size>maxSize*.48f);
        // A small reduction can bring a lone trailing word onto the previous
        // line. Prefer that over a needlessly tall subtitle block.
        if(l.getLineCount()>1&&l.getHeight()<=height&&l.getLineWidth(l.getLineCount()-1)<width*.4f){
            float fitted=p.getTextSize();int originalLines=l.getLineCount();boolean improved=false;
            for(float smaller=fitted-1;smaller>=Math.max(maxSize*.48f,fitted*.85f);smaller--){
                p.setTextSize(smaller);StaticLayout candidate=layout(text,p,width,100);
                if(candidate.getLineCount()<originalLines&&candidate.getHeight()<=height){l=candidate;improved=true;break;}
            }
            if(!improved)p.setTextSize(fitted);
        }
        int lines=l.getLineCount();
        while(lines>1&&l.getLineBottom(lines-1)>height)lines--;
        if(lines<l.getLineCount())l=layout(text,p,width,lines);
        return l;
    }
    public static void draw(Canvas c,StaticLayout layout,TextPaint p,SurfaceStyle style){
        if(style.subtitle()){
            p.setStyle(Paint.Style.STROKE);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeWidth(Math.max(2,p.getTextSize()*.075f));p.setColor(Color.BLACK);layout.draw(c);
        }
        p.setStyle(Paint.Style.FILL);p.setColor(style.text());layout.draw(c);
    }
}

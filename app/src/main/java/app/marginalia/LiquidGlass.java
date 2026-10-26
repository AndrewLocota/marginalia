package app.marginalia;

import android.graphics.Bitmap;
import android.graphics.Color;

/** Native adaptation of the rounded-rectangle distance and exponential edge/rim
 * falloffs in dashersw/liquid-glass-js/container.js (MIT, Armagan Amcalar 2025).
 * Only the optical rim/tint is drawn here: RemoteViews cannot sample or refract
 * the launcher's wallpaper. See assets/liquid-glass-license.txt. */
public final class LiquidGlass {
    private LiquidGlass(){}
    static float distance(float x,float y,float width,float height,float radius){
        float qx=Math.abs(x-width*.5f)-(width*.5f-radius);
        float qy=Math.abs(y-height*.5f)-(height*.5f-radius);
        return (float)Math.hypot(Math.max(qx,0),Math.max(qy,0))+Math.min(Math.max(qx,qy),0)-radius;
    }
    static Bitmap render(int widthDp,int heightDp,float density){
        float scale=Math.min(density,(float)Math.sqrt(140_000f/Math.max(1f,(float)widthDp*heightDp)));
        int w=Math.max(1,Math.round(widthDp*scale)),h=Math.max(1,Math.round(heightDp*scale));
        Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);int[] pixels=new int[w*h];
        float radius=Math.min(26,Math.min(widthDp,heightDp)*.5f);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            float px=(x+.5f)/scale,py=(y+.5f)/scale;
            float d=distance(px,py,widthDp,heightDp,radius);
            float coverage=Math.max(0,Math.min(1,.5f-d*scale));
            if(coverage==0)continue;
            float inside=Math.max(0,-d);
            float edge=(float)Math.exp(-inside*.18f),rim=(float)Math.exp(-inside*1.25f);
            float corner=(float)Math.exp(-Math.max(Math.min(px,widthDp-px),Math.min(py,heightDp-py))*.3f);
            float vertical=py/Math.max(1,heightDp);
            float sheen=(1-vertical)*.07f;
            // Preserve transparency: no captured wallpaper or fake backdrop is baked in.
            float light=Math.min(.62f,edge*.12f+rim*.46f+corner*.10f+sheen);
            float tint=.18f;float alpha=(tint+light*(1-tint))*coverage;
            float base=34f+12f*(1-vertical),highlight=255f*(1-.3f*vertical);
            float rgb=(base*tint*(1-light)+highlight*light)/Math.max(.001f,tint+light*(1-tint));
            int channel=Math.max(0,Math.min(255,Math.round(rgb)));
            pixels[y*w+x]=Color.argb(Math.round(255*alpha),channel,Math.min(255,channel+3),Math.min(255,channel+7));
        }
        bitmap.setPixels(pixels,0,w,0,0,w,h);return bitmap;
    }
}

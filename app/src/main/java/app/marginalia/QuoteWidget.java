package app.marginalia;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.*;
import android.os.Bundle;
import android.os.Build;
import android.util.TypedValue;
import android.util.SizeF;
import android.widget.RemoteViews;
import android.view.View;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.TypefaceSpan;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;

public class QuoteWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids) {
        Repository r=new Repository(c);
        for(int id:ids)r.prefs.edit().remove("widget_render_"+id).apply();
        refresh(c);
    }
    @Override public void onEnabled(Context c) { refresh(c); }
    @Override public void onDisabled(Context c) { Sync.schedule(c); }
    @Override public void onDeleted(Context c,int[] ids){Repository r=new Repository(c);for(int id:ids)r.prefs.edit().remove("widget_render_"+id).remove("widget_recoil_"+id).apply();}
    @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle options){refresh(c);}
    private void refresh(Context c) {
        PendingResult result=goAsync(); Sync.refresh(c,()->result.finish());
    }
    static void updateAll(Context c,Quote q) { updateAll(c,q,AppWidgetManager.INVALID_APPWIDGET_ID,-1); }
    private static void updateAll(Context c,Quote q,int recoilId,int recoilStep) {
        Repository repo=new Repository(c); Palette p=new Palette(repo.dark());
        AppWidgetManager manager=AppWidgetManager.getInstance(c);
        for(Class<?> provider:new Class<?>[]{QuoteWidget.class,LockQuoteWidget.class}) {
        int[] ids=manager.getAppWidgetIds(new ComponentName(c,provider));
        for(int id:ids) {
            if(recoilId!=AppWidgetManager.INVALID_APPWIDGET_ID&&id!=recoilId)continue;
            Bundle options=manager.getAppWidgetOptions(id);
            int w=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,250);
            int h=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,180);
            SurfaceStyle style=SurfaceStyle.read(repo,surfaceFor(options,provider==LockQuoteWidget.class));
            ArrayList<SizeF> hostSizes=Build.VERSION.SDK_INT>=31?options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES):null;
            String key="v13:"+q.id+":"+style.key()+":"+w+":"+h+":"+hostSizes+":"+repo.prefs.getBoolean("widget_burst",true)+":"+c.getResources().getConfiguration().fontScale;
            if(recoilStep<0&&key.equals(repo.prefs.getString("widget_render_"+id,"")))continue;
            RemoteViews view;
            if(Build.VERSION.SDK_INT>=31){
                Map<SizeF,RemoteViews> sizes=new LinkedHashMap<>();
                int[][] dimensions={{180,90,R.layout.quote_widget_compact},{250,160,R.layout.quote_widget},{320,110,R.layout.quote_widget_compact},{320,180,R.layout.quote_widget},{250,260,R.layout.quote_widget_tall},{320,260,R.layout.quote_widget_tall},{400,260,R.layout.quote_widget_tall}};
                if(hostSizes!=null&&!hostSizes.isEmpty()){
                    // Render at the launcher's actual dimensions: a nearby preset
                    // shrinks a subtitle bitmap inside a wider/taller ImageView.
                    for(SizeF size:hostSizes){
                        if(sizes.size()>=8)break;
                        int sw=Math.max(1,Math.round(size.getWidth())),sh=Math.max(1,Math.round(size.getHeight()));
                        sizes.put(size,makeSized(c,q,style,sh>=260?R.layout.quote_widget_tall:sh>=160?R.layout.quote_widget:R.layout.quote_widget_compact,sw,sh,id,recoilStep));
                    }
                }else for(int[] size:dimensions)sizes.put(new SizeF(size[0],size[1]),makeSized(c,q,style,size[2],size[0],size[1],id,recoilStep));
                view=new RemoteViews(sizes);
            }else view=makeSized(c,q,style,h>=260?R.layout.quote_widget_tall:h>=160?R.layout.quote_widget:R.layout.quote_widget_compact,Math.max(180,w),Math.max(90,h),id,recoilStep);
            manager.updateAppWidget(id,view);
            repo.prefs.edit().putString("widget_render_"+id,key).putInt("widget_recoil_"+id,Math.max(0,recoilStep)).apply();
        }
    }
    }
    static boolean hasWidgets(Context c){
        AppWidgetManager manager=AppWidgetManager.getInstance(c);
        return manager.getAppWidgetIds(new ComponentName(c,QuoteWidget.class)).length>0||manager.getAppWidgetIds(new ComponentName(c,LockQuoteWidget.class)).length>0;
    }
    static String surfaceFor(Bundle options,boolean lockProvider){return lockProvider||(options.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,1)&AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD)!=0?"lock":"widget";}
    static RemoteViews make(Context c,Quote q,Repository repo,Palette p,int layout){
            return makeStyled(c,q,SurfaceStyle.read(repo,"widget"),layout);
    }
    static RemoteViews makeStyled(Context c,Quote q,SurfaceStyle style,int layout){
            return makeSized(c,q,style,layout,layout==R.layout.quote_widget_compact?180:320,layout==R.layout.quote_widget_compact?110:layout==R.layout.quote_widget_tall?260:180);
    }
    static RemoteViews makeSized(Context c,Quote q,SurfaceStyle style,int layout,int availableWidth,int availableHeight){
        return makeSized(c,q,style,layout,availableWidth,availableHeight,AppWidgetManager.INVALID_APPWIDGET_ID);
    }
    static RemoteViews makeSized(Context c,Quote q,SurfaceStyle style,int layout,int availableWidth,int availableHeight,int widgetId){
        return makeSized(c,q,style,layout,availableWidth,availableHeight,widgetId,-1);
    }
    private static RemoteViews makeSized(Context c,Quote q,SurfaceStyle style,int layout,int availableWidth,int availableHeight,int widgetId,int recoilStep){
            Palette p=new Palette(style.dark());
            RemoteViews view=new RemoteViews(c.getPackageName(),layout);
            int baseInset=layout==R.layout.quote_widget_compact?12:layout==R.layout.quote_widget_tall?24:20;
            int basePixels=Math.round(baseInset*c.getResources().getDisplayMetrics().density);
            view.setViewPadding(R.id.widget_content,basePixels,basePixels,basePixels,basePixels);
            view.setViewVisibility(R.id.widget_quote,View.VISIBLE);
            view.setViewVisibility(R.id.widget_art,View.GONE);
            view.setViewVisibility(R.id.widget_label,layout==R.layout.quote_widget_compact?View.GONE:View.VISIBLE);
            view.setInt(R.id.widget_author,"setMinHeight",0);
            view.setInt(R.id.widget_author,"setMaxHeight",Integer.MAX_VALUE);
            view.setViewVisibility(R.id.widget_glass_layer,style.liquid()?View.VISIBLE:View.GONE);
            if(style.liquid()){
                view.setInt(R.id.widget_root,"setBackgroundColor",android.graphics.Color.TRANSPARENT);
                android.util.DisplayMetrics dm=c.getResources().getDisplayMetrics();
                float scale=Math.min(dm.density,(float)Math.sqrt((dm.widthPixels*(float)dm.heightPixels*.05f)/Math.max(1f,availableWidth*(float)availableHeight)));
                view.setImageViewBitmap(R.id.widget_glass_layer,LiquidGlass.render(availableWidth,availableHeight,scale));
            }else view.setInt(R.id.widget_root,"setBackgroundResource",style.glass()?R.drawable.widget_glass:style.dark()?R.drawable.widget_dark:R.drawable.widget_paper);
            SpannableString passage=new SpannableString(q.text);
            passage.setSpan(new TypefaceSpan(style.font.equals("serif")?"serif":"sans-serif"),0,passage.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            view.setTextViewText(R.id.widget_quote,passage);
            float fontScale=c.getResources().getConfiguration().fontScale;
            int lines=layout==R.layout.quote_widget_compact?3:layout==R.layout.quote_widget_tall?8:5;
            view.setInt(R.id.widget_quote,"setMaxLines",Math.max(1,(int)(lines/Math.max(1,fontScale))));
            view.setTextViewText(R.id.widget_author,"— "+q.shortAuthor()+"  ↗");
            view.setTextColor(R.id.widget_quote,style.text()); view.setTextColor(R.id.widget_author,style.glass()?0xFFF1ECE5:p.accent);view.setTextColor(R.id.widget_label,style.glass()?0xFFE4E0DA:p.muted);
            if(style.subtitle()){
                boolean compact=layout==R.layout.quote_widget_compact,tall=layout==R.layout.quote_widget_tall;
                // Bounded raster work at update time; launcher motion only composites the result.
                float density=Math.min(2.5f,c.getResources().getDisplayMetrics().density);
                int inset=compact?10:14;
                int pxInset=Math.round(inset*c.getResources().getDisplayMetrics().density);
                view.setViewPadding(R.id.widget_content,pxInset,pxInset,pxInset,pxInset);
                view.setViewVisibility(R.id.widget_label,View.GONE);
                // Reserve a predictable attribution row, including the fallback
                // font used by the source-link arrow and accessibility scaling.
                int authorPixels=Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,tall?20:18,c.getResources().getDisplayMetrics()));
                view.setInt(R.id.widget_author,"setHeight",authorPixels);
                float authorHeight=authorPixels/c.getResources().getDisplayMetrics().density;
                float contentWidth=Math.max(1,availableWidth-2*inset);
                float contentHeight=Math.max(1,availableHeight-2*inset-authorHeight-(compact?4:0));
                // Cap resolution with a common scale so very large widgets retain
                // their aspect ratio instead of squeezing text into the centre.
                float rasterScale=Math.min(density,Math.min(1400/contentWidth,1000/contentHeight));
                float bitmapBudget=Math.min(260_000f,c.getResources().getDisplayMetrics().widthPixels*(float)c.getResources().getDisplayMetrics().heightPixels*.09f);
                rasterScale=Math.min(rasterScale,(float)Math.sqrt(bitmapBudget/(contentWidth*contentHeight)));
                int width=Math.max(1,Math.round(contentWidth*rasterScale));
                int height=Math.max(1,Math.round(contentHeight*rasterScale));
                view.setViewVisibility(R.id.widget_quote,View.GONE);view.setViewVisibility(R.id.widget_art,View.VISIBLE);
                float maxSp=Math.max(16,Math.min(22,availableWidth/18f));
                view.setImageViewBitmap(R.id.widget_art,QuoteArt.render(q.text,style,width,height,maxSp*rasterScale*Math.min(1.5f,fontScale)));
            }
            boolean burst=new Repository(c).prefs.getBoolean("widget_burst",true);
            String description=q.text+" — "+q.author+(burst?". Tap for an author emoji burst. Open Marginalia for the full passage.":". Tap to read the full passage.");
            view.setContentDescription(R.id.widget_root,description);
            Intent open=new Intent(c,burst?BurstActivity.class:MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_NO_ANIMATION);
            open.setData(android.net.Uri.parse("marginalia://widget/upward-v13/"+widgetId+"/"+q.id));
            open.putExtra("quote_id",q.id).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId);
            // Explicit internal activity. Mutable only so the host can fill the
            // clicked view's source bounds; it cannot replace this component.
            int flags=PendingIntent.FLAG_UPDATE_CURRENT|(burst&&Build.VERSION.SDK_INT>=31?PendingIntent.FLAG_MUTABLE:burst?0:PendingIntent.FLAG_IMMUTABLE);
            view.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getActivity(c,widgetId,open,flags));
            view.setOnClickPendingIntent(R.id.widget_author,QuoteCard.source(c,q));
            view.setContentDescription(R.id.widget_author,"Read source: "+q.author+", "+q.citation());
            RemoteViews shell=new RemoteViews(c.getPackageName(),R.layout.quote_widget_shell);
            shell.removeAllViews(R.id.widget_flipper);
            if(Build.VERSION.SDK_INT>=31){
                shell.addStableView(R.id.widget_flipper,view,0);shell.addStableView(R.id.widget_flipper,view,1);
            }else{shell.addView(R.id.widget_flipper,view);shell.addView(R.id.widget_flipper,view);}
            if(recoilStep>=0)shell.setInt(R.id.widget_flipper,"setDisplayedChild",recoilStep);
            shell.setContentDescription(R.id.widget_flipper,description);
            return shell;
    }
    static void recoil(Context c,int widgetId){
        AppWidgetManager manager=AppWidgetManager.getInstance(c);
        android.appwidget.AppWidgetProviderInfo info=manager.getAppWidgetInfo(widgetId);
        if(info==null||!c.getPackageName().equals(info.provider.getPackageName()))return;
        Repository r=new Repository(c);int next=1-r.prefs.getInt("widget_recoil_"+widgetId,0);
        // Responsive variants need the action in each layout. Stable child IDs
        // let the launcher reuse the two faces instead of removing/reinflating them.
        String rendered=r.prefs.getString("widget_render_"+widgetId,"");
        Quote displayed=r.current();for(Quote q:r.all())if(rendered.startsWith("v13:"+q.id+":")){displayed=q;break;}
        updateAll(c,displayed,widgetId,next);
    }
}

package app.marginalia;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.ImageView;
import java.util.*;

/** Dependency-free, on-device integration checks. Run only on a test emulator. */
public final class SmokeTests extends Instrumentation {
    private int checks=0;
    private void check(boolean value,String message){if(!value)throw new AssertionError(message);checks++;}
    private android.service.notification.StatusBarNotification awaitCard(NotificationManager manager,String previous)throws Exception{
        long until=android.os.SystemClock.uptimeMillis()+4000;
        do{for(android.service.notification.StatusBarNotification card:manager.getActiveNotifications())if(card.getId()==QuoteCard.ID&&(previous==null||!previous.equals(card.getNotification().extras.getString("quote_id"))))return card;Thread.sleep(40);}while(android.os.SystemClock.uptimeMillis()<until);
        throw new AssertionError("Expected quotation notification did not arrive");
    }
    private void awaitGone(NotificationManager manager)throws Exception{
        long until=android.os.SystemClock.uptimeMillis()+4000;
        do{boolean found=false;for(android.service.notification.StatusBarNotification card:manager.getActiveNotifications())if(card.getId()==QuoteCard.ID)found=true;if(!found)return;Thread.sleep(40);}while(android.os.SystemClock.uptimeMillis()<until);
        throw new AssertionError("Quotation notification did not clear");
    }
    @Override public void onCreate(Bundle b){super.onCreate(b);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try {
            Context c=getTargetContext();Repository r=new Repository(c);r.prefs.edit().clear().commit();r=new Repository(c);
            check(r.all().size()>1300,"Large offline library");
            Set<String> ids=new HashSet<>(),texts=new HashSet<>();
            for(Quote q:r.all()){check(ids.add(q.id),"Unique id "+q.id);check(texts.add(q.text),"Unique quote");check(!q.translator.isEmpty()&&q.url.startsWith("https://"),"Provenance");}
            check(r.current().id.equals("nietzsche-108"),"Conceptual maxim is the first thought");
            for(String author:r.authors()){check(AuthorEmoji.hasAuthor(c,author),"Author-specific motif: "+author);check(AuthorEmoji.forAuthor(c,author).length>=3,"At least three emoji motifs for "+author);}
            check(Arrays.equals(AuthorEmoji.forAuthor(c,"Friedrich Nietzsche"),new String[]{"🐪","🦁","🧒"}),"Nietzsche uses the three metamorphoses");
            check(BurstMotion.DELAY_MS>BurstMotion.RECOIL_MS,"Particles follow recoil");
            for(int seed=0;seed<20;seed++)for(BurstMotion.Particle particle:BurstMotion.plan(40,100,360,280,400,800,1,new Random(seed))){
                check(particle.y>280&&particle.x>=40&&particle.x<=360,"Bubble originates along bottom edge");
                check(particle.dy<0,"Every bubble rises");
                check(particle.delay>=0&&particle.delay+particle.life<=1,"Staggered bubble ends within animation");
                check(particle.alpha(0)==0&&particle.alpha(1)==0,"No hard appearance or disappearance");
                for(int frame=0;frame<=20;frame++){
                    float t=frame/20f;
                    check(particle.xAt(t)>=particle.margin&&particle.xAt(t)<=400-particle.margin&&particle.yAt(t)>=particle.margin&&particle.yAt(t)<=800-particle.margin,"Whole path stays within safe screen area");
                    if(frame>0)check(particle.yAt(t)<particle.yAt((frame-1)/20f),"Rise is continuous and upward");
                }
            }
            BurstMotion.Particle[] bubblesA=BurstMotion.plan(40,100,360,280,400,800,1,new Random(1));
            BurstMotion.Particle[] bubblesB=BurstMotion.plan(40,100,360,280,400,800,1,new Random(2));
            check(bubblesA[0].x!=bubblesB[0].x&&bubblesA[0].size!=bubblesB[0].size&&bubblesA[0].delay!=bubblesB[0].delay,"Each tap varies placement, size and timing");
            for(BurstMotion.Particle particle:BurstMotion.plan(0,600,400,800,400,800,1,new Random(4)))
                check(particle.y<=780&&particle.dy<0,"Bottom-of-screen widget bubbles rise inside safe area");
            check(r.authors().size()==95&&r.selectedCount()>=316,"Expanded curated collection");
            for(String author:r.authors()){
                int selected=0;
                for(Quote q:r.all())if(q.author.equals(author)&&q.selected)selected++;
                check(selected>=3,"At least three selected passages for "+author);
            }
            for(Quote q:r.all())if(q.id.startsWith("minimum-"))check(q.selected&&!q.idea.isEmpty()&&!q.reference.isEmpty(),"Verified expansion has provenance and an explanatory note: "+q.id);
            org.json.JSONArray required;
            try(java.io.InputStream in=c.getAssets().open("requested-authors.json")){required=new org.json.JSONArray(MainActivity.readStream(in));}
            check(required.length()==54,"All expansion requests are tracked");
            for(int i=0;i<required.length();i++){
                String name=required.getString(i);Quote found=null;
                for(Quote q:r.all())if(q.author.equals(name)&&q.selected&&!q.idea.isEmpty()){found=q;break;}
                check(found!=null,"Conceptual passage and reading note for "+name);
                check(!found.reference.isEmpty()&&!found.translator.isEmpty(),"Specific source for "+name);
                check(MainActivity.detailText(found).contains("READING NOTE · MARGINALIA\n"+found.idea)&&MainActivity.detailText(found).startsWith(found.text),"Reading note separated from quotation for "+name);
            }
            check(r.selectedOnly(),"Selected maxims are the default rotation");
            Set<String> featuredAuthors=new HashSet<>();
            for(Quote q:r.deck()){check(q.selected&&!q.theme.isEmpty(),"Only selected and themed maxims in default rotation");featuredAuthors.add(q.author);}
            check(featuredAuthors.equals(new HashSet<>(r.authors())),"Every thinker represented in selected mode");
            check(MainActivity.searchKey("Slavoj Žižek").contains("zizek")&&MainActivity.searchKey("François").equals("francois"),"Search works without diacritics");
            Quote selectedFirst=r.current();r.setSelectedOnly(false);
            check(r.deck().size()==r.all().size()&&r.current().id.equals(selectedFirst.id),"Full anthology includes everything and preserves current maxim");
            Quote archive=null;for(Quote q:r.all())if(!q.selected){archive=q;break;}
            r.use(archive);r.toggleSaved(archive);r.setSelectedOnly(true);
            check(r.current().selected&&r.isSaved(archive),"Selected mode preserves saved anthology passages");
            r.use(archive);check(!r.selectedOnly()&&r.current().id.equals(archive.id),"Explicitly choosing an anthology passage makes it current");
            r.toggleSaved(archive);r.setSelectedOnly(true);r.use(selectedFirst);
            Set<String> oldAll=new HashSet<>(Arrays.asList("Johann Wolfgang von Goethe","Friedrich Nietzsche","François de La Rochefoucauld","Epictetus","Marcus Aurelius","Epicurus"));
            r.prefs.edit().putStringSet("authors",oldAll).remove("maxims_edition").commit();new Repository(c);
            check(r.enabledAuthors().size()==95,"Upgrade expands the former all-thinkers selection");
            r.prefs.edit().putStringSet("authors",new HashSet<>(Collections.singletonList("Epictetus"))).remove("maxims_edition").commit();new Repository(c);
            check(r.enabledAuthors().equals(Collections.singleton("Epictetus")),"Upgrade retains a deliberately narrowed author selection");
            r.prefs.edit().remove("authors").commit();
            Set<String> previousAll=new HashSet<>();
            try(java.io.InputStream in=c.getAssets().open("previous-authors.json")){org.json.JSONArray a=new org.json.JSONArray(MainActivity.readStream(in));for(int i=0;i<a.length();i++)previousAll.add(a.getString(i));}
            r.prefs.edit().putStringSet("authors",previousAll).remove("concepts_edition").putBoolean("dark",true).putInt("hours",12).putStringSet("saved",Collections.singleton(selectedFirst.id)).putString("widget_background","glass").commit();new Repository(c);
            check(previousAll.size()==43&&r.enabledAuthors().size()==95,"Version 1.2 all-authors selection expands to 95");
            check(r.dark()&&r.hours()==12&&r.isSaved(selectedFirst)&&r.current().id.equals(selectedFirst.id)&&SurfaceStyle.read(r,"widget").background.equals("glass"),"Concept edition preserves saved thought, current thought, cadence and styling");
            r.prefs.edit().putStringSet("authors",Collections.singleton("Epictetus")).remove("concepts_edition").commit();new Repository(c);
            check(r.enabledAuthors().equals(Collections.singleton("Epictetus")),"Concept edition preserves a narrowed selection");
            r.prefs.edit().remove("authors").remove("saved").remove("widget_background").remove("dark").remove("hours").commit();
            check(SurfaceStyle.read(r,"widget").font.equals("serif")&&SurfaceStyle.read(r,"lock").font.equals("serif"),"Literary serif is the default on both screens");
            check(Rotation.index(999,0,12,2,10)==2,"Stable before boundary");
            check(Rotation.index(43_200_000,0,12,2,10)==3,"12 hour rollover");
            check(Rotation.index(86_400_000,0,24,9,10)==0,"Daily wraparound");
            check(Rotation.index(86_400_000L*5,0,24,2,10)==7,"Missed days catch up");
            check(Rotation.index(-10,0,24,2,10)==2,"Clock before anchor");
            check(Rotation.next(43_200_000,0,12)==86_400_000,"Future next boundary");
            Quote first=r.current();r.toggleSaved(first);check(new Repository(c).isSaved(first),"Favourite persisted");r.toggleSaved(first);check(!r.isSaved(first),"Favourite removed");
            r.setHours(12);check(r.hours()==12&&r.current().id.equals(first.id),"Cadence preserves thought");
            r.next();check(!r.current().id.equals(first.id),"Next changes thought");
            r.setAuthors(new HashSet<>(Collections.singletonList("Epictetus")));check(r.current().author.equals("Epictetus"),"Author filter");
            HashSet<String> rotation=new HashSet<>();for(int i=0;i<r.deck().size();i++){check(rotation.add(r.current().id),"No repeat in cycle");r.next();}
            r.use(first);check(r.current().id.equals(first.id),"Select library quote and enable author");
            r.prefs.edit().putBoolean("dark",true).commit();check(new Repository(c).dark(),"Theme persisted");
            // 1.99: balance actual exposure, preserve state, bound storage and validate backups.
            String before199=LocalBackup.exportData(r);
            r.setAuthors(new HashSet<>(r.authors()));r.setBalanced(true);
            Quote balancedFirst=r.current();long balancedAnchor=r.prefs.getLong("anchor",0);
            Map<String,Integer> exposures=new HashMap<>();Map<String,Set<String>> authorPassages=new HashMap<>();
            for(int i=0;i<285;i++){
                Quote q=r.current();exposures.put(q.author,exposures.getOrDefault(q.author,0)+1);
                authorPassages.computeIfAbsent(q.author,k->new HashSet<>()).add(q.id);
                if(i==1)check(q.id.equals(r.currentAt(System.currentTimeMillis()).id),"Balanced quote stable within slot");
                r.next();
            }
            check(exposures.size()==95,"Balanced mode covers all thinkers");
            for(String name:exposures.keySet()){
                check(exposures.get(name)==3,"Equal exposure for "+name);
                check(authorPassages.get(name).size()==3,"Advances each author's own passages: "+name);
            }
            r.use(balancedFirst);check(r.current().id.equals(balancedFirst.id),"Explicit balanced choice preserved");
            check(!r.currentAt(r.prefs.getLong("anchor",0)+r.hours()*3_600_000L).id.equals(balancedFirst.id),"Balanced timed rollover");
            check(new Repository(c).current().id.equals(balancedFirst.id),"Balanced position survives repository recreation");
            r.stay();long heldUntil=r.prefs.getLong("hold_until",0);
            check(r.staying()&&r.currentAt(heldUntil-1).id.equals(balancedFirst.id),"Stay keeps passage for 24 hours");
            check(!r.currentAt(heldUntil).id.equals(balancedFirst.id),"Stay expires exactly at next boundary");
            check(r.nextAt()==heldUntil,"Stay schedules its expiry");r.next();check(!r.staying(),"Next cancels stay");
            r.setShortPassages(true);for(Quote q:r.deck())check(q.text.length()<=180,"Short text filter");
            check(r.current().text.length()<=180,"Filtered current quote fits short preference");
            Quote longChoice=Collections.max(r.all(),Comparator.comparingInt(q->q.text.length()));r.use(longChoice);
            check(!r.shortPassages()&&r.current().id.equals(longChoice.id),"Explicit long quote exits short mode");
            for(int i=0;i<70;i++)r.remember(r.all().get(i));
            check(r.recent().size()==50&&r.recent().get(0).id.equals(r.all().get(69).id),"History bounded to 50, newest first");
            r.remember(r.all().get(69));check(r.recent().size()==50,"History has no consecutive duplicates");
            r.toggleSaved(balancedFirst);r.remember(r.current());String backup=LocalBackup.exportData(r);Quote backedUp=r.current();
            r.setBalanced(false);r.prefs.edit().putBoolean("dark",!r.dark()).commit();LocalBackup.restore(r,backup);
            check(r.balanced()&&r.current().id.equals(backedUp.id),"Backup restores mode and current thought");
            check(LocalBackup.exportData(r).equals(backup),"Backup round trip retains user preferences and history");
            String validBefore=LocalBackup.exportData(r);boolean rejected=false;
            try{LocalBackup.restore(r,"{\"format\":\"marginalia-backup\",\"version\":1,\"settings\":{\"dark\":true,\"hours\":3}}");}catch(org.json.JSONException expected){rejected=true;}
            check(rejected&&LocalBackup.exportData(r).equals(validBefore),"Malformed backup cannot partially change settings");
            check(BuildInfo.label(c).contains("build 13"),"Installed build number is visible");
            check(StorageReport.apkBytes(c)<5_000_000,"Installed APK remains under 5 MB");
            check(StorageReport.read(c).contains("Android app size:"),"Own-package storage totals available without additional permission");
            LocalBackup.restore(r,before199);
            Quote longest=Collections.max(r.all(),Comparator.comparingInt(q->q.text.length()));
            Repository renderRepo=r;
            runOnMainSync(()->{
                int[][] sizes={{180,90,R.layout.quote_widget_compact},{320,110,R.layout.quote_widget_compact},{250,160,R.layout.quote_widget},{250,180,R.layout.quote_widget},{320,260,R.layout.quote_widget_tall},{360,350,R.layout.quote_widget_tall}};
                float density=c.getResources().getDisplayMetrics().density;
                for(int[] size:sizes){
                    View view=QuoteWidget.make(c,longest,renderRepo,new Palette(renderRepo.dark()),size[2]).apply(c,new FrameLayout(c));
                    int w=Math.round(size[0]*density),h=Math.round(size[1]*density);
                    view.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));view.layout(0,0,w,h);
                    TextView quote=view.findViewById(R.id.widget_quote),author=view.findViewById(R.id.widget_author);
                    check(quote.getText().toString().equals(longest.text),"Widget source preserved at "+size[0]);
                    check(quote.getBottom()<=author.getTop(),"Quote does not overlap attribution");
                    check(author.getBottom()<=h,"Attribution stays inside widget");
                    check(quote.getLayout()!=null&&quote.getHeight()>0,"Measured text layout");
                    check(quote.getLayout().getHeight()<=quote.getHeight()+1,"Text fits visible widget height at "+size[0]+"x"+size[1]+" id="+longest.id+" textHeight="+quote.getLayout().getHeight()+" available="+quote.getHeight()+" authorHeight="+author.getHeight()+" font="+quote.getTextSize());
                    check(view.getContentDescription().toString().contains(longest.text),"Full accessible quotation");
                }
                for(String background:new String[]{"paper","ink","glass","liquid"})for(String font:new String[]{"serif","sans","subtitle"}){
                    SurfaceStyle style=new SurfaceStyle(background,font,"auto");
                    View view=QuoteWidget.makeStyled(c,longest,style,R.layout.quote_widget).apply(c,new FrameLayout(c));
                    view.measure(View.MeasureSpec.makeMeasureSpec(Math.round(300*density),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(Math.round(200*density),View.MeasureSpec.EXACTLY));
                    view.layout(0,0,Math.round(300*density),Math.round(200*density));
                    ImageView art=view.findViewById(R.id.widget_art);
                    check((art.getVisibility()==View.VISIBLE)==font.equals("subtitle"),"Outlined renderer selected");
                    if(font.equals("subtitle"))check(art.getDrawable()!=null,"Pre-rendered subtitle bitmap");
                }
            });
            runOnMainSync(()->{
                SurfaceStyle liquid=new SurfaceStyle("liquid","subtitle","auto");
                float density=c.getResources().getDisplayMetrics().density;
                for(int[] size:new int[][]{{180,90},{360,110},{460,180},{320,300}}){
                    int layout=size[1]>=260?R.layout.quote_widget_tall:size[1]>=160?R.layout.quote_widget:R.layout.quote_widget_compact;
                    View view=QuoteWidget.makeSized(c,first,liquid,layout,size[0],size[1]).apply(c,new FrameLayout(c));
                    int width=Math.round(size[0]*density),height=Math.round(size[1]*density);
                    view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));view.layout(0,0,width,height);
                    ImageView art=view.findViewById(R.id.widget_art);
                    Bitmap pixels=((android.graphics.drawable.BitmapDrawable)art.getDrawable()).getBitmap();
                    float imageRatio=(float)pixels.getWidth()/pixels.getHeight(),slotRatio=(float)art.getWidth()/art.getHeight();
                    check(Math.abs(imageRatio/slotRatio-1)<.04f,"Subtitle raster matches actual slot at "+size[0]+"x"+size[1]+" bitmap="+pixels.getWidth()+"x"+pixels.getHeight()+" slot="+art.getWidth()+"x"+art.getHeight());
                    check(view.findViewById(R.id.widget_label).getVisibility()==View.GONE,"Subtitle surface gives heading space to quote");
                    check(art.getWidth()>=width-28*density-1,"Subtitles use the available widget width");
                    check(pixels.getAllocationByteCount()<=1_050_000,"Subtitle bitmap stays within per-size memory budget");
                    Bitmap preview=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
                    android.graphics.Canvas canvas=new android.graphics.Canvas(preview);android.graphics.Paint backdrop=new android.graphics.Paint();
                    backdrop.setShader(new android.graphics.LinearGradient(0,0,width,height,new int[]{0xFF9F9278,0xFF354E57,0xFF122630},null,android.graphics.Shader.TileMode.CLAMP));canvas.drawRect(0,0,width,height,backdrop);view.draw(canvas);
                    try(java.io.FileOutputStream file=new java.io.FileOutputStream(new java.io.File(c.getExternalFilesDir(null),"liquid-subtitles-"+size[0]+"x"+size[1]+".png"))){preview.compress(Bitmap.CompressFormat.PNG,100,file);}catch(java.io.IOException e){throw new RuntimeException(e);}preview.recycle();
                    check(((android.widget.ViewFlipper)view).getChildCount()==2,"Recoil shell holds exactly two widget faces");
                    QuoteWidget.makeSized(c,first,new SurfaceStyle("paper","serif","auto"),layout,size[0],size[1]).reapply(c,view);
                    check(((android.widget.ViewFlipper)view).getChildCount()==2,"RemoteViews reapply replaces old faces");
                    check(view.findViewById(R.id.widget_quote).getVisibility()==View.VISIBLE&&view.findViewById(R.id.widget_art).getVisibility()==View.GONE,"Switching away from subtitles restores native text");
                }
                android.text.TextPaint paint=new android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG);paint.setTypeface(liquid.typeface());
                String text="There is no such thing as moral phenomena, but only a moral interpretation of phenomena.";
                android.text.StaticLayout wide=QuoteArt.fit(text,paint,1100,120,40),narrow=QuoteArt.fit(text,paint,350,240,40);
                check(wide.getLineCount()<narrow.getLineCount(),"Subtitle line count responds to width rather than a fixed three lines");
                check(wide.getEllipsisCount(wide.getLineCount()-1)==0&&narrow.getEllipsisCount(narrow.getLineCount()-1)==0,"Responsive example preserves the complete quote");
                Bitmap material=LiquidGlass.render(320,180,1);
                check(android.graphics.Color.alpha(material.getPixel(0,0))==0,"Rounded glass corner is transparent");
                check(android.graphics.Color.alpha(material.getPixel(160,1))>android.graphics.Color.alpha(material.getPixel(160,90)),"Glass rim is stronger than centre");
                int alpha=android.graphics.Color.alpha(material.getPixel(160,90));check(alpha>0&&alpha<255,"Liquid material keeps the wallpaper visible");material.recycle();
            });
            SurfaceStyle subtitles=new SurfaceStyle("glass","subtitle","auto");
            Bitmap caption=QuoteArt.render("A thought to keep.",subtitles,600,200,55);int yellow=0,black=0;
            for(int y=0;y<caption.getHeight();y++)for(int x=0;x<caption.getWidth();x++){int color=caption.getPixel(x,y);if(color==0xFFFFE539)yellow++;if(color==0xFF000000)black++;}
            check(yellow>100&&black>100,"Yellow fill and real black outline rendered");caption.recycle();
            r.prefs.edit().putString("widget_background","glass").putString("widget_font","subtitle").commit();
            check(SurfaceStyle.read(r,"widget").glass()&&!SurfaceStyle.read(r,"lock").glass(),"Independent widget and lock-screen themes");
            AppWidgetManager manager=AppWidgetManager.getInstance(c);
            AppWidgetHost host=new AppWidgetHost(c,7492);int id=host.allocateAppWidgetId();
            boolean bound=manager.bindAppWidgetIdIfAllowed(id,new ComponentName(c,QuoteWidget.class));check(bound,"Widget bind permission supplied by test setup");
            Bundle dimensions=new Bundle();dimensions.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES,new ArrayList<>(Arrays.asList(new android.util.SizeF(360,110),new android.util.SizeF(460,180))));manager.updateAppWidgetOptions(id,dimensions);
            r.prefs.edit().putString("widget_background","liquid").commit();
            QuoteWidget.updateAll(c,first);check(manager.getAppWidgetInfo(id)!=null,"Widget provider bound");
            check(r.prefs.getString("widget_render_"+id,"").contains("360.0x110.0"),"Host dimensions participate in widget render invalidation");
            int lockWidget=host.allocateAppWidgetId();
            check(manager.bindAppWidgetIdIfAllowed(lockWidget,new ComponentName(c,LockQuoteWidget.class)),"Dedicated lock widget binds");
            Bundle keyguard=new Bundle();keyguard.putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD);
            check(QuoteWidget.surfaceFor(keyguard,false).equals("lock"),"Keyguard host selects lock style");
            check(QuoteWidget.surfaceFor(new Bundle(),true).equals("lock"),"Dedicated lock provider selects lock style without host metadata");
            check(QuoteWidget.surfaceFor(new Bundle(),false).equals("widget"),"Home host keeps home style");
            QuoteWidget.updateAll(c,first);
            check(r.prefs.getString("widget_render_"+lockWidget,"").contains(SurfaceStyle.read(r,"lock").key()),"Dedicated widget updated with lock style");
            String beforeRecoil=r.current().id;
            QuoteWidget.recoil(c,id);check(r.prefs.getInt("widget_recoil_"+id,0)==1,"First tap switches recoil face");
            QuoteWidget.recoil(c,id);check(r.prefs.getInt("widget_recoil_"+id,1)==0,"Second tap switches back");
            check(beforeRecoil.equals(r.current().id),"Recoil preserves the shared maxim");
            WallpaperManager wallpapers=WallpaperManager.getInstance(c);
            int home=wallpapers.getWallpaperId(WallpaperManager.FLAG_SYSTEM),lock=wallpapers.getWallpaperId(WallpaperManager.FLAG_LOCK);
            String[] permissions=c.getPackageManager().getPackageInfo(c.getPackageName(),android.content.pm.PackageManager.GET_PERMISSIONS).requestedPermissions;
            check(!Arrays.asList(permissions).contains("android.permission.SET_WALLPAPER"),"App cannot request wallpaper changes");
            r.prefs.edit().putBoolean("wallpaper",true).remove("wallpaper_retired").commit();new Repository(c);
            check(!r.prefs.getBoolean("wallpaper",true),"Upgrade retires old wallpaper rotation");
            check(QuoteCard.allowed(c),"Notification permission supplied by test setup");
            r.prefs.edit().putBoolean("quote_card",true).commit();Sync.updateNow(c);Sync.schedule(c);
            NotificationManager notifications=QuoteCard.manager(c);
            android.service.notification.StatusBarNotification card=awaitCard(notifications,null);
            Notification n=card.getNotification();
            check(r.prefs.getString("widget_render_"+id,"").startsWith("v13:"+n.extras.getString("quote_id")+":"),"Home widget and notification use identical current quote");
            check(r.prefs.getString("widget_render_"+lockWidget,"").startsWith("v13:"+n.extras.getString("quote_id")+":"),"Lock widget and notification use identical current quote");
            check(String.valueOf(n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)).equals(r.current().text),"Expanded card contains full quotation");
            check(n.contentView!=null,"Collapsed card provides a multiline quotation layout");
            Quote compactExample=null;for(Quote q:r.all())if(q.id.equals("nietzsche-108"))compactExample=q;
            final Quote compactQuote=compactExample;
            runOnMainSync(()->{
                View compact=QuoteCard.build(c,compactQuote).contentView.apply(c,new FrameLayout(c));
                TextView words=compact.findViewById(R.id.notification_quote);
                int width=Math.round(240*c.getResources().getDisplayMetrics().density),height=Math.round(48*c.getResources().getDisplayMetrics().density);
                compact.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));compact.layout(0,0,width,height);
                check(words.getText().toString().equals(QuoteCard.compactText(compactQuote)),"Collapsed card contains the complete quote and attribution");
                android.text.Layout lines=words.getLayout();
                check(lines.getLineCount()>1&&lines.getLineEnd(lines.getLineCount()-1)==QuoteCard.compactText(compactQuote).length(),"Example aphorism wraps fully within 240dp collapsed card");
                check(lines.getEllipsisCount(lines.getLineCount()-1)==0&&lines.getHeight()<=height,"Example is neither ellipsized nor vertically clipped");
            });
            check(n.visibility==Notification.VISIBILITY_PUBLIC,"Quote is eligible for public lock-screen display");
            check((n.flags&Notification.FLAG_ONGOING_EVENT)!=0&&n.actions.length==3,"Card has persistent flag, controls and source action");
            check(n.actions[2].title.toString().equals("Read source")&&n.extras.getString("source_url").equals(r.current().url),"Card links the displayed passage to its source");
            NotificationChannel channel=notifications.getNotificationChannel(QuoteCard.CHANNEL);
            check(channel.getSound()==null&&!channel.shouldVibrate(),"Card channel is silent");
            check(wallpapers.getWallpaperId(WallpaperManager.FLAG_SYSTEM)==home&&wallpapers.getWallpaperId(WallpaperManager.FLAG_LOCK)==lock,"Posting card preserves both wallpapers");
            long posted=card.getPostTime();Sync.updateNow(c);
            check(awaitCard(notifications,null).getPostTime()==posted,"Unchanged quote is not reposted");
            check(((android.app.job.JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE)).getPendingJob(7491)!=null,"Card keeps scheduled rotation active");
            String previous=n.extras.getString("quote_id");n.actions[0].actionIntent.send();
            card=awaitCard(notifications,previous);check(!card.getNotification().extras.getString("quote_id").equals(previous),"Next action advances the quotation");
            check(r.prefs.getString("widget_render_"+id,"").startsWith("v13:"+card.getNotification().extras.getString("quote_id")+":"),"Notification Next updates widget to same quote");
            notifications.cancel(QuoteCard.ID);awaitGone(notifications);
            new QuoteCardReceiver().onReceive(c,new Intent(QuoteCardReceiver.DISMISSED));Sync.updateNow(c);
            check(notifications.getActiveNotifications().length==0,"Dismissed quotation stays dismissed during same interval");
            r.prefs.edit().remove("card_dismissed_quote").putLong("anchor",System.currentTimeMillis()-43_200_005L).commit();Sync.updateNow(c);
            card=awaitCard(notifications,null);check(card.getNotification().extras.getString("quote_id").equals(r.current().id),"Card catches up after interval rollover");
            card.getNotification().actions[1].actionIntent.send();awaitGone(notifications);
            check(!r.prefs.getBoolean("quote_card",true),"Turn-off action disables card");
            check(wallpapers.getWallpaperId(WallpaperManager.FLAG_SYSTEM)==home&&wallpapers.getWallpaperId(WallpaperManager.FLAG_LOCK)==lock,"Actions and rollover preserve both wallpapers");
            host.deleteHost();
            Sync.schedule(c);
            boolean otherWidgets=QuoteWidget.hasWidgets(c);
            check((((android.app.job.JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE)).getPendingJob(7491)!=null)==otherWidgets,"Background recovery exists only while a screen is active");
            r.prefs.edit().clear().commit();new Repository(c);
            result.putString("stream","\nPASS: "+checks+" assertions, including rotation, corpus, persistence, native widget binding, notification actions and unchanged wallpapers.\n");
            finish(Activity.RESULT_OK,result);
        } catch(Throwable e){result.putString("stream","\nFAIL after "+checks+" checks: "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}
    }
}

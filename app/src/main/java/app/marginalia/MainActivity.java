package app.marginalia;

import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public final class MainActivity extends Activity {
    private Repository repo; private Palette p; private LinearLayout root,body,feed;
    private int tab=0,limit=40; private String author="All thinkers",query="",topic="All themes";
    private boolean browseSelected=true;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private String renderedId="";
    private final Runnable clockTick=new Runnable(){ public void run(){
        if(repo!=null && !repo.current().id.equals(renderedId)) { render(); Sync.refresh(MainActivity.this,null); }
        handler.postDelayed(this,60_000);
    }};
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); repo=new Repository(this);
        if(state!=null){tab=state.getInt("tab");query=state.getString("query","");author=state.getString("author","All thinkers");topic=state.getString("topic","All themes");browseSelected=state.getBoolean("browseSelected",true);}
        render(); Sync.refresh(this,null);
    }
    @Override protected void onResume(){super.onResume(); if(repo!=null){render();Sync.refresh(this,null);} handler.postDelayed(clockTick,60_000);}
    @Override protected void onPause(){super.onPause();handler.removeCallbacks(clockTick);}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putInt("tab",tab);state.putString("query",query);state.putString("author",author);state.putString("topic",topic);state.putBoolean("browseSelected",browseSelected);}
    private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable shape(int color,int radius,boolean border){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(border)d.setStroke(dp(1),p.line);return d;}
    private TextView text(String s,int size,int color,boolean serif){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setFontFeatureSettings("kern");t.setTypeface(Typeface.create(serif?"serif":"sans-serif",Typeface.NORMAL));t.setIncludeFontPadding(false);t.setLineSpacing(dp(3),1.08f);return t;}
    private TextView label(String s){TextView t=text(s,10,p.muted,false);t.setLetterSpacing(.14f);return t;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private void space(LinearLayout l,int h){l.addView(new View(this),new LinearLayout.LayoutParams(1,dp(h)));}
    private void line(LinearLayout l){View v=new View(this);v.setBackgroundColor(p.line);l.addView(v,new LinearLayout.LayoutParams(-1,dp(1)));}
    private void add(LinearLayout l,View v,int top){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(top);l.addView(v,lp);}
    private TextView button(String title,boolean primary,Runnable click){TextView b=text(title,14,primary?p.bg:p.text,false);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setGravity(Gravity.CENTER);b.setPadding(dp(18),dp(15),dp(18),dp(15));b.setMinHeight(dp(48));b.setBackground(shape(primary?p.text:p.card,28,!primary));b.setOnClickListener(v->click.run());b.setFocusable(true);return b;}
    private View iconButton(String icon,String title,Runnable click){FrameLayout f=new FrameLayout(this);IconView v=new IconView(this,icon,p.text);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(22),dp(22),Gravity.CENTER);f.addView(v,lp);f.setContentDescription(title);f.setBackground(shape(p.bg,24,false));f.setOnClickListener(x->click.run());f.setFocusable(true);f.setLayoutParams(new LinearLayout.LayoutParams(dp(48),dp(48)));return f;}
    private AlertDialog.Builder dialogBuilder(){return new AlertDialog.Builder(new ContextThemeWrapper(this,repo.dark()?android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert));}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private void changed(){Sync.refresh(this,null);render();}
    private void switchTab(int n){((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(root.getWindowToken(),0);tab=n;limit=40;render();}

    private void render(){
        p=new Palette(repo.dark());renderedId=repo.current().id;repo.remember(repo.current());
        getWindow().setStatusBarColor(p.bg);getWindow().setNavigationBarColor(p.bg);
        getWindow().getDecorView().setSystemUiVisibility(repo.dark()?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        root=column();root.setBackgroundColor(p.bg);
        root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
        setContentView(root);root.requestApplyInsets();
        LinearLayout header=row();header.setPadding(dp(20),dp(8),dp(12),dp(8));
        TextView brand=text("m.",32,p.accent,true);header.addView(brand,new LinearLayout.LayoutParams(dp(47),-2));
        TextView name=text("Marginalia",22,p.text,false);name.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));header.addView(name,new LinearLayout.LayoutParams(0,-2,1));
        header.addView(iconButton(repo.dark()?"sun":"moon",repo.dark()?"Switch to paper theme":"Switch to ink theme",()->{repo.prefs.edit().putBoolean("dark",!repo.dark()).commit();changed();}));
        header.addView(iconButton("info","About and sources",this::about));root.addView(header);line(root);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setVerticalScrollBarEnabled(false);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));body=column();body.setPadding(dp(24),dp(28),dp(24),dp(28));scroll.addView(body);
        if(tab==0)today();else if(tab==1||tab==2)library();else screens();
        line(root);LinearLayout nav=row();nav.setPadding(dp(10),dp(8),dp(10),dp(8));String[] names={"Today","Library","Saved","Screens"};String[] icons={"today","library","saved","screens"};
        for(int i=0;i<4;i++){final int index=i;LinearLayout n=column();n.setGravity(Gravity.CENTER);n.setPadding(0,dp(5),0,dp(3));IconView iv=new IconView(this,icons[i],tab==i?p.accent:p.muted);n.addView(iv,new LinearLayout.LayoutParams(dp(22),dp(22)));TextView t=text(names[i],10,tab==i?p.accent:p.muted,false);t.setPadding(0,dp(6),0,0);t.setGravity(Gravity.CENTER);n.addView(t);n.setContentDescription(names[i]+" tab");n.setOnClickListener(v->switchTab(index));n.setFocusable(true);n.setSelected(tab==i);nav.addView(n,new LinearLayout.LayoutParams(0,dp(58),1));}root.addView(nav);
    }
    private void today(){
        Quote q=repo.current();LinearLayout meta=row();TextView day=label(new SimpleDateFormat("EEEE, d MMMM",Locale.ENGLISH).format(new Date()).toUpperCase(Locale.ENGLISH));meta.addView(day,new LinearLayout.LayoutParams(0,-2,1));meta.addView(label("MAXIMS & APHORISMS"));body.addView(meta);
        add(body,text("A thought to keep.",30,p.text,true),17);
        add(body,text("Knowledge. Power. Human nature.",13,p.muted,false),9);
        LinearLayout card=column();card.setPadding(dp(23),dp(21),dp(23),dp(23));card.setBackground(shape(p.card,20,true));
        TextView mark=text("“",56,p.accent,true);card.addView(mark,new LinearLayout.LayoutParams(-1,dp(46)));
        TextView quote=text(q.text,q.text.length()>260?21:q.text.length()>160?23:24,p.text,true);quote.setLineSpacing(dp(5),1.06f);quote.setTextIsSelectable(true);add(card,quote,12);
        space(card,26);LinearLayout credit=row();TextView badge=text(q.shortAuthor().substring(0,1),20,p.accent,true);badge.setGravity(Gravity.CENTER);badge.setBackground(shape(p.bg,30,true));credit.addView(badge,new LinearLayout.LayoutParams(dp(42),dp(42)));
        LinearLayout by=column();by.setPadding(dp(13),0,0,0);TextView a=text(q.shortAuthor(),14,p.text,false);a.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));by.addView(a);add(by,text(q.work,11,p.muted,false),5);credit.addView(by,new LinearLayout.LayoutParams(0,-2,1));card.addView(credit);
        if(!q.theme.isEmpty())add(card,label(q.theme.toUpperCase(Locale.ENGLISH)),18);
        add(body,card,25);
        LinearLayout actions=row();TextView save=button(repo.isSaved(q)?"Saved to your collection":"Save this thought",false,()->{repo.toggleSaved(q);render();});actions.addView(save,new LinearLayout.LayoutParams(0,-2,1));actions.addView(iconButton("share","Share aphorism",()->share(q)));actions.addView(iconButton("next","Show another aphorism",()->{repo.next();changed();}));add(body,actions,16);
        add(body,button(repo.staying()?"Resume normal rhythm":"Stay with this thought · 24 hours",false,()->{if(repo.staying())repo.resumeRotation();else repo.stay();changed();}),10);
        add(body,button("Recently shown  →",false,this::recentThoughts),8);
        TextView source=text(q.citation()+"  ↗",12,p.muted,false);source.setMinHeight(dp(44));source.setGravity(Gravity.CENTER_VERTICAL);source.setOnClickListener(v->openUrl(q.url));add(body,source,3);
        if(!q.idea.isEmpty()){add(body,label("READING NOTE · MARGINALIA"),12);add(body,text(q.idea,14,p.text,false),10);add(body,button("Edition & context  →",false,()->details(q)),14);space(body,18);}
        line(body);space(body,20);LinearLayout footer=row();TextView timing=text("A new thought every "+repo.hours()+" hours",12,p.text,false);footer.addView(timing,new LinearLayout.LayoutParams(0,-2,1));footer.addView(label("OFFLINE"));body.addView(footer);add(body,text("Next change · "+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(repo.nextAt())),11,p.muted,false),7);
        add(body,button("Make room on your screens  →",true,()->switchTab(3)),23);
    }
    private void library(){
        boolean onlySaved=tab==2;add(body,label(onlySaved?"YOUR PERSONAL ANTHOLOGY":"THE COMMONPLACE BOOK"),0);
        add(body,text(onlySaved?"Worth returning to.":"Ideas with an edge.",29,p.text,true),12);
        add(body,text(onlySaved?repo.saved().size()+" saved thoughts":String.format(Locale.ENGLISH,"%,d passages · %d thinkers · always offline",repo.all().size(),repo.authors().size()),12,p.muted,false),10);
        EditText search=new EditText(this);search.setSingleLine(true);search.setTextSize(15);search.setTextColor(p.text);search.setHintTextColor(p.muted);search.setHint("Search words or thinkers");search.setContentDescription("Search aphorisms");search.setPadding(dp(17),dp(13),dp(17),dp(13));search.setBackground(shape(p.card,12,true));search.setText(query);add(body,search,22);
        if(!onlySaved){
            LinearLayout collection=row();
            for(boolean selected:new boolean[]{true,false})collection.addView(button(selected?"Selected maxims":"Full anthology",browseSelected==selected,()->{browseSelected=selected;limit=40;render();}),new LinearLayout.LayoutParams(0,-2,1));
            add(body,collection,14);
            add(body,button(author+"  ▾",false,this::chooseBrowseAuthor),12);
            add(body,button(topic+"  ▾",false,this::chooseTopic),8);
        }
        feed=column();add(body,feed,16);populate();
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){query=s.toString();limit=40;populate();}public void afterTextChanged(Editable e){}});
    }
    private void populate(){
        feed.removeAllViews();String needle=searchKey(query);Set<String> saved=repo.saved();ArrayList<Quote> matches=new ArrayList<>();
        for(Quote q:repo.all()) if((tab!=2||saved.contains(q.id))&&(tab==2||!browseSelected||q.selected)&&(tab==2||topic.equals("All themes")||topic.equals(q.theme))&&(tab==2||author.equals("All thinkers")||author.equals(q.author))&&searchKey(q.text+" "+q.author+" "+q.work+" "+q.theme+" "+q.idea).contains(needle))matches.add(q);
        if(matches.isEmpty()){add(feed,text(tab==2&&query.isEmpty()?"Keep a thought with the save button.\nIt will be waiting for you here.":"No passages found. Try another word.",17,p.muted,true),28);return;}
        TextView count=label(matches.size()+(matches.size()==1?" PASSAGE":" PASSAGES"));add(feed,count,2);
        for(int i=0;i<Math.min(limit,matches.size());i++){
            Quote q=matches.get(i);LinearLayout item=column();item.setPadding(0,dp(22),0,dp(18));
            LinearLayout top=row();TextView authorText=text(q.shortAuthor(),12,p.accent,false);top.addView(authorText,new LinearLayout.LayoutParams(0,-2,1));
            TextView save=text(saved.contains(q.id)?"Saved":"Save",12,saved.contains(q.id)?p.accent:p.muted,false);save.setGravity(Gravity.CENTER);save.setMinWidth(dp(54));save.setMinHeight(dp(44));save.setContentDescription((saved.contains(q.id)?"Unsave ":"Save ")+q.id);save.setOnClickListener(v->{boolean added=repo.toggleSaved(q);save.setText(added?"Saved":"Save");save.setTextColor(added?p.accent:p.muted);if(tab==2)populate();});top.addView(save);item.addView(top);
            TextView qt=text(q.text,21,p.text,true);qt.setLineSpacing(dp(3),1.08f);add(item,qt,4);qt.setOnClickListener(v->details(q));
            TextView ref=text(q.citation()+"  ↗",10,p.muted,false);ref.setPadding(0,dp(12),0,dp(8));ref.setMinHeight(dp(44));ref.setOnClickListener(v->openUrl(q.url));item.addView(ref);if(!q.idea.isEmpty())add(item,button("Reading note  →",false,()->details(q)),6);feed.addView(item);line(feed);
        }
        if(matches.size()>limit)add(feed,button("Read more passages",false,()->{limit+=40;populate();}),20);
    }
    static String searchKey(String text){return java.text.Normalizer.normalize(text,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT);}
    private void chooseBrowseAuthor(){ArrayList<String> names=new ArrayList<>();names.add("All thinkers");names.addAll(repo.authors());dialogBuilder().setTitle("Browse a thinker").setItems(names.toArray(new String[0]),(d,i)->{author=names.get(i);limit=40;render();}).show();}
    private void chooseTopic(){ArrayList<String> topics=new ArrayList<>();topics.add("All themes");topics.addAll(repo.themes());dialogBuilder().setTitle("Explore an idea").setItems(topics.toArray(new String[0]),(d,i)->{topic=topics.get(i);limit=40;render();}).show();}
    static String detailText(Quote q){return q.text+(q.idea.isEmpty()?"":"\n\nREADING NOTE · MARGINALIA\n"+q.idea)+"\n\n"+q.citation()+"\nEdition / translation: "+q.translator+(q.note.isEmpty()?"":"\n\nContext: "+q.note)+(q.original.isEmpty()?"":"\n\nOriginal: "+q.original);}
    private void details(Quote q){
        dialogBuilder().setTitle(q.shortAuthor()).setMessage(detailText(q))
            .setPositiveButton("Use on my screens",(d,w)->{repo.use(q);tab=0;changed();toast(q.selected?"Your rotation starts with this maxim":"Full anthology rotation starts with this passage");})
            .setNeutralButton("Read source text",(d,w)->openUrl(q.url)).setNegativeButton("Close",null).show();
    }
    private void share(Quote q){Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,q.text+"\n\n— "+q.author+"\n"+q.citation()+"\n"+q.url);startActivity(Intent.createChooser(i,"Share this thought"));}
    private void openUrl(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(ActivityNotFoundException e){toast("No browser is installed");}}

    private void screens(){
        body.addView(label("A LITTLE PHILOSOPHY, WITHIN REACH"));add(body,text("Outside the app.",30,p.text,true),12);add(body,text("One thought, wherever you pause.",13,p.muted,false),10);add(body,text(BuildInfo.label(this)+" · Upward bubbles",12,p.accent,false),8);
        add(body,label("APPEARANCE"),28);LinearLayout themes=row();themes.addView(themeOption("Paper","Warm & quiet",false),new LinearLayout.LayoutParams(0,dp(99),1));View gap=new View(this);themes.addView(gap,new LinearLayout.LayoutParams(dp(12),1));themes.addView(themeOption("Ink","After hours",true),new LinearLayout.LayoutParams(0,dp(99),1));add(body,themes,12);
        add(body,label("YOUR RHYTHM"),26);LinearLayout cadence=row();cadence.setPadding(dp(4),dp(4),dp(4),dp(4));cadence.setBackground(shape(p.line,28,false));for(int h:new int[]{24,12}){TextView b=button(h==24?"Every day":"Every 12 hours",repo.hours()==h,()->{repo.setHours(h);changed();});cadence.addView(b,new LinearLayout.LayoutParams(0,-2,1));}add(body,cadence,12);
        add(body,button((repo.selectedOnly()?"Selected maxims":"Full anthology")+"  ▾",false,this::chooseCollection),12);
        add(body,text(repo.selectedOnly()?repo.selectedCount()+" selected passages on knowledge, power, history and human nature.":"The complete library, including longer historical passages.",12,p.muted,false),8);
        TextView authors=button(repo.enabledAuthors().size()+" thinkers in your rotation  ▾",false,this::chooseRotationAuthors);add(body,authors,12);
        add(body,button(repo.balanced()?"Rotation · Equal exposure  ▾":"Rotation · Passage shuffle  ▾",false,()->dialogBuilder().setTitle("Rotation").setSingleChoiceItems(new String[]{"Passage shuffle · every passage once per cycle","Equal exposure · each thinker once per round"},repo.balanced()?1:0,(d,i)->{repo.setBalanced(i==1);d.dismiss();changed();}).setNegativeButton("Close",null).show()),10);
        add(body,button(repo.shortPassages()?"Text fit · Short passages  ▾":"Text fit · All lengths  ▾",false,()->dialogBuilder().setTitle("Text fit").setSingleChoiceItems(new String[]{"All lengths · text adapts to the widget","Short passages · up to 180 characters"},repo.shortPassages()?1:0,(d,i)->{repo.setShortPassages(i==1);d.dismiss();changed();}).setNegativeButton("Close",null).show()),10);
        add(body,text("Short passages leave more room for lettering. Thinkers without a matching short passage are temporarily excluded. All screens still share the same thought.",12,p.muted,false),8);
        add(body,label("HOME SCREEN"),28); Quote q=repo.current(); View tapPreview=surfacePreview(SurfaceStyle.read(repo,"widget"));add(body,tapPreview,12);
        add(body,button("Preview upward bubbles",false,()->previewBubbles(tapPreview)),10); add(body,button("Widget style  →",false,()->editSurface("widget")),12); add(body,text(SurfaceStyle.read(repo,"widget").summary(),11,p.muted,false),8);
        add(body,button("Add home-screen widget",true,this::pinWidget),14);add(body,text("Hold to resize. The layout adapts from a compact note to a large quotation. Text gets smaller as the widget shrinks. Very small sizes may shorten long passages. Tap the thought for a brief emoji burst and recoil, or the author to read its source. Open Marginalia from its app icon.",12,p.muted,false),10);
        add(body,button(repo.prefs.getBoolean("widget_burst",true)?"Widget tap · Emoji burst  ▾":"Widget tap · Open app  ▾",false,this::chooseWidgetTap),12);
        add(body,label("LOCK SCREEN"),30);
        add(body,text("Keep your wallpaper.",26,p.text,true),12);
        add(body,text("Marginalia 1.99 does not change either wallpaper.",13,p.muted,false),10);
        add(body,button("Native lock-screen widget  →",false,this::lockWidgetHelp),18);
        add(body,button("Lock-screen widget style  →",false,()->editSurface("lock")),10);
        add(body,text("For phones with a third-party lock-screen widget picker. Position and available sizes depend on your phone.",12,p.muted,false),10);
        add(body,label("QUOTATION CARD"),26);
        add(body,text("A quiet thought over your wallpaper.",22,p.text,true),12);
        add(body,text("An optional notification card works without a lock-screen widget picker. The quote wraps and shrinks to fit the collapsed card. Longer passages still need expansion because Android limits its height. Android controls expansion; the app cannot force it open when you lock. Android controls its position. Choose whether it stays in the notification shade or disappears when you unlock.",13,p.muted,false),10);
        add(body,button(QuoteCard.lockOnly(repo)?"Visibility · lock screen only  ▾":"Visibility · always available  ▾",false,this::chooseCardVisibility),14);
        if(QuoteCard.lockOnly(repo)){
            add(body,text("The quote disappears on unlock and returns when you lock again. Special Notification Access keeps the lock observer available without a background-service notification. Your wallpaper stays unchanged.",12,p.muted,false),10);
            add(body,button(LockScreenMonitor.allowed(this)?"Notification Access enabled · manage":"Set up lock-screen-only mode",false,this::explainLockAccess),10);
        }
        boolean enabled=repo.prefs.getBoolean("quote_card",false);
        add(body,button(enabled?"Show quotation card again":"Show lock-screen quotation card",true,this::enableQuoteCard),16);
        if(enabled)add(body,button("Turn off quotation card",false,()->{QuoteCard.stop(this);render();}),10);
        add(body,button("Notification settings  →",false,this::notificationSettings),10);
        if(enabled&&!QuoteCard.allowed(this))add(body,text("Notifications are blocked. Allow Marginalia in Notification settings, then show the card again.",12,p.accent,false),10);
        add(body,text("Allow notifications on the lock screen and show their content. On Motorola, select the List notification style if your phone offers it. The card has no sound or vibration; your phone may hide silent notifications until you change that setting.",12,p.muted,false),12);
        add(body,text("Already used version 1.0’s wallpaper? Choose your own wallpaper once in your phone’s Settings. This update stops wallpaper rotation but cannot recover the previous picture.",11,p.muted,false),20);
        add(body,label("YOUR DATA"),28);
        add(body,button("Storage breakdown  →",false,this::storageReport),12);
        add(body,button("Export favourites & settings",false,()->backupFile(true)),10);
        add(body,button("Restore a backup",false,()->backupFile(false)),10);
        add(body,text("Android may delay refreshes while the phone sleeps. Allow background battery use if needed. Open the app once after a force-stop.",11,p.muted,false),18);
    }

    private void recentThoughts(){
        List<Quote> recent=repo.recent();String[] names=new String[recent.size()];
        for(int i=0;i<names.length;i++){Quote q=recent.get(i);names[i]=q.shortAuthor()+" · "+q.text;}
        dialogBuilder().setTitle("Recently shown · last 50 distinct thoughts").setItems(names,(d,i)->details(recent.get(i))).setNegativeButton("Close",null).show();
    }
    private void storageReport(){
        AlertDialog dialog=dialogBuilder().setTitle("Storage on this phone").setMessage("Measuring…").setPositiveButton("Close",null).setNeutralButton("Share report",null).show();
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(false);
        new Thread(()->{String report=StorageReport.read(getApplicationContext());handler.post(()->{
            if(isFinishing()||isDestroyed()||!dialog.isShowing())return;
            dialog.setMessage(report);dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(true);
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{Intent share=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,report);startActivity(Intent.createChooser(share,"Share storage report"));});
        });},"storage-report").start();
    }
    private void backupFile(boolean export){
        Intent intent=new Intent(export?Intent.ACTION_CREATE_DOCUMENT:Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json");
        if(export)intent.putExtra(Intent.EXTRA_TITLE,"Marginalia-backup.json");
        try{startActivityForResult(intent,export?199:200);}catch(ActivityNotFoundException e){toast("No document picker is available");}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if((request!=199&&request!=200)||result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        if(request==199){try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){
            if(out==null)throw new IOException();out.write(LocalBackup.exportData(repo).getBytes(StandardCharsets.UTF_8));toast("Backup saved");
        }catch(Exception e){toast("Could not save backup");}return;}
        try(InputStream in=getContentResolver().openInputStream(uri)){
            if(in==null)throw new IOException();ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int count;
            while((count=in.read(buffer))!=-1){if(bytes.size()+count>262144)throw new IOException();bytes.write(buffer,0,count);}
            String raw=bytes.toString("UTF-8");
            dialogBuilder().setTitle("Restore backup?").setMessage("Replace saved thoughts, reading choices and widget styles with this backup. Notification permissions stay unchanged. The daily rhythm starts again from the restored thought.").setNegativeButton("Cancel",null).setPositiveButton("Restore",(d,w)->{
                try{LocalBackup.restore(repo,raw);changed();toast("Backup restored");}catch(Exception e){toast("Invalid or unsupported backup");}
            }).show();
        }catch(Exception e){toast("Cannot read this backup (maximum 256 KB)");}
    }
    private void previewBubbles(View preview){
        int[] location=new int[2];preview.getLocationOnScreen(location);
        Intent intent=new Intent(this,BurstActivity.class).putExtra("quote_id",repo.current().id).putExtra("preview",true);
        intent.setSourceBounds(new Rect(location[0],location[1],location[0]+preview.getWidth(),location[1]+preview.getHeight()));
        startActivity(intent);overridePendingTransition(0,0);
    }
    private void chooseWidgetTap(){
        dialogBuilder().setTitle("When you tap a widget").setSingleChoiceItems(new String[]{"Author emoji burst + gentle recoil","Open Marginalia"},repo.prefs.getBoolean("widget_burst",true)?0:1,(d,which)->{repo.prefs.edit().putBoolean("widget_burst",which==0).commit();d.dismiss();changed();}).setNegativeButton("Cancel",null).show();
    }
    private void disablePreviewTaps(View view){
        view.setOnClickListener(null);view.setClickable(false);
        if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++)disablePreviewTaps(group.getChildAt(i));}
    }
    private View themeOption(String title,String sub,boolean dark){Palette colors=new Palette(dark);LinearLayout l=column();l.setPadding(dp(18),dp(15),dp(18),dp(13));GradientDrawable background=new GradientDrawable();background.setColor(colors.bg);background.setCornerRadius(dp(15));background.setStroke(dp(repo.dark()==dark?2:1),repo.dark()==dark?p.accent:p.line);l.setBackground(background);l.addView(text(title+(repo.dark()==dark?"  ✓":""),22,colors.text,true));add(l,text(sub,11,colors.muted,false),10);l.setOnClickListener(v->{repo.prefs.edit().putBoolean("dark",dark).commit();changed();});l.setContentDescription(title+" theme");l.setFocusable(true);return l;}
    private View surfacePreview(SurfaceStyle style){return surfacePreview(style,220);}
    private View surfacePreview(SurfaceStyle style,int heightDp){
        FrameLayout frame=new FrameLayout(this);
        frame.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(heightDp)));
        if(style.glass()){GradientDrawable backdrop=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xFF8F9B90,0xFF30424B,0xFFBDAB90});backdrop.setCornerRadius(dp(24));frame.setBackground(backdrop);}
        View widget=QuoteWidget.makeStyled(this,repo.current(),style,(heightDp<160?R.layout.quote_widget_compact:heightDp>=260?R.layout.quote_widget_tall:R.layout.quote_widget)).apply(this,frame);
        disablePreviewTaps(widget);
        frame.addView(widget,new FrameLayout.LayoutParams(-1,dp(heightDp)));
        frame.addOnLayoutChangeListener((v,left,top,right,bottom,oldLeft,oldTop,oldRight,oldBottom)->{
            int width=right-left,height=bottom-top;
            if(width<=0||height<=0||(width==oldRight-oldLeft&&height==oldBottom-oldTop))return;
            // Replacing children inside the active layout pass can leave them
            // unmeasured. Rebuild after that pass with the measured dimensions.
            frame.post(()->{
                if(frame.getWidth()!=width||frame.getHeight()!=height)return;
                float density=getResources().getDisplayMetrics().density;
                View fitted=QuoteWidget.makeSized(this,repo.current(),style,heightDp<160?R.layout.quote_widget_compact:heightDp>=260?R.layout.quote_widget_tall:R.layout.quote_widget,Math.round(width/density),Math.round(height/density)).apply(this,frame);
                disablePreviewTaps(fitted);
                frame.removeAllViews();frame.addView(fitted,new FrameLayout.LayoutParams(-1,-1));
            });
        });return frame;
    }
    private void editSurface(String surface){
        SurfaceStyle current=SurfaceStyle.read(repo,surface);String[] selected={current.background,current.font,current.color};
        LinearLayout content=column();content.setPadding(dp(20),dp(8),dp(20),dp(16));
        FrameLayout preview=new FrameLayout(this);content.addView(preview,new LinearLayout.LayoutParams(-1,dp(220)));
        int[] previewHeight={220};
        Runnable update=()->{preview.getLayoutParams().height=dp(previewHeight[0]);preview.requestLayout();preview.removeAllViews();View v=surfacePreview(new SurfaceStyle(selected[0],selected[1],selected[2]),previewHeight[0]);preview.addView(v,new FrameLayout.LayoutParams(-1,dp(previewHeight[0])));};update.run();
        LinearLayout sizes=row();for(int h:new int[]{110,220,300})sizes.addView(button(h==110?"Compact":h==220?"Regular":"Tall",false,()->{previewHeight[0]=h;update.run();}),new LinearLayout.LayoutParams(0,-2,1));add(content,sizes,10);
        add(content,text("Preview sizes · your launcher determines the final width and height.",11,p.muted,false),8);
        String[][] titles={{"Paper","Ink","Frosted glass","Liquid glass"},{"Literary serif","Clean sans","Subtitles"},{"Auto","Cream","Yellow","Charcoal"}};
        String[][] values={{"paper","ink","glass","liquid"},{"serif","sans","subtitle"},{"auto","ivory","yellow","charcoal"}};
        String[] labels={"BACKGROUND","LETTERING","TEXT COLOUR"};
        for(int group=0;group<3;group++){
            final int index=group;add(content,label(labels[group]),18);RadioGroup radios=new RadioGroup(this);radios.setOrientation(RadioGroup.VERTICAL);
            for(int i=0;i<titles[group].length;i++){RadioButton radio=new RadioButton(this);radio.setId(View.generateViewId());radio.setText(titles[group][i]);radio.setTextSize(13);radio.setTextColor(p.text);radio.setButtonTintList(ColorStateList.valueOf(p.accent));radio.setMinHeight(dp(40));radios.addView(radio);radio.setChecked(selected[group].equals(values[group][i]));String value=values[group][i];radio.setOnClickListener(v->{selected[index]=value;update.run();});}content.addView(radios);
        }
        add(content,text(surface.equals("widget")?"Liquid glass adds reflective edges and translucent highlights; frosted glass is softer. These effects do not blur or refract the live wallpaper. Subtitles flow across the available width and adapt their line count as you resize.":"This style applies to the native lock-screen widget. Glass effects are translucent, without live wallpaper blur. The notification card uses your phone’s system appearance.",12,p.muted,false),16);
        ScrollView scroll=new ScrollView(this);scroll.addView(content);
        dialogBuilder().setTitle(surface.equals("widget")?"Your widget style":"Your lock-screen style").setView(scroll).setNegativeButton("Cancel",null).setPositiveButton("Apply style",(d,w)->{
            repo.prefs.edit().putString(surface+"_background",selected[0]).putString(surface+"_font",selected[1]).putString(surface+"_color",selected[2]).commit();changed();
        }).show();
    }
    private void lockWidgetHelp(){
        dialogBuilder().setTitle("Native lock-screen widget").setMessage("If your phone’s lock-screen customization includes an app widget picker, choose ‘Marginalia · lock screen’. It uses the style selected here and keeps your wallpaper.\n\nAndroid 16 alone does not guarantee this picker: the phone manufacturer must provide it. If you cannot find it on your Moto G85 or S20, use the quotation card below. Marginalia cannot add a widget host to the system lock screen.").setPositiveButton("Got it",null).show();
    }
    private void enableQuoteCard(){
        QuoteCard.ensureChannel(this);
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){
            if(repo.prefs.getBoolean("notification_permission_asked",false)&&!shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)){notificationSettings();toast("Allow notifications, then show the card again");return;}
            repo.prefs.edit().putBoolean("notification_permission_asked",true).apply();
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},62);return;
        }
        if(!QuoteCard.allowed(this)){notificationSettings();toast("Allow the Lock-screen aphorism notification channel");return;}
        activateQuoteCard();
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){
        super.onRequestPermissionsResult(request,permissions,results);
        if(request!=62)return;
        if(results.length>0&&results[0]==android.content.pm.PackageManager.PERMISSION_GRANTED){activateQuoteCard();}
        else toast("The card needs notification permission. Your wallpaper is unchanged.");
    }
    private void activateQuoteCard(){
        QuoteCard.enable(this);render();
        if(QuoteCard.lockOnly(repo)&&!LockScreenMonitor.allowed(this))explainLockAccess();
        else toast(QuoteCard.lockOnly(repo)?"Ready for your next lock screen":"Quotation card enabled");
    }
    private void chooseCardVisibility(){
        dialogBuilder().setTitle("When to show the quote").setSingleChoiceItems(new String[]{"Always available · lock screen and notification shade","Lock screen only · hide on unlock"},QuoteCard.lockOnly(repo)?1:0,(d,i)->{
            repo.prefs.edit().putBoolean("card_lock_only",i==1).remove("card_dismissed_quote").commit();d.dismiss();changed();
            if(i==1&&!LockScreenMonitor.allowed(this))explainLockAccess();
        }).setNegativeButton("Close",null).show();
    }
    private void explainLockAccess(){
        dialogBuilder().setTitle("Lock-screen-only setup").setMessage("Android needs special Notification Access to keep the lock observer available without leaving a service notification on your unlocked phone.\n\nAndroid’s permission is broad and can grant access to other apps’ notification contents. Marginalia disables all notification-content categories and does not read, store or transmit other apps’ notifications. It only observes screen and unlock events to show or remove its own quote.\n\nEnable ‘Marginalia · lock-screen-only quotes’ on the next screen. You can revoke access there at any time.\n\nIf Android blocks this for a downloaded APK, review App info → the three-dot menu → Allow restricted settings, then return here. Only enable this if you trust the app.\n\nLock-screen notification content must also be allowed in your phone’s Settings. Battery restrictions or a force-stop can interrupt the observer; reopen Marginalia if needed.")
            .setPositiveButton("Open Android settings",(d,w)->{
                Intent intent=new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                try{startActivity(intent);}catch(ActivityNotFoundException e){toast("Open Settings → Special app access → Notification access");}
            }).setNegativeButton("Not now",null).show();
    }
    private void notificationSettings(){
        Intent intent=new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,getPackageName());
        try{startActivity(intent);}catch(ActivityNotFoundException e){toast("Open Settings → Apps → Marginalia → Notifications");}
    }
    private void chooseCollection(){dialogBuilder().setTitle("Daily reading").setSingleChoiceItems(new String[]{"Selected maxims · concise & conceptual","Full anthology · every passage"},repo.selectedOnly()?0:1,(d,i)->{repo.setSelectedOnly(i==0);d.dismiss();changed();}).setNegativeButton("Close",null).show();}
    private void chooseRotationAuthors(){List<String> names=repo.authors();Set<String> selected=repo.enabledAuthors();boolean[] checks=new boolean[names.size()];for(int i=0;i<checks.length;i++)checks[i]=selected.contains(names.get(i));AlertDialog dialog=dialogBuilder().setTitle("Your thinkers").setMultiChoiceItems(names.toArray(new String[0]),checks,(d,i,yes)->{if(yes)selected.add(names.get(i));else selected.remove(names.get(i));}).setPositiveButton("Apply",null).setNegativeButton("Cancel",null).create();dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(x->{if(selected.isEmpty()){toast("Choose at least one thinker");return;}repo.setAuthors(selected);changed();dialog.dismiss();}));dialog.show();}
    private void pinWidget(){AppWidgetManager m=AppWidgetManager.getInstance(this);if(m.isRequestPinAppWidgetSupported()){boolean sent=m.requestPinAppWidget(new ComponentName(this,QuoteWidget.class),null,null);if(!sent)toast("Hold your home screen, choose Widgets, then Marginalia.");}else dialogBuilder().setTitle("Add your widget").setMessage("Hold an empty space on your home screen. Tap Widgets, find Marginalia, then drag the widget into place.").setPositiveButton("Got it",null).show();}
    private void about(){dialogBuilder().setTitle("Marginalia").setMessage("A little philosophy, every day.\n\n"+repo.all().size()+" short passages from "+repo.authors().size()+" thinkers. No account, no ads, no tracking, and no internet permission. Saved thoughts and settings stay on your phone. The optional quotation card uses notifications; lock-screen-only mode additionally requires opt-in Notification Access and processes no other apps’ notification content; this version never changes your wallpaper.\n\nInspired by Read Adagia. This is an independent app. Selected maxims focus on knowledge, power, institutions, history and human nature; the full anthology stays available.\n\nEvery passage links to its source text or a publisher’s excerpt. Reading notes are Marginalia interpretations, separate from the quotations. Source notes distinguish authorial writing, dialogue and reported sayings. Historical translations retain older wording; new editorial translations are labelled. Whitespace and printed emphasis are normalized; editorial notes are omitted.\n\nVersion "+BuildInfo.label(this)+" · Android 8+").setPositiveButton("Close",null).setNeutralButton("Sources & licences",(d,w)->licenses()).show();}
    private void licenses(){
        String text="";
        try(InputStream in=getAssets().open("source-notes.txt")){text=readStream(in)+"\n\n";}catch(IOException ignored){}
        try(InputStream in=getAssets().open("source-license.txt")){text+=readStream(in);}catch(IOException ignored){}
        try(InputStream in=getAssets().open("liquid-glass-license.txt")){text+="\n\n"+readStream(in);}catch(IOException ignored){}
        TextView t=new TextView(this);t.setText(text);t.setTextColor(p.text);t.setTextSize(12);t.setPadding(dp(24),dp(12),dp(24),dp(20));ScrollView s=new ScrollView(this);s.addView(t);dialogBuilder().setTitle("Sources & licences").setView(s).setPositiveButton("Close",null).show();
    }
    static String readStream(InputStream in)throws IOException{ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)bytes.write(buffer,0,n);return new String(bytes.toByteArray(),StandardCharsets.UTF_8);}
}

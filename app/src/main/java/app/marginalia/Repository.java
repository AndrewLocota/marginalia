package app.marginalia;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class Repository {
    private static List<Quote> cached;
    public final SharedPreferences prefs;
    private final Context context;
    public Repository(Context c) {
        context=c.getApplicationContext(); prefs=context.getSharedPreferences("marginalia",Context.MODE_PRIVATE);
        if(!prefs.contains("wallpaper_retired"))prefs.edit().putBoolean("wallpaper_retired",true).putBoolean("wallpaper",false).remove("wallpaper_key").remove("wallpaper_error").apply();
        if (!prefs.contains("anchor")) prefs.edit().putLong("anchor",System.currentTimeMillis()).putString("start","nietzsche-108").commit();
        if(!prefs.contains("maxims_edition")){
            SharedPreferences.Editor edit=prefs.edit().putBoolean("maxims_edition",true);
            // Expand an old all-thinkers selection; retain any deliberately narrowed choice.
            Set<String> old=prefs.getStringSet("authors",null);
            if(old!=null&&old.equals(new HashSet<>(Arrays.asList("Johann Wolfgang von Goethe","Friedrich Nietzsche","François de La Rochefoucauld","Epictetus","Marcus Aurelius","Epicurus"))))edit.remove("authors");
            edit.commit();
        }
        if(!prefs.getBoolean("concepts_edition",false)){
            SharedPreferences.Editor edit=prefs.edit().putBoolean("concepts_edition",true);
            // A saved all-authors choice should include the new writers after an update.
            // Explicit subsets remain the user's choice.
            Set<String> old=prefs.getStringSet("authors",null);
            if(old!=null){
                try(InputStream in=context.getAssets().open("previous-authors.json")){
                    JSONArray a=new JSONArray(MainActivity.readStream(in));Set<String> previous=new HashSet<>();
                    for(int i=0;i<a.length();i++)previous.add(a.getString(i));
                    if(old.equals(previous))edit.remove("authors");
                }catch(Exception e){throw new IllegalStateException("Previous author list could not be read",e);}
            }
            edit.commit();
        }
    }
    public synchronized List<Quote> all() {
        if(cached!=null) return cached;
        try(InputStream in=context.getAssets().open("quotes.json")) {
            JSONArray a=new JSONArray(MainActivity.readStream(in));
            ArrayList<Quote> list=new ArrayList<>();
            for(int i=0;i<a.length();i++) list.add(new Quote(a.getJSONObject(i)));
            cached=Collections.unmodifiableList(list); return cached;
        } catch(Exception e) { throw new IllegalStateException("The bundled library could not be read",e); }
    }
    public List<String> authors() {
        LinkedHashSet<String> names=new LinkedHashSet<>();
        for(Quote q:all()) names.add(q.author); ArrayList<String> sorted=new ArrayList<>(names);Collections.sort(sorted);return sorted;
    }
    public boolean selectedOnly(){return prefs.getBoolean("selected_only",true);}
    public int selectedCount(){int n=0;for(Quote q:all())if(q.selected)n++;return n;}
    public List<String> themes(){TreeSet<String> names=new TreeSet<>();for(Quote q:all())if(!q.theme.isEmpty())names.add(q.theme);return new ArrayList<>(names);}
    public Set<String> enabledAuthors() { return new HashSet<>(prefs.getStringSet("authors", new HashSet<>(authors()))); }
    public List<Quote> deck() {
        Set<String> selected=enabledAuthors(); ArrayList<Quote> list=new ArrayList<>();
        for(Quote q:all()) if(selected.contains(q.author)&&(!selectedOnly()||q.selected)) list.add(q);
        if(list.isEmpty())for(Quote q:all())if(!selectedOnly()||q.selected)list.add(q);
        if(shortPassages()){
            ArrayList<Quote> shortList=new ArrayList<>();
            for(Quote q:list)if(q.text.length()<=180)shortList.add(q);
            if(!shortList.isEmpty())list=shortList;
        }
        Collections.shuffle(list,new Random(8417)); return list;
    }
    public int hours() { return prefs.getInt("hours",24)==12?12:24; }
    public boolean dark() { return prefs.getBoolean("dark",false); }
    public Quote current() { return currentAt(System.currentTimeMillis()); }
    public Quote currentAt(long now) {
        List<Quote> list=deck();
        if(balanced())return balancedAt(list,prefs.getLong("balanced_step",0)+Rotation.elapsedSlots(now,prefs.getLong("anchor",now),hours()));
        int start=0;
        String id=prefs.getString("start","nietzsche-108");
        for(int i=0;i<list.size();i++) if(list.get(i).id.equals(id)) { start=i; break; }
        return list.get(Rotation.index(now,prefs.getLong("anchor",now),hours(),start,list.size()));
    }
    public long nextAt() { return Rotation.next(System.currentTimeMillis(),prefs.getLong("anchor",System.currentTimeMillis()),hours()); }
    public void use(Quote q) {
        Set<String> authors=enabledAuthors(); authors.add(q.author);
        SharedPreferences.Editor edit=prefs.edit().putString("start",q.id).putLong("anchor",System.currentTimeMillis()).putStringSet("authors",authors);
        if(!q.selected)edit.putBoolean("selected_only",false);
        if(q.text.length()>180)edit.putBoolean("short_passages",false);
        edit.remove("hold_until").commit();
        if(balanced())prefs.edit().putLong("balanced_step",positionOf(deck(),q)).commit();
        remember(q);
    }
    public void setSelectedOnly(boolean selected){
        Quote q=current();prefs.edit().putBoolean("selected_only",selected).commit();
        use(!selected||q.selected?q:deck().get(0));
    }
    public void next() {
        if(balanced()){
            long now=System.currentTimeMillis();
            long step=prefs.getLong("balanced_step",0)+Rotation.elapsedSlots(now,prefs.getLong("anchor",now),hours())+1;
            prefs.edit().putLong("balanced_step",step).putLong("anchor",now).remove("hold_until").commit();remember(current());return;
        }
        List<Quote> list=deck(); String id=current().id;
        for(int i=0;i<list.size();i++) if(list.get(i).id.equals(id)) { use(list.get((i+1)%list.size())); return; }
    }
    public void setHours(int h) { Quote q=current(); use(q); prefs.edit().putInt("hours",h).commit(); }
    public void setAuthors(Set<String> names) {
        Quote current=current(); prefs.edit().putStringSet("authors",new HashSet<>(names)).commit();
        if(names.contains(current.author)) use(current); else use(deck().get(0));
    }
    public boolean balanced(){return prefs.getBoolean("balanced",false);}
    public boolean shortPassages(){return prefs.getBoolean("short_passages",false);}
    public void setBalanced(boolean enabled){Quote q=current();prefs.edit().putBoolean("balanced",enabled).commit();use(q);}
    public void setShortPassages(boolean enabled){
        Quote q=current();prefs.edit().putBoolean("short_passages",enabled).commit();
        List<Quote> choices=deck();boolean found=false;for(Quote v:choices)if(v.id.equals(q.id))found=true;
        use(found?q:choices.get(0));
    }
    private static LinkedHashMap<String,List<Quote>> groups(List<Quote> deck){
        TreeMap<String,List<Quote>> sorted=new TreeMap<>();
        for(Quote q:deck)sorted.computeIfAbsent(q.author,k->new ArrayList<>()).add(q);
        List<String> authors=new ArrayList<>(sorted.keySet());Collections.shuffle(authors,new Random(199));
        LinkedHashMap<String,List<Quote>> groups=new LinkedHashMap<>();for(String a:authors)groups.put(a,sorted.get(a));return groups;
    }
    static Quote balancedAt(List<Quote> deck,long step){
        List<List<Quote>> groups=new ArrayList<>(groups(deck).values());
        int author=(int)Math.floorMod(step,groups.size());List<Quote> choices=groups.get(author);
        return choices.get((int)Math.floorMod(Math.floorDiv(step,groups.size()),choices.size()));
    }
    private static long positionOf(List<Quote> deck,Quote q){
        LinkedHashMap<String,List<Quote>> groups=groups(deck);int author=0;
        for(List<Quote> choices:groups.values()){
            for(int i=0;i<choices.size();i++)if(choices.get(i).id.equals(q.id))return (long)i*groups.size()+author;
            author++;
        }return 0;
    }
    public void stay(){
        Quote q=current();use(q);long until=System.currentTimeMillis()+86_400_000L;
        prefs.edit().putLong("anchor",until-hours()*3_600_000L).putLong("hold_until",until).commit();
    }
    public boolean staying(){return prefs.getLong("hold_until",0)>System.currentTimeMillis();}
    public void resumeRotation(){use(current());}
    public synchronized void remember(Quote q){
        String previous=prefs.getString("recent","");String[] entries=previous.split("\\n");
        if(entries.length>0&&entries[0].equals(q.id))return;
        StringBuilder out=new StringBuilder(q.id);int count=1;
        for(String id:entries)if(!id.isEmpty()&&!id.equals(q.id)&&count++<50)out.append('\n').append(id);
        prefs.edit().putString("recent",out.toString()).apply();
    }
    public List<Quote> recent(){
        Map<String,Quote> byId=new HashMap<>();for(Quote q:all())byId.put(q.id,q);
        List<Quote> result=new ArrayList<>();for(String id:prefs.getString("recent","").split("\\n"))if(byId.containsKey(id))result.add(byId.get(id));return result;
    }
    public Set<String> saved() { return new HashSet<>(prefs.getStringSet("saved",Collections.emptySet())); }
    public boolean isSaved(Quote q) { return saved().contains(q.id); }
    public boolean toggleSaved(Quote q) {
        Set<String> set=saved(); boolean added=set.add(q.id); if(!added)set.remove(q.id);
        prefs.edit().putStringSet("saved",set).apply(); return added;
    }
}

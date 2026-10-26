package app.marginalia;

import android.content.SharedPreferences;
import org.json.*;
import java.util.*;

/** Versioned, bounded user-preference backup. Never imports permissions or render caches. */
public final class LocalBackup {
    private static final String[] BOOLS={"dark","selected_only","balanced","short_passages","widget_burst"};
    private static final String[] SETS={"authors","saved"};
    private static final String[] STYLE={"widget_background","lock_background","widget_font","lock_font","widget_color","lock_color"};
    private LocalBackup(){}
    public static String exportData(Repository r)throws JSONException{
        JSONObject values=new JSONObject();
        for(String k:BOOLS)if(r.prefs.contains(k))values.put(k,r.prefs.getBoolean(k,false));
        for(String k:SETS)if(r.prefs.contains(k))values.put(k,new JSONArray(new TreeSet<>(r.prefs.getStringSet(k,Collections.emptySet()))));
        for(String k:STYLE)if(r.prefs.contains(k))values.put(k,r.prefs.getString(k,""));
        values.put("hours",r.hours());values.put("start",r.current().id);values.put("recent",r.prefs.getString("recent",""));
        return new JSONObject().put("format","marginalia-backup").put("version",1).put("settings",values).toString(2);
    }
    public static void restore(Repository r,String raw)throws JSONException{
        if(raw.length()>262144)throw new JSONException("Backup is too large");
        JSONObject root=new JSONObject(raw);
        if(!"marginalia-backup".equals(root.optString("format"))||root.optInt("version")!=1)throw new JSONException("Unsupported backup");
        JSONObject data=root.getJSONObject("settings");
        Set<String> authors=new HashSet<>(r.authors()),ids=new HashSet<>();for(Quote q:r.all())ids.add(q.id);
        Map<String,Object> valid=new HashMap<>();
        for(String k:BOOLS)if(data.has(k)){Object v=data.get(k);if(!(v instanceof Boolean))throw new JSONException("Invalid "+k);valid.put(k,v);}
        for(String k:SETS)if(data.has(k)){
            JSONArray array=data.getJSONArray(k);if(array.length()>2000)throw new JSONException("Too many entries");
            Set<String> set=new HashSet<>();for(int i=0;i<array.length();i++){String v=array.getString(i);if((k.equals("authors")?authors:ids).contains(v))set.add(v);}
            if(k.equals("authors")&&set.isEmpty())throw new JSONException("No known thinkers");valid.put(k,set);
        }
        for(String k:STYLE)if(data.has(k)){
            String v=data.getString(k);String allowed=k.endsWith("background")?"paper ink glass liquid":k.endsWith("font")?"serif sans subtitle":"auto ivory yellow charcoal";
            if(!Arrays.asList(allowed.split(" ")).contains(v))throw new JSONException("Invalid style");valid.put(k,v);
        }
        int hours=data.optInt("hours",24);if(hours!=12&&hours!=24)throw new JSONException("Invalid rhythm");
        String start=data.getString("start");if(!ids.contains(start))throw new JSONException("Unknown current passage");
        StringBuilder recent=new StringBuilder();int count=0;Set<String> seen=new HashSet<>();
        for(String id:data.optString("recent","").split("\n"))if(ids.contains(id)&&seen.add(id)&&count++<50){if(recent.length()>0)recent.append('\n');recent.append(id);}
        // Validate the entire document before changing a single preference.
        SharedPreferences.Editor edit=r.prefs.edit();
        for(String[] keys:new String[][]{BOOLS,SETS,STYLE})for(String k:keys)edit.remove(k);
        for(Map.Entry<String,Object> entry:valid.entrySet()){
            Object v=entry.getValue();String k=entry.getKey();
            if(v instanceof Boolean)edit.putBoolean(k,(Boolean)v);
            else if(v instanceof Set)edit.putStringSet(k,(Set<String>)v);
            else edit.putString(k,(String)v);
        }
        edit.putInt("hours",hours).putString("recent",recent.toString()).remove("hold_until");
        for(String k:r.prefs.getAll().keySet())if(k.startsWith("widget_render_"))edit.remove(k);
        if(!edit.commit())throw new JSONException("Could not save preferences");
        for(Quote q:r.all())if(q.id.equals(start)){r.use(q);break;}
    }
}

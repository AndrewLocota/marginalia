package app.marginalia;

import android.content.Context;
import org.json.*;
import java.io.InputStream;
import java.util.*;

/** Small, offline visual motifs; these are decoration, not author quotations. */
final class AuthorEmoji {
    private static Map<String,String[]> palettes;
    static synchronized String[] forAuthor(Context context,String author){
        if(palettes==null){
            Map<String,String[]> loaded=new HashMap<>();
            try(InputStream in=context.getAssets().open("author-emojis.json")){
                JSONObject json=new JSONObject(MainActivity.readStream(in));
                for(Iterator<String> keys=json.keys();keys.hasNext();){String key=keys.next();JSONArray values=json.getJSONArray(key);String[] list=new String[values.length()];for(int i=0;i<list.length;i++)list[i]=values.getString(i);loaded.put(key,list);}
            }catch(Exception e){android.util.Log.w("Marginalia","Cannot load emoji motifs",e);}
            palettes=loaded;
        }
        String[] value=palettes.get(author);
        return value==null?new String[]{"📚","💭","✨","🖋️"}:value.clone();
    }
    static boolean hasAuthor(Context context,String author){forAuthor(context,author);return palettes.containsKey(author);}
}

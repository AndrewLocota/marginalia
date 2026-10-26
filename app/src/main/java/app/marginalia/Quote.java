package app.marginalia;

import org.json.JSONObject;

public final class Quote {
    public final String id, text, author, work, reference, translator, url, theme, note, original, idea;
    public final boolean selected;
    Quote(JSONObject j) {
        id=j.optString("id"); text=j.optString("text"); author=j.optString("author");
        work=j.optString("work"); reference=j.optString("reference");
        translator=j.optString("translator"); url=j.optString("url");
        selected=j.optBoolean("selected");theme=j.optString("theme");note=j.optString("note");original=j.optString("original");
        idea=j.optString("idea");
    }
    public String shortAuthor() {
        return author.replace("Johann Wolfgang von ", "").replace("Friedrich ", "")
                .replace("François de ", "");
    }
    public String citation() { return work + " · " + reference; }
}

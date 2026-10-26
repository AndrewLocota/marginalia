package app.marginalia;

import android.content.*;

public final class QuoteCardReceiver extends BroadcastReceiver {
    static final String NEXT="app.marginalia.CARD_NEXT",STOP="app.marginalia.CARD_STOP",DISMISSED="app.marginalia.CARD_DISMISSED";
    @Override public void onReceive(Context c,Intent intent){
        String action=intent.getAction();Repository r=new Repository(c);
        if(STOP.equals(action)){QuoteCard.stop(c);return;}
        if(DISMISSED.equals(action)){r.prefs.edit().putString("card_dismissed_quote",r.current().id).apply();return;}
        if(!NEXT.equals(action)||!r.prefs.getBoolean("quote_card",false))return;
        r.next();PendingResult pending=goAsync();Sync.refresh(c,pending::finish);
    }
}

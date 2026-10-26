package app.marginalia;

import android.app.KeyguardManager;
import android.content.*;
import android.os.*;
import android.service.notification.NotificationListenerService;

/** Optional, system-bound lock-state observer. No notification content callbacks or queries. */
public final class LockScreenMonitor extends NotificationListenerService {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean registered;
    private final Runnable settle=()->Sync.refresh(this,null);
    private final BroadcastReceiver screenEvents=new BroadcastReceiver(){
        @Override public void onReceive(Context c,Intent intent){
            Repository r=new Repository(c);
            if(!r.prefs.getBoolean("quote_card",false)||!QuoteCard.lockOnly(r))return;
            if(Intent.ACTION_USER_PRESENT.equals(intent.getAction())){
                // Cancel immediately; do not wait behind queued widget rendering.
                QuoteCard.manager(c).cancel(QuoteCard.ID);
            }
            handler.removeCallbacks(settle);
            Sync.refresh(c,null);
            // Keyguard can settle just after SCREEN_ON/OFF is delivered.
            handler.postDelayed(settle,750);
        }
    };
    static boolean locked(Context c){return c.getSystemService(KeyguardManager.class).isKeyguardLocked();}
    static boolean allowed(Context c){
        ComponentName component=new ComponentName(c,LockScreenMonitor.class);
        if(Build.VERSION.SDK_INT>=27)return QuoteCard.manager(c).isNotificationListenerAccessGranted(component);
        String names=android.provider.Settings.Secure.getString(c.getContentResolver(),"enabled_notification_listeners");
        if(names!=null)for(String name:names.split(":"))if(component.equals(ComponentName.unflattenFromString(name)))return true;
        return false;
    }
    @Override public void onListenerConnected(){
        super.onListenerConnected();
        if(!registered){
            IntentFilter filter=new IntentFilter();filter.addAction(Intent.ACTION_SCREEN_ON);filter.addAction(Intent.ACTION_SCREEN_OFF);filter.addAction(Intent.ACTION_USER_PRESENT);
            // USER_PRESENT can originate from System UI's UID. These three actions
            // are protected system broadcasts, so ordinary apps cannot spoof them.
            if(Build.VERSION.SDK_INT>=33)registerReceiver(screenEvents,filter,Context.RECEIVER_EXPORTED);else registerReceiver(screenEvents,filter);
            registered=true;
        }
        Sync.refresh(this,null);
    }
    private void cleanup(){
        handler.removeCallbacksAndMessages(null);
        if(registered){unregisterReceiver(screenEvents);registered=false;}
        if(QuoteCard.lockOnly(new Repository(this)))QuoteCard.manager(this).cancel(QuoteCard.ID);
    }
    @Override public void onListenerDisconnected(){cleanup();super.onListenerDisconnected();}
    @Override public void onDestroy(){cleanup();super.onDestroy();}
}

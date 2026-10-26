package app.marginalia;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.widget.RemoteViews;

/** A quiet, optional notification card. Never reads or writes the user's wallpaper. */
public final class QuoteCard {
    static final String CHANNEL="lock_screen_aphorism";
    static final int ID=7493;
    private QuoteCard(){}
    static NotificationManager manager(Context c){return c.getSystemService(NotificationManager.class);}
    static void ensureChannel(Context c){
        NotificationChannel channel=new NotificationChannel(CHANNEL,"Lock-screen aphorism",NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("One quiet quotation, refreshed every 12 or 24 hours.");
        channel.setSound(null,null);channel.enableVibration(false);channel.enableLights(false);
        channel.setShowBadge(false);channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        manager(c).createNotificationChannel(channel);
    }
    static boolean allowed(Context c){
        if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return false;
        NotificationChannel channel=manager(c).getNotificationChannel(CHANNEL);
        return manager(c).areNotificationsEnabled()&&(channel==null||channel.getImportance()!=NotificationManager.IMPORTANCE_NONE);
    }
    static PendingIntent action(Context c,String action){
        return PendingIntent.getBroadcast(c,0,new Intent(c,QuoteCardReceiver.class).setAction(action),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    static PendingIntent source(Context c,Quote q){
        Intent intent=new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(q.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(c,7494,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    static Notification build(Context c,Quote q){
        PendingIntent open=PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Bundle extras=new Bundle();extras.putString("quote_id",q.id);extras.putInt("card_edition",3);extras.putString("source_url",q.url);
        RemoteViews compact=new RemoteViews(c.getPackageName(),R.layout.quote_notification);
        compact.setTextViewText(R.id.notification_quote,compactText(q));
        compact.setContentDescription(R.id.notification_quote,q.text+" — "+q.author);
        return new Notification.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_quote_notification)
            .setContentTitle(q.shortAuthor()).setContentText(q.text).setSubText(q.shortAuthor())
            .setCustomContentView(compact)
            .setStyle(new Notification.BigTextStyle().bigText(q.text).setSummaryText(q.work))
            .setContentIntent(open).setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_REMINDER).setShowWhen(false).setOnlyAlertOnce(true)
            .setOngoing(true).setAutoCancel(false).setBadgeIconType(Notification.BADGE_ICON_NONE)
            .setDeleteIntent(action(c,QuoteCardReceiver.DISMISSED)).addExtras(extras)
            .addAction(new Notification.Action.Builder(null,"Next thought",action(c,QuoteCardReceiver.NEXT)).build())
            .addAction(new Notification.Action.Builder(null,"Turn off",action(c,QuoteCardReceiver.STOP)).build())
            .addAction(new Notification.Action.Builder(null,"Read source",source(c,q)).build())
            .build();
    }
    static boolean lockOnly(Repository r){return r.prefs.getBoolean("card_lock_only",false);}
    static String compactText(Quote q){return q.text+" — "+q.shortAuthor();}
    static void update(Context c,Repository r,Quote q){
        if(!r.prefs.getBoolean("quote_card",false)){manager(c).cancel(ID);return;}
        if(lockOnly(r)&&(!LockScreenMonitor.allowed(c)||!LockScreenMonitor.locked(c))){manager(c).cancel(ID);return;}
        ensureChannel(c);
        if(!allowed(c))return;
        if(q.id.equals(r.prefs.getString("card_dismissed_quote","")))return;
        for(StatusBarNotification active:manager(c).getActiveNotifications())
            if(active.getId()==ID&&q.id.equals(active.getNotification().extras.getString("quote_id"))&&active.getNotification().extras.getInt("card_edition")==3)return;
        manager(c).notify(ID,build(c,q));
    }
    static void enable(Context c){
        new Repository(c).prefs.edit().putBoolean("quote_card",true).remove("card_dismissed_quote").apply();
        Sync.refresh(c,null);
    }
    static void stop(Context c){
        new Repository(c).prefs.edit().putBoolean("quote_card",false).remove("card_dismissed_quote").apply();
        manager(c).cancel(ID);Sync.schedule(c);
    }
}

package app.marginalia;

import android.app.*;
import android.app.job.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.util.Log;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Sync {
    private static final ExecutorService worker=Executors.newSingleThreadExecutor();
    private static final int JOB=7491;
    private Sync() {}
    public static void refresh(Context c,Runnable done) {
        Context app=c.getApplicationContext();
        worker.execute(()->{ try { updateNow(app); } catch(Exception e) { Log.e("Marginalia","Refresh failed",e); }
            finally { schedule(app); if(done!=null)done.run(); } });
    }
    static synchronized void updateNow(Context c) {
        Repository r=new Repository(c); Quote q=r.current(); r.remember(q); QuoteWidget.updateAll(c,q);
        QuoteCard.update(c,r,q);
    }

    static void schedule(Context c) {
        Repository r=new Repository(c);
        boolean widget=QuoteWidget.hasWidgets(c);
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi=PendingIntent.getBroadcast(c,14,new Intent(c,RefreshReceiver.class).setAction("app.marginalia.REFRESH"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        JobScheduler jobs=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if(!widget && !r.prefs.getBoolean("quote_card",false)) { am.cancel(pi); jobs.cancel(JOB); return; }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,r.nextAt(),pi);
        if(jobs.getPendingJob(JOB)==null)
            jobs.schedule(new JobInfo.Builder(JOB,new ComponentName(c,RefreshJob.class))
                .setPersisted(true).setPeriodic(3_600_000L,900_000L).build());
    }
}

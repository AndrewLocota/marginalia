package app.marginalia;

import android.content.*;

public final class RefreshReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent) {
        String action=intent.getAction();
        if(!Intent.ACTION_BOOT_COMPLETED.equals(action) && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                && !Intent.ACTION_TIME_CHANGED.equals(action) && !Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                && !"app.marginalia.REFRESH".equals(action)) return;
        PendingResult pending=goAsync(); Sync.refresh(context,pending::finish);
    }
}

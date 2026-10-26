package app.marginalia;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.app.usage.StorageStats;
import android.app.usage.StorageStatsManager;
import android.os.Process;
import android.os.Build;
import android.text.format.Formatter;
import java.io.File;

/** Reads only our package's statistics. No usage-access permission or file deletion. */
public final class StorageReport {
    private StorageReport(){}
    static long apkBytes(Context c){
        ApplicationInfo info=c.getApplicationInfo();long size=new File(info.sourceDir).length();
        if(info.splitSourceDirs!=null)for(String path:info.splitSourceDirs)size+=new File(path).length();return size;
    }
    public static String read(Context c){
        StringBuilder out=new StringBuilder("Marginalia "+BuildInfo.label(c)+"\nAndroid "+Build.VERSION.RELEASE+" · "+Build.MANUFACTURER+" "+Build.MODEL+"\n\nInstalled APK files: "+size(c,apkBytes(c)));
        try{
            StorageStatsManager manager=c.getSystemService(StorageStatsManager.class);
            StorageStats stats=manager.queryStatsForPackage(c.getApplicationInfo().storageUuid,c.getPackageName(),Process.myUserHandle());
            out.append("\nAndroid app size: ").append(size(c,stats.getAppBytes()));
            out.append("\nUser data (includes cache): ").append(size(c,stats.getDataBytes()));
            out.append("\nCache: ").append(size(c,stats.getCacheBytes()));
            if(Build.VERSION.SDK_INT>=35){
                out.append("\nCompiled/runtime files*: ").append(size(c,stats.getAppBytesByDataType(StorageStats.APP_DATA_TYPE_FILE_TYPE_DEXOPT_ARTIFACT)));
                out.append("\n*May overlap app, data and cache totals; do not add them together.");
            }
        }catch(Exception e){out.append("\nAndroid storage totals unavailable. Check Settings → Apps → Marginalia → Storage.");}
        out.append("\n\nNo repositories or books are downloaded. Source links open in your browser. This report does not clear or modify anything.");return out.toString();
    }
    private static String size(Context c,long bytes){return Formatter.formatFileSize(c,bytes)+" ("+bytes+" bytes)";}
}

package app.marginalia;

import android.content.Context;
import android.content.pm.PackageInfo;

final class BuildInfo {
    static String label(Context context){
        try{PackageInfo info=context.getPackageManager().getPackageInfo(context.getPackageName(),0);return info.versionName+" · build "+info.versionCode;}
        catch(Exception e){return "Build unavailable";}
    }
}

package app.marginalia;

import android.app.*;
import android.app.usage.*;
import android.content.Context;
import android.os.*;
import org.json.*;
import java.io.File;

/** Read-only probe: no preference clearing, widget creation or notification actions. */
public final class StorageProbe extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    private void files(File root,File file,JSONArray result,int depth)throws Exception{
        if(depth>4||result.length()>100||!file.getCanonicalPath().startsWith(root.getCanonicalPath()+File.separator))return;
        JSONObject entry=new JSONObject().put("path",root.toURI().relativize(file.toURI()).toString());
        if(file.isDirectory()){
            File[] children=file.listFiles();entry.put("directory",true).put("readable",children!=null);result.put(entry);
            if(children!=null)for(File child:children)files(root,child,result,depth+1);
        }else result.put(entry.put("bytes",file.length()).put("readable",file.canRead()));
    }
    @Override public void onStart(){
        Bundle out=new Bundle();
        try{
            Context c=getTargetContext();JSONObject report=new JSONObject();
            report.put("api",Build.VERSION.SDK_INT).put("model",Build.MODEL).put("build",BuildInfo.label(c)).put("uid",android.os.Process.myUid()).put("gids",new JSONArray(c.getPackageManager().getPackageGids(c.getPackageName()))).put("apk_bytes",StorageReport.apkBytes(c));
            StorageStatsManager manager=c.getSystemService(StorageStatsManager.class);JSONArray samples=new JSONArray();
            for(int i=0;i<3;i++){
                StorageStats stats=manager.queryStatsForPackage(c.getApplicationInfo().storageUuid,c.getPackageName(),android.os.Process.myUserHandle());
                samples.put(new JSONObject().put("app_bytes",stats.getAppBytes()).put("data_bytes",stats.getDataBytes()).put("cache_bytes",stats.getCacheBytes()));
            }
            report.put("samples",samples);JSONArray code=new JSONArray();File root=new File(c.getApplicationInfo().sourceDir).getParentFile();File[] list=root.listFiles();
            report.put("code_directory_readable",list!=null);if(list!=null)for(File f:list)files(root,f,code,0);report.put("code_files",code);
            report.put("volume_available_bytes",new StatFs(c.getFilesDir().getAbsolutePath()).getAvailableBytes());
            out.putString("stream","\nSTORAGE_PROBE "+report.toString()+"\n");finish(Activity.RESULT_OK,out);
        }catch(Exception e){out.putString("stream",android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,out);}
    }
}

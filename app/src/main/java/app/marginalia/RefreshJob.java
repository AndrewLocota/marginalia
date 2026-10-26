package app.marginalia;

import android.app.job.*;

public final class RefreshJob extends JobService {
    @Override public boolean onStartJob(JobParameters params) { Sync.refresh(this,()->jobFinished(params,false)); return true; }
    @Override public boolean onStopJob(JobParameters params) { return true; }
}

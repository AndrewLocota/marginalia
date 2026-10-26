package app.marginalia;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.KeyguardManager;
import android.appwidget.AppWidgetManager;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityManager;
import java.util.Random;

/** A brief transparent tap effect, with no overlay permission or background loop.
 * The launcher renders the widget recoil; this window renders only the particles. */
public final class BurstActivity extends Activity {
    private BurstView burst;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);overridePendingTransition(0,0);
        KeyguardManager keyguard=(KeyguardManager)getSystemService(KEYGUARD_SERVICE);
        AccessibilityManager accessibility=(AccessibilityManager)getSystemService(ACCESSIBILITY_SERVICE);
        if(keyguard.isKeyguardLocked()||!ValueAnimator.areAnimatorsEnabled()||accessibility.isTouchExplorationEnabled()){finish();return;}
        Repository repo=new Repository(this);Quote quote=null;
        String id=getIntent().getStringExtra("quote_id");
        for(Quote candidate:repo.all())if(candidate.id.equals(id)){quote=candidate;break;}
        if(quote==null||(!getIntent().getBooleanExtra("preview",false)&&!repo.prefs.getBoolean("widget_burst",true))){finish();return;}
        Window window=getWindow();window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setStatusBarColor(Color.TRANSPARENT);window.setNavigationBarColor(Color.TRANSPARENT);
        if(Build.VERSION.SDK_INT>=29){window.setStatusBarContrastEnforced(false);window.setNavigationBarContrastEnforced(false);}
        if(Build.VERSION.SDK_INT>=30)window.setDecorFitsSystemWindows(false);
        else window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        Rect bounds=getIntent().getSourceBounds();
        burst=new BurstView(bounds,AuthorEmoji.forAuthor(this,quote.author));setContentView(burst);
        QuoteWidget.recoil(this,getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID));
    }
    @Override public void finish(){super.finish();overridePendingTransition(0,0);}
    @Override protected void onPause(){super.onPause();if(!isFinishing())finish();}
    @Override protected void onDestroy(){if(burst!=null)burst.stop();super.onDestroy();}
    private final class BurstView extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Rect source;
        private final String[] emoji=new String[BurstMotion.COUNT];
        private BurstMotion.Particle[] particles=new BurstMotion.Particle[0];
        private final Random random=new Random();
        private final ValueAnimator animator=ValueAnimator.ofFloat(0,1);
        private final float density=getResources().getDisplayMetrics().density;
        private float progress,safeTop;
        private boolean started;
        BurstView(Rect bounds,String[] palette){
            super(BurstActivity.this);source=bounds==null?null:new Rect(bounds);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            paint.setTextAlign(Paint.Align.CENTER);
            int phase=random.nextInt(palette.length);
            for(int i=0;i<emoji.length;i++)emoji[i]=palette[(i+phase)%palette.length];
            for(int i=emoji.length-1;i>0;i--){int j=random.nextInt(i+1);String swap=emoji[i];emoji[i]=emoji[j];emoji[j]=swap;}
            animator.setStartDelay(BurstMotion.DELAY_MS);
            animator.setDuration(BurstMotion.DURATION_MS);animator.setInterpolator(new android.view.animation.LinearInterpolator());
            animator.addUpdateListener(value->{progress=(float)value.getAnimatedValue();invalidate();});
            animator.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator a){if(!isFinishing())finish();}});
            setOnClickListener(v->finish());
        }
        @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
            int[] location=new int[2];getLocationOnScreen(location);
            Rect area=source==null?new Rect(w/4,h/3,w*3/4,h/2):new Rect(source);
            if(source!=null)area.offset(-location[0],-location[1]);
            if(!area.intersect(0,0,w,h))area.set(w/4,h/3,w*3/4,h/2);
            WindowInsets insets=getRootWindowInsets();
            safeTop=insets==null?24*density:insets.getSystemWindowInsetTop();
            float safeBottom=insets==null?24*density:insets.getSystemWindowInsetBottom();
            particles=BurstMotion.plan(area.left,area.top-safeTop,area.right,area.bottom-safeTop,w,h-safeTop-safeBottom,density,random);
            if(!started){started=true;animator.start();}
        }
        @Override protected void onDraw(Canvas canvas){
            if(progress<=0||particles.length==0)return;
            for(int i=0;i<particles.length;i++){
                BurstMotion.Particle particle=particles[i];
                float t=particle.progress(progress);
                if(t<=0||t>=1)continue;
                paint.setAlpha(Math.round(255*particle.alpha(t)));
                canvas.save();canvas.translate(particle.xAt(t),safeTop+particle.yAt(t));
                canvas.rotate(particle.tilt(t));
                paint.setTextSize(particle.size*particle.scale(t));
                float baseline=-(paint.ascent()+paint.descent())*.5f;
                canvas.drawText(emoji[i],0,baseline,paint);canvas.restore();
            }
        }
        void stop(){animator.removeAllListeners();animator.cancel();}
    }
}

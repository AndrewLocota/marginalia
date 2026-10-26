package app.marginalia;

import java.util.Random;

/** A staggered stream rising from the lower edge, with bounded, buoyant drift. */
final class BurstMotion {
    static final long RECOIL_MS=260, DELAY_MS=RECOIL_MS+30, DURATION_MS=1400;
    static final int COUNT=8;
    static final class Particle {
        final float x,y,dx,dy,size,rotation,delay,life,sway,phase,margin,width;
        Particle(float x,float y,float dx,float dy,float size,float rotation,float delay,float life,float sway,float phase,float margin,float width){
            this.x=x;this.y=y;this.dx=dx;this.dy=dy;this.size=size;this.rotation=rotation;
            this.delay=delay;this.life=life;this.sway=sway;this.phase=phase;this.margin=margin;this.width=width;
        }
        float progress(float clock){return Math.max(0,Math.min(1,(clock-delay)/life));}
        float xAt(float t){
            float drift=dx*t+sway*(float)Math.sin(Math.PI*t)*(float)Math.sin(phase+t*Math.PI*1.6);
            return Math.max(margin,Math.min(width-margin,x+drift));
        }
        float yAt(float t){return y+dy*(.8f*t+.2f*t*t);}
        float alpha(float t){return Math.min(1,t/.13f)*Math.min(1,(1-t)/.32f);}
        float scale(float t){float enter=Math.min(1,t/.18f);return .5f+.5f*(1-(1-enter)*(1-enter));}
        float tilt(float t){return rotation*(float)Math.sin(phase+t*Math.PI*1.5)*(float)Math.sin(Math.PI*t);}
    }
    static Particle[] plan(float left,float top,float right,float bottom,float width,float height,float density,Random random){
        float margin=20*density;
        float minX=Math.max(margin,left+margin),maxX=Math.min(width-margin,right-margin);
        float originY=Math.min(height-margin,bottom+15*density);
        if(maxX<=minX||originY-margin<18*density)return new Particle[0];
        int capacity=Math.max(1,Math.min(COUNT,(int)((maxX-minX)/(32*density))));
        int count=Math.max(1,capacity-(capacity>3?random.nextInt(2):0));
        Particle[] result=new Particle[count];
        for(int i=0;i<count;i++){
            // Scatter within loose lanes, then stagger emission so it never reads as a row.
            float t=(i+.2f+random.nextFloat()*.6f)/count;
            float x=minX+(maxX-minX)*t;
            float rise=Math.min(originY-margin,(90+random.nextFloat()*80)*density);
            float size=(18+random.nextFloat()*8)*density;
            float delay=random.nextFloat()*.22f;
            float life=.62f+random.nextFloat()*.15f;
            result[i]=new Particle(x,originY,(random.nextFloat()-.5f)*44*density,-rise,size,
                8+random.nextFloat()*9,delay,life,(5+random.nextFloat()*8)*density,
                random.nextFloat()*(float)(Math.PI*2),margin,width);
        }
        return result;
    }
}

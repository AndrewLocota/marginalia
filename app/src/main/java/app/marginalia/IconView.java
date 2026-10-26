package app.marginalia;

import android.content.Context;
import android.graphics.*;
import android.view.View;

final class IconView extends View {
    private final String name; private final int color;
    IconView(Context c,String name,int color) { super(c);this.name=name;this.color=color; }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); canvas.save(); float s=Math.min(getWidth(),getHeight())/24f;
        canvas.translate((getWidth()-24*s)/2,(getHeight()-24*s)/2); canvas.scale(s,s);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); p.setColor(color);p.setStrokeWidth(1.65f);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
        Path path=new Path();
        switch(name) {
            case "today": canvas.drawRoundRect(4,3,20,21,2,2,p);canvas.drawLine(8,8,16,8,p);canvas.drawLine(8,12,16,12,p);canvas.drawLine(8,16,13,16,p);break;
            case "library":canvas.drawRoundRect(3,4,8,21,1,1,p);canvas.drawRoundRect(10,4,15,21,1,1,p);canvas.drawLine(18,4,22,20,p);break;
            case "saved":case "save":path.moveTo(6,21);path.lineTo(6,3);path.lineTo(18,3);path.lineTo(18,21);path.lineTo(12,17);path.close();canvas.drawPath(path,p);break;
            case "screens":canvas.drawRoundRect(6,2,18,22,3,3,p);canvas.drawLine(10,18,14,18,p);break;
            case "share":path.moveTo(12,15);path.lineTo(12,2);path.moveTo(7,7);path.lineTo(12,2);path.lineTo(17,7);path.moveTo(5,11);path.lineTo(5,21);path.lineTo(19,21);path.lineTo(19,11);canvas.drawPath(path,p);break;
            case "next":path.moveTo(5,12);path.lineTo(20,12);path.moveTo(14,6);path.lineTo(20,12);path.lineTo(14,18);canvas.drawPath(path,p);break;
            case "sun":canvas.drawCircle(12,12,4,p);for(int i=0;i<8;i++){double a=i*Math.PI/4;canvas.drawLine(12+(float)Math.cos(a)*8,12+(float)Math.sin(a)*8,12+(float)Math.cos(a)*10,12+(float)Math.sin(a)*10,p);}break;
            case "moon":path.moveTo(19,16);path.cubicTo(8,19,4,8,11,3);path.cubicTo(0,3,0,21,12,21);path.cubicTo(16,21,19,18,19,16);canvas.drawPath(path,p);break;
            case "info":canvas.drawCircle(12,12,9,p);canvas.drawLine(12,11,12,17,p);canvas.drawPoint(12,7,p);break;
        }
        canvas.restore();
    }
}

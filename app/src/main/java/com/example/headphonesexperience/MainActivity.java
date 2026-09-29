package com.example.headphonesexperience;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.content.*;
import java.util.Random;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        setContentView(new CoinView(this));
    }

    static class CoinView extends View {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Random random = new Random();
        float coinX, coinY, rotation, scaleX=1f;
        long start, duration;
        boolean flipping=false;
        float drift, spin, wobble;
        RectF button = new RectF();

        CoinView(Context c) {
            super(c);
            setBackgroundColor(Color.WHITE);
            p.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        void resetCoin() {
            coinX=getWidth()/2f;
            coinY=Math.max(150,getHeight()/2f-30);
            rotation=0;
            scaleX=1f;
        }

        @Override protected void onSizeChanged(int w,int h,int ow,int oh) {
            resetCoin();
            button.set(w/2f-70,h/2f+100,w/2f+70,h/2f+152);
        }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            if (!flipping) resetCoin();
            drawCoin(c,coinX,coinY,rotation,scaleX);
            p.setColor(Color.rgb(20,20,20));
            c.drawRoundRect(button,26,26,p);
            p.setColor(Color.WHITE);
            p.setTextSize(16);
            p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
            p.setTextAlign(Paint.Align.CENTER);
            c.drawText("FLIP",getWidth()/2f,button.centerY()+6,p);
            if(flipping) {
                long elapsed=System.currentTimeMillis()-start;
                float t=Math.min(1f,elapsed/(float)duration);
                float ease=1-(float)Math.pow(1-t,3);
                float arc=(float)Math.sin(Math.PI*t);
                coinX=getWidth()/2f + drift*ease;
                coinY=Math.max(95,getHeight()/2f-30 - 185*arc + 55*t);
                rotation=spin*ease;
                float depth=(float)Math.abs(Math.cos(rotation*Math.PI/180f));
                scaleX=Math.max(.10f,depth);
                float wob=(float)Math.sin(t*Math.PI*4)*wobble*(1-t);
                coinX+=wob;
                drawCoin(c,coinX,coinY,rotation,scaleX);
                if(t>=1f){ flipping=false; invalidate(); }
                else invalidate();
            }
        }

        void drawCoin(Canvas c,float x,float y,float rot,float sx) {
            c.save();
            c.translate(x,y);
            c.rotate(rot);
            c.scale(sx,1f);
            float r=58;
            p.setShadowLayer(8,0,5,0x30000000);
            p.setShader(new LinearGradient(-r,-r,r,r,Color.rgb(198,153,245),Color.rgb(108,48,174),Shader.TileMode.CLAMP));
            c.drawCircle(0,0,r,p);
            p.clearShadowLayer();
            p.setShader(null);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(3);
            p.setColor(Color.rgb(145,82,205));
            c.drawCircle(0,0,r-2,p);
            p.setStrokeWidth(1.5f);
            p.setColor(0x80FFFFFF);
            c.drawCircle(0,0,r-8,p);
            p.setStyle(Paint.Style.FILL);

            // Stylized raised Lincoln profile: forehead, nose, lips, chin, neck and coat.
            Path head=new Path();
            head.moveTo(-7,-38);
            head.cubicTo(8,-45,22,-35,24,-20);
            head.cubicTo(25,-12,31,-7,35,-2);
            head.cubicTo(30,1,25,3,20,5);
            head.cubicTo(25,10,21,13,14,13);
            head.cubicTo(13,23,9,30,1,34);
            head.lineTo(-17,34);
            head.cubicTo(-15,24,-11,18,-5,14);
            head.cubicTo(-17,9,-21,-3,-18,-14);
            head.cubicTo(-16,-26,-12,-35,-7,-38);
            head.close();
            p.setColor(0xFF7B3EB9);
            c.drawPath(head,p);
            Path hair=new Path();
            hair.moveTo(-18,-14); hair.cubicTo(-24,-27,-17,-43,-4,-47);
            hair.cubicTo(8,-50,19,-45,25,-35);
            hair.cubicTo(15,-39,5,-39,-4,-35);
            hair.close();
            p.setColor(0xFF6930A5); c.drawPath(hair,p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2);
            p.setColor(0xFFB985E7);
            Path collar=new Path();
            collar.moveTo(-16,34); collar.lineTo(-2,24); collar.lineTo(8,36);
            collar.moveTo(8,36); collar.lineTo(15,24); collar.lineTo(25,34);
            c.drawPath(collar,p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFF8E51C9);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTypeface(Typeface.create("serif",Typeface.BOLD));
            p.setTextSize(11);
            c.drawText("ONE CENT",0,48,p);
            c.restore();
        }

        @Override public boolean onTouchEvent(android.view.MotionEvent e) {
            if(e.getAction()==MotionEvent.ACTION_UP && button.contains(e.getX(),e.getY()) && !flipping) {
                flipping=true;
                start=System.currentTimeMillis();
                duration=1100+random.nextInt(700);
                drift=(random.nextFloat()-.5f)*150f;
                spin=(5+random.nextInt(5))*360f*(random.nextBoolean()?1:-1);
                wobble=random.nextBoolean()?random.nextFloat()*45f:0f;
                invalidate();
                return true;
            }
            return true;
        }
    }
}
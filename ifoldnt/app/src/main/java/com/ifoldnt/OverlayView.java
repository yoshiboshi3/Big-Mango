package com.ifoldnt;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

final class OverlayView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean drawFade;
    private boolean drawBevel;
    private boolean fadeLeft;
    private float fadeInternalAlpha;
    private int cutPx;

    OverlayView(Context context) {
        super(context);
        paint.setColor(Color.BLACK);
        setBackgroundColor(Color.TRANSPARENT);
        setWillNotDraw(false);
    }

    void setEffect(boolean fade, boolean left, float internalAlpha, boolean bevel, int cut) {
        drawFade = fade;
        fadeLeft = left;
        fadeInternalAlpha = Math.max(0f, Math.min(1f, internalAlpha));
        drawBevel = bevel;
        cutPx = Math.max(0, cut);
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        if (drawFade && fadeInternalAlpha > 0f) {
            paint.setAlpha(Math.round(255f * fadeInternalAlpha));
            if (fadeLeft) canvas.drawRect(0, 0, w / 2f, h, paint);
            else canvas.drawRect(w / 2f, 0, w, h, paint);
        }

        if (drawBevel && cutPx > 0) {
            paint.setAlpha(255);
            float c = Math.min(cutPx, Math.min(w, h) * 0.18f);
            drawTri(canvas, 0, 0, c, 0, 0, c);
            drawTri(canvas, w, 0, w - c, 0, w, c);
            drawTri(canvas, 0, h, c, h, 0, h - c);
            drawTri(canvas, w, h, w - c, h, w, h - c);
        }
    }

    private void drawTri(Canvas c, float x1, float y1, float x2, float y2, float x3, float y3) {
        Path p = new Path();
        p.moveTo(x1, y1); p.lineTo(x2, y2); p.lineTo(x3, y3); p.close();
        c.drawPath(p, paint);
    }
}

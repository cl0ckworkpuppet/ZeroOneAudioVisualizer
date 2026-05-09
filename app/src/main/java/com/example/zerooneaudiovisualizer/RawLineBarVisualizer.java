package com.example.zerooneaudiovisualizer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class RawLineBarVisualizer extends View {
    private final Paint paint = new Paint();
    private int density = 70;
    private float[] barHeights;
    private static final float DECAY = 0.85f; // How fast the bars fall
    private static final float SMOOTHING = 0.4f; // How fast the bars rise
    private static final float SENSITIVITY = 1.5f; // Adjust this for overall responsiveness

    public RawLineBarVisualizer(Context context) {
        super(context);
        init();
    }

    public RawLineBarVisualizer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAntiAlias(true);
        barHeights = new float[density];
    }

    public void setColor(int color) {
        paint.setColor(color);
    }

    public void setDensity(int density) {
        this.density = density;
        this.barHeights = new float[density];
    }

    public void onRawData(byte[] data) {
        if (data == null || data.length < 2) return;

        // Convert 16-bit PCM bytes to shorts
        short[] samples = new short[data.length / 2];
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples);

        int samplesPerBar = samples.length / density;
        if (samplesPerBar == 0) return;

        for (int i = 0; i < density; i++) {
            float maxAmplitude = 0;
            int start = i * samplesPerBar;
            int end = Math.min(start + samplesPerBar, samples.length);

            // Find peak amplitude in this bar's bucket
            for (int j = start; j < end; j++) {
                float val = Math.abs(samples[j]);
                if (val > maxAmplitude) maxAmplitude = val;
            }

            // Normalize (16-bit max is 32768)
            float targetHeight = (maxAmplitude / 32768f) * SENSITIVITY;
            if (targetHeight > 1.0f) targetHeight = 1.0f;

            // Apply smoothing: rise quickly, fall slowly
            if (targetHeight > barHeights[i]) {
                barHeights[i] = barHeights[i] + (targetHeight - barHeights[i]) * SMOOTHING;
            } else {
                barHeights[i] *= DECAY;
            }
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        float barWidth = width / density;
        float spacing = barWidth * 0.2f;
        paint.setStrokeWidth(barWidth - spacing);

        for (int i = 0; i < density; i++) {
            float barHeight = barHeights[i] * height;
            
            // Keep a tiny minimum height so the bar is always visible
            if (barHeight < 4f) barHeight = 4f;

            float x = i * barWidth + barWidth / 2;
            float yStart = (height - barHeight) / 2;
            float yEnd = (height + barHeight) / 2;

            canvas.drawLine(x, yStart, x, yEnd, paint);
        }
    }

    public void release() {
        for (int i = 0; i < density; i++) {
            barHeights[i] = 0;
        }
        invalidate();
    }
}
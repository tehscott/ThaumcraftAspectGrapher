package com.stromberg.scott.thaumcraftaspectgrapher;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

import java.text.DecimalFormat;

public class DrawAreaThread extends Thread {
    private final static int 	MAX_FPS = 50;
    private final static int	MAX_FRAME_SKIPS = 15;
    private final static int	FRAME_PERIOD = 1000 / MAX_FPS;

    private DecimalFormat df = new DecimalFormat("0.##");
    private final static int 	STAT_INTERVAL = 1000;
    private final static int	FPS_HISTORY_NR = 10;
    private long statusIntervalTimer	= 0L;

    private int frameCountPerStatCycle = 0;
    private double 	fpsStore[];
    private long 	statsCount = 0;
    private long lastStatusStore = 0;

    private SurfaceHolder surfaceHolder;
    private DrawArea drawArea;

    private boolean running;

    public void setRunning(boolean running) {
        this.running = running;
    }

    public DrawAreaThread(SurfaceHolder surfaceHolder, DrawArea drawArea) {
        super();
        this.surfaceHolder = surfaceHolder;
        this.drawArea = drawArea;
    }

    @Override
    public void run() {
        Canvas canvas;
        initTimingElements();

        long beginTime;
        long timeDiff;
        int sleepTime;
        int framesSkipped;

        while (running) {
            canvas = null;
            try {
                canvas = this.surfaceHolder.lockCanvas();
                synchronized (surfaceHolder) {
                    beginTime = System.currentTimeMillis();
                    framesSkipped = 0;
                    this.drawArea.render(canvas);
                    timeDiff = System.currentTimeMillis() - beginTime;
                    sleepTime = (int)(FRAME_PERIOD - timeDiff);

                    if (sleepTime > 0) {
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {}
                    }

                    while (sleepTime < 0 && framesSkipped < MAX_FRAME_SKIPS) {
                        sleepTime += FRAME_PERIOD;
                        framesSkipped++;
                    }

                    storeStats();
                }
            } finally {
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    private void storeStats() {
        frameCountPerStatCycle++;
        statusIntervalTimer += (System.currentTimeMillis() - statusIntervalTimer);

        if (statusIntervalTimer >= lastStatusStore + STAT_INTERVAL) {
            double actualFps = (double)(frameCountPerStatCycle / (STAT_INTERVAL / 1000));
            fpsStore[(int) statsCount % FPS_HISTORY_NR] = actualFps;

            statsCount++;

            double totalFps = 0.0;
            for (int i = 0; i < FPS_HISTORY_NR; i++) {
                totalFps += fpsStore[i];
            }

            double averageFps = 0.0;
            if (statsCount < FPS_HISTORY_NR) {
                averageFps = totalFps / statsCount;
            } else {
                averageFps = totalFps / FPS_HISTORY_NR;
            }

            statusIntervalTimer = 0;
            frameCountPerStatCycle = 0;

            statusIntervalTimer = System.currentTimeMillis();
            lastStatusStore = statusIntervalTimer;
            drawArea.setAvgFps("FPS: " + df.format(averageFps));
        }
    }

    private void initTimingElements() {
        fpsStore = new double[FPS_HISTORY_NR];
        for (int i = 0; i < FPS_HISTORY_NR; i++) {
            fpsStore[i] = 0.0;
        }
    }
}

// TODO: https://github.com/Clans/FloatingActionButton/issues/61

package com.stromberg.scott.thaumcraftaspectgrapher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Typeface;
import android.os.Vibrator;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.util.HashMap;

public class DrawArea extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = DrawArea.class.getSimpleName();
    private final int DRAG_THRESHOLD = 10;
    private final int HORIZONTAL_ASPECT_COUNT = 14;
    private final int VERTICAL_ASPECT_COUNT = 8;

    public static boolean DEBUG = false;

    private float xOffset = 0;
    private float yOffset = 0;
    private float lastX = 0;
    private float lastY = 0;
    private float lastDownX = 0;
    private float lastDownY = 0;

    private boolean isDragging = false;
    private boolean isInLinkingMode = false;
    private boolean isInMargin = false;

    private DrawAreaThread thread;

    private String avgFps = "";
    public void setAvgFps(String avgFps) {
        this.avgFps = avgFps;
    }

    private HashMap<Integer, Bitmap> aspectImages = new HashMap<>();

    private Bitmap aspectBackgroundBitmap;
    private Bitmap aspectBackgroundSelectedBitmap;

    public DrawArea(Context context, AttributeSet attrs) {
        super(context);

        if(!isInEditMode()) {
            getHolder().addCallback(this);
            setFocusable(true);

            this.setOnTouchListener(new OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    onTouchEvent(event);

                    return true;
                }
            });

            setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    handleLongPress();

                    return true;
                }
            });

            aspectBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.aspect_background);
            aspectBackgroundSelectedBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.aspect_background_selected);
        }
    }

    private void handleLongPress() {
        if(!isDragging && !isInLinkingMode) {
            setSelectedAspect(lastX, lastY);

            if (MainActivity.selectedAspect != null) {
                isInLinkingMode = true;

                ((Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(50);
            }
        }
    }

    public DrawArea(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        final int action = event.getActionMasked();

        float x = event.getX();
        float y = event.getY();

        switch(action) {
            case MotionEvent.ACTION_DOWN:
                handleDown(x, y);

                break;

            case MotionEvent.ACTION_UP:
                handleUp(x, y);

                break;

            case MotionEvent.ACTION_MOVE:
                handleMove(x, y);

                break;
        }

        return true;
    }

    private void handleMove(float x, float y) {
        if(Math.abs(x - lastDownX) > DRAG_THRESHOLD || Math.abs(y - lastDownY) > DRAG_THRESHOLD) {
            if(isInLinkingMode) {
                MainActivity.selectedLinkingModeAspect = getAspectAt(x, y);

                int widthMargin = (int) (getWidth() * .15);
                int heightMargin = (int) (getHeight() * .15);
                isInMargin = x <= widthMargin || x >= (getWidth() - widthMargin) || y <= heightMargin || y >= (getHeight() - heightMargin);
            } else {
                isDragging = true;

                xOffset += (lastX - x);
                yOffset += (lastY - y);

                fixOffset();
            }

            lastX = x;
            lastY = y;
        }
    }

    private void fixOffset() {
        float canvasWidth = (MainActivity.defaultAspectBackgroundWidth * HORIZONTAL_ASPECT_COUNT) +
                (Aspect.horizontalSpacing * (HORIZONTAL_ASPECT_COUNT - 1)) +
                (2 * MainActivity.horizontalPadding);
        float canvasHeight = (MainActivity.defaultAspectBackgroundHeight * VERTICAL_ASPECT_COUNT) +
                (Aspect.verticalSpacing * (VERTICAL_ASPECT_COUNT - 1)) +
                (2 * MainActivity.verticalPadding);

        float maxWidth = Math.abs(canvasWidth - getWidth());
        float maxHeight = Math.abs(canvasHeight - getHeight());

        if (xOffset <= 0) {
            xOffset = 0;
        }

        if (yOffset <= 0) {
            yOffset = 0;
        }

        if (xOffset >= maxWidth) {
            xOffset = maxWidth;
        }

        if (yOffset >= maxHeight) {
            yOffset = maxHeight;
        }
    }

    private void handleUp(float x, float y) {
        lastX = 0;
        lastY = 0;
        lastDownX = 0;
        lastDownY = 0;

        if(isInLinkingMode) {
            MainActivity.selectedLinkingModeAspect = getAspectAt(x, y);

            if(MainActivity.selectedAspect != null && MainActivity.selectedLinkingModeAspect != null && !MainActivity.selectedAspect.equals(MainActivity.selectedLinkingModeAspect)) {
                ((MainActivity) getContext()).createAspectLink(MainActivity.selectedAspect, MainActivity.selectedLinkingModeAspect);
            } else {
                MainActivity.selectedAspect = null;
                ((MainActivity)getContext()).toggleAspectMenu(null, false);
            }
        }

        if(!isDragging && !isInLinkingMode) {
            setSelectedAspect(x, y);
        }

        MainActivity.selectedLinkingModeAspect = null;

        isInLinkingMode = false;
        isDragging = false;
    }

    private void setSelectedAspect(float x, float y) {
        ((MainActivity)getContext()).toggleAspectMenu(null, false);

        MainActivity.selectedAspect = getAspectAt(x, y);
        if(MainActivity.selectedAspect != null) {
            ((MainActivity)getContext()).toggleAspectMenu(MainActivity.selectedAspect, true);
        }
    }

    private Aspect getAspectAt(float x, float y) {
        for (Aspect graphAspect : MainActivity.graphAspects) {
            if (graphAspect.isInBounds(x + xOffset, y + yOffset)) {
                return graphAspect;
            }
        }

        return null;
    }

    private void handleDown(float x, float y) {
        lastX = x;
        lastY = y;
        lastDownX = x;
        lastDownY = y;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    public void surfaceCreated(SurfaceHolder holder) {
        thread = new DrawAreaThread(getHolder(), this);
        thread.setRunning(true);
        thread.start();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        while (retry) {
            try {
                thread.setRunning(false);
                thread.join();
                retry = false;
            } catch (InterruptedException e) {}
        }
    }

	/*
	 * DRAW METHODS
	 */
    public void render(Canvas canvas) {
        if(canvas != null) {
            Paint paint = new Paint();
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            canvas.drawPaint(paint);
            canvas.drawColor(ContextCompat.getColor(getContext(), R.color.background));

            focusOnLastAddedAspect();

            if(isInMargin && isInLinkingMode) {
                int widthMargin = (int) (getWidth() * .10);
                int heightMargin = (int) (getHeight() * .10);

                if(lastX <= widthMargin) {
                    xOffset -= (widthMargin / 2);
                } else if(lastX >= (getWidth() - widthMargin)) {
                    xOffset += (widthMargin / 2);
                } else if(lastY <= heightMargin) {
                    yOffset -= (heightMargin / 2);
                } else if(lastY >= (getHeight() - heightMargin)) {
                    yOffset += (heightMargin / 2);
                }

                fixOffset();
            }

            drawGraph(canvas);

            if(DEBUG) {
                drawDebugInfo(canvas);
            }
        }
    }

    private void focusOnLastAddedAspect() {
        Aspect lastAddedAspect = ((MainActivity) getContext()).lastAddedAspect;
        if(lastAddedAspect != null) {
            if(lastAddedAspect.getX() > 0 && lastAddedAspect.getY() > 0) {
                int middleX = getWidth() / 2;
                int middleY = getHeight() / 2;

                xOffset = lastAddedAspect.getX() - middleX;
                yOffset = lastAddedAspect.getY() - middleY;

                fixOffset();
            }

            ((MainActivity) getContext()).lastAddedAspect = null;
        }
    }

    private void drawDebugInfo(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);

        int pixel = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        paint.setTextSize(pixel);

        String textToDraw = avgFps + "\n" +
                "Offset: " + Math.round(xOffset) + ", " + Math.round(yOffset) + "\n" +
                "Offset (zoom): " + Math.round(xOffset) + ", " + Math.round(yOffset);

        float y = paint.descent() - paint.ascent();
        for (String line : textToDraw.split("\n")) {
            float textWidth = paint.measureText(line);
            float x = this.getWidth() - textWidth - 10;
            canvas.drawText(line, x, y, paint);
            y += paint.descent() - paint.ascent();
        }
    }

    private void drawGraph(Canvas canvas) {
        synchronized (MainActivity.graphAspects) {
            for (Aspect aspect : MainActivity.graphAspects) {
                drawAspectLinks(aspect, canvas);
            }

            for (Aspect aspect : MainActivity.graphAspects) {
                drawAspect(aspect, canvas);
            }

            if(isInLinkingMode) {
                drawLinkingModeLink(canvas);
            }
        }
    }

    private void drawAspect(Aspect aspect, Canvas canvas) {
        Bitmap aspectBitmap = aspectImages.get(aspect.getId());
        Bitmap background = aspectBackgroundBitmap;

        if(aspectBitmap == null) {
            aspectBitmap = BitmapFactory.decodeResource(getResources(), aspect.getImageResourceId());
            aspectImages.put(aspect.getId(), aspectBitmap);
        }

        Paint paint = new Paint();

        if(MainActivity.selectedAspect != null && MainActivity.selectedAspect.equals(aspect)) {
            background = aspectBackgroundSelectedBitmap;
        } else if(MainActivity.selectedLinkingModeAspect != null && MainActivity.selectedLinkingModeAspect.equals(aspect)) {
            background = aspectBackgroundSelectedBitmap;
        }

        if(MainActivity.selectedAspect != null && !MainActivity.selectedAspect.equals(aspect)
                && !MainActivity.selectedAspect.getLinkedAspectIds().contains(aspect.getId())) {
            paint.setAlpha(127);
        }

        canvas.drawBitmap(background, aspect.getX() - xOffset - (aspectBitmap.getWidth() / 2), aspect.getY() - yOffset - (aspectBitmap.getHeight() / 2), paint);
        canvas.drawBitmap(aspectBitmap, aspect.getX() - xOffset, aspect.getY() - yOffset, paint);

        if(MainActivity.selectedAspect != null && MainActivity.selectedAspect.equals(aspect)
                || MainActivity.selectedLinkingModeAspect != null && MainActivity.selectedLinkingModeAspect.equals(aspect)) {
            drawAspectNameTag(aspect, canvas);
        }
    }

    private void drawAspectNameTag(Aspect aspect, Canvas canvas) {
        Paint paint = new Paint();
        int pixel = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18, getResources().getDisplayMetrics());
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(pixel);

        float textWidth = paint.measureText(aspect.getName());
        float textHeight = paint.descent() - paint.ascent();

        float x = aspect.getX() - xOffset;
        float y = aspect.getY() - yOffset;

        x = x + (aspect.getWidth() / 2) - (textWidth / 2);
        y = y + (aspect.getHeight() / 2) - (textHeight * 1.75f);

        canvas.drawText(aspect.getName(), x, y, paint);
    }

    private void drawLinkingModeLink(Canvas canvas) {
        if(MainActivity.selectedAspect != null) {
            float startX = MainActivity.selectedAspect.getX() - xOffset + (MainActivity.defaultAspectWidth / 2);
            float startY = MainActivity.selectedAspect.getY() - yOffset + (MainActivity.defaultAspectHeight / 2);

            Paint paint = new Paint();
            paint.setStrokeWidth(15f);
            paint.setAlpha(50);
            canvas.drawLine(startX, startY, lastX, lastY, paint);
        }
    }

    private void drawAspectLinks(Aspect aspect, Canvas canvas) {
        if(aspect.getLinkedAspectIds() != null) {
            synchronized (aspect.getLinkedAspectIds()) {
                float startX, startY;
                Paint paint = new Paint();
                paint.setStrokeWidth(15f);
                paint.setAlpha(50);

                startX = aspect.getX() - xOffset + (MainActivity.defaultAspectWidth / 2);
                startY = aspect.getY() - yOffset + (MainActivity.defaultAspectHeight / 2);

                for (Integer linkedAspectId : aspect.getLinkedAspectIds()) {
                    float endX, endY;

                    Aspect linkedAspect = MainActivity.getAspectById(linkedAspectId);
                    endX = linkedAspect.getX() - xOffset + (MainActivity.defaultAspectWidth / 2);
                    endY = linkedAspect.getY() - yOffset + (MainActivity.defaultAspectHeight / 2);

                    if (MainActivity.selectedAspect != null && !MainActivity.selectedAspect.equals(aspect)
                            && !MainActivity.selectedAspect.getLinkedAspectIds().contains(aspect.getId())) {
                        paint.setAlpha(10);
                    }

                    canvas.drawLine(startX, startY, endX, endY, paint);
                }
            }
        }
    }
}
package com.stromberg.scott.thaumcraft4aspectmapper;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;

public class AspectMapTableLayout extends TableLayout {
    private MainActivity mMainActivity;

    public AspectMapTableLayout(MainActivity mainActivity) {
        super(mainActivity);
        mMainActivity = mainActivity;
        init();
    }

    public AspectMapTableLayout(MainActivity mainActivity, AttributeSet attrs) {
        super(mainActivity, attrs);
        mMainActivity = mainActivity;
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.aspect_map, this);
        setWillNotDraw(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        synchronized (MainActivity.graphAspects) {
            for (Aspect aspect : MainActivity.graphAspects) {
                drawAspectLinks(aspect, canvas);
            }

//            if(isInLinkingMode) {
//                drawLinkingModeLink(canvas);
//            }
        }

        Log.i("drawing", "onDraw: ");

        super.onDraw(canvas);

    }

    private void drawAspectLinks(Aspect aspect, Canvas canvas) {
        if(aspect.getLinkedAspectIds() != null) {
            synchronized (aspect.getLinkedAspectIds()) {
                Paint paint = new Paint();
                paint.setStrokeWidth(15f);
                paint.setAlpha(50);

                View startingAspectButton = mMainActivity.getAspectButtonByAspectName(aspect.getName());

                if(startingAspectButton == null) {
                    Log.e("drawlinks", "failed to draw link from " + aspect.getName());
                }

                int buttonWidth = startingAspectButton.getWidth();
                int buttonHeight = startingAspectButton.getHeight();

                int startX = (int) (((TableRow)startingAspectButton.getParent()).getX() + startingAspectButton.getX() + (buttonWidth / 2));
                int startY = (int) (((TableRow)startingAspectButton.getParent()).getY() + startingAspectButton.getY() + (buttonHeight / 2));

                for (Integer linkedAspectId : aspect.getLinkedAspectIds()) {
                    Aspect linkedAspect = MainActivity.getAspectById(linkedAspectId);
                    View endingAspectButton = mMainActivity.getAspectButtonByAspectName(linkedAspect.getName());

                    if (MainActivity.selectedAspect != null && !MainActivity.selectedAspect.equals(aspect)
                            && !MainActivity.selectedAspect.getLinkedAspectIds().contains(aspect.getId())) {
                        paint.setAlpha(10);
                    }

                    int endX = (int) (((TableRow)endingAspectButton.getParent()).getX() + endingAspectButton.getX() + (buttonWidth / 2));
                    int endY = (int) (((TableRow)endingAspectButton.getParent()).getY() + endingAspectButton.getY() + (buttonHeight / 2));

                    canvas.drawLine(startX, startY, endX, endY, paint);
                }
            }
        }
    }

    private void drawLinkingModeLink(Canvas canvas) {
//        if(MainActivity.selectedAspect != null) {
//            float startX = MainActivity.selectedAspect.getX() - xOffset + (MainActivity.defaultAspectWidth / 2);
//            float startY = MainActivity.selectedAspect.getY() - yOffset + (MainActivity.defaultAspectHeight / 2);
//
//            Paint paint = new Paint();
//            paint.setStrokeWidth(15f);
//            paint.setAlpha(50);
//            canvas.drawLine(startX, startY, lastX, lastY, paint);
//        }
    }
}

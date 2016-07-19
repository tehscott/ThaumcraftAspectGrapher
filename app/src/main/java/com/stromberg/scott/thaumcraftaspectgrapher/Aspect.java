package com.stromberg.scott.thaumcraftaspectgrapher;

import android.support.annotation.DrawableRes;

import com.google.gson.Gson;

import java.util.ArrayList;

public class Aspect {
    public static final int horizontalSpacing = 10;
    public static final int verticalSpacing = 75;

    private int x;
    private int y;
    private int horizontalPosition;
    private int verticalPosition;
    private int width;
    private int height;
    private int id;
    @DrawableRes private int imageResourceId;
    private String name;
    private ArrayList<Integer> linkedAspectIds = new ArrayList<>();

    public Aspect(int id, int imageResourceId, int horizontalPosition, int verticalPosition, String name) {
        this.id = id;
        this.imageResourceId = imageResourceId;
        this.horizontalPosition = horizontalPosition;
        this.verticalPosition = verticalPosition;
        this.name = name;


        this.width = MainActivity.defaultAspectWidth;
        this.height = MainActivity.defaultAspectHeight;
        setPosition();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getImageResourceId() {
        return imageResourceId;
    }

    public void setImageResourceId(int imageResourceId) {
        this.imageResourceId = imageResourceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<Integer> getLinkedAspectIds() {
        return linkedAspectIds;
    }

    public ArrayList<Aspect> getLinkedAspects() {
        ArrayList<Aspect> aspects = new ArrayList<>();
        for (Integer aspectId : getLinkedAspectIds()) {
            aspects.add(MainActivity.getAspectById(aspectId));
        }

        return aspects;
    }

    public int getVerticalPosition() {
        return verticalPosition;
    }

    public void setVerticalPosition(int verticalPosition) {
        this.verticalPosition = verticalPosition;
    }

    public int getHorizontalPosition() {
        return horizontalPosition;
    }

    public void setHorizontalPosition(int horizontalPosition) {
        this.horizontalPosition = horizontalPosition;
    }

    private void setPosition() {
        if(width == 0 || height == 0) {
            throw new RuntimeException("Width and Height must be set first.");
        }

        x = MainActivity.defaultAspectBackgroundWidth + (MainActivity.defaultAspectBackgroundWidth * (horizontalPosition - 1) + horizontalSpacing * (horizontalPosition - 1));
        y = MainActivity.defaultAspectBackgroundHeight + (MainActivity.defaultAspectBackgroundHeight * (verticalPosition - 1) + verticalSpacing * (verticalPosition - 1));
    }

    public boolean isInBounds(float x, float y) {
        return x >= getX() && x <= getX() + MainActivity.defaultAspectBackgroundWidth
                && y >= getY() && y <= getY() + MainActivity.defaultAspectBackgroundHeight;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Aspect) {
            return ((Aspect) o).getId() == getId();
        } else {
            return false;
        }
    }

    public Aspect clone() {
        String cloneJson = new Gson().toJson(this);
        return new Gson().fromJson(cloneJson, Aspect.class);
    }
}

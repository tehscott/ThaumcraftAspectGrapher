package com.stromberg.scott.thaumcraft4aspectmapper;

import com.google.gson.Gson;

import java.util.ArrayList;

public class Aspect {
    private int id;
    private String name;
    private ArrayList<Integer> linkedAspectIds = new ArrayList<>();

    public Aspect(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

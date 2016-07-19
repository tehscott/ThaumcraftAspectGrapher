package com.stromberg.scott.thaumcraftaspectgrapher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Dijkstra {
    private Set<Aspect> settledNodes;
    private Set<Aspect> unSettledNodes;
    private Map<String, Aspect> predecessors;
    private Map<String, Integer> distance;

    public void execute(Aspect source) {
        settledNodes = new HashSet<>();
        unSettledNodes = new HashSet<>();
        distance = new HashMap<>();
        predecessors = new HashMap<>();
        distance.put(source.getName(), 0);
        unSettledNodes.add(source);
        while (unSettledNodes.size() > 0) {
            Aspect node = getMinimum(unSettledNodes);
            settledNodes.add(node);
            unSettledNodes.remove(node);
            findMinimalDistances(node);
        }
    }

    private void findMinimalDistances(Aspect node) {
        List<Aspect> adjacentNodes = getNeighbors(node);
        for (Aspect target : adjacentNodes) {
//            if (getShortestDistance(target) > getShortestDistance(node) + getDistance()) {
                distance.put(target.getName(), getShortestDistance(node) + getDistance());
                predecessors.put(target.getName(), node);
                unSettledNodes.add(target);
//            }
        }
    }

    private int getDistance() {
        return 1; // It seems that ANY weight is necessary, but CANNOT be zero
    }

    private List<Aspect> getNeighbors(Aspect node) {
        List<Aspect> neighbors = new ArrayList<>();

        for (Integer aspectId : node.getLinkedAspectIds()) {
            Aspect linkedAspect = MainActivity.getAspectById(aspectId);

            if (!isSettled(linkedAspect)) {
                neighbors.add(linkedAspect);
            }
        }

        return neighbors;
    }

    private boolean isSettled(Aspect vertex) {
        return settledNodes.contains(vertex);
    }

    private Aspect getMinimum(Set<Aspect> Aspectes) {
        Aspect minimum = null;
        for (Aspect Aspect : Aspectes) {
            if (minimum == null) {
                minimum = Aspect;
            } else {
                if (getShortestDistance(Aspect) < getShortestDistance(minimum)) {
                    minimum = Aspect;
                }
            }
        }
        return minimum;
    }

    private int getShortestDistance(Aspect destination) {
        Integer d = distance.get(destination);
        if (d == null) {
            return Integer.MAX_VALUE;
        } else {
            return d;
        }
    }

    public LinkedList<Aspect> getPath(Aspect target) {
        LinkedList<Aspect> path = new LinkedList<Aspect>();
        Aspect step = target;

        if (predecessors.get(step.getName()) == null) {
            return null;
        }

        path.add(step);
        while (predecessors.get(step.getName()) != null) {
            step = predecessors.get(step.getName());
            path.add(step);
        }

        Collections.reverse(path);
        return path;
    }
}
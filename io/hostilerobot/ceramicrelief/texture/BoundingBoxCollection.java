package io.hostilerobot.ceramicrelief.texture;

import io.hostilerobot.ceramicrelief.imesh.IMesh;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;

import java.util.*;
import java.util.function.BiFunction;

/**
 * represents a set of bounding boxes.
 * internally we sort these bounding boxes so we can quickly test for intersections
 * @param <T>
 */
public class BoundingBoxCollection<T extends Bounds2D> {
    private List<T> items; // bounding boxes for each face
    // ordered locations for bounding boxes
    // maps from x or y position to the set of faces that have that local minima or maxima
    // todo - use bitset representation rather than Set<Integer>. BitSet OR long + index offset
    // then we can just easily OR out intersections
    private TreeMap<Double, Set<Integer>> xBoundsMin;
    private TreeMap<Double, Set<Integer>> xBoundsMax;
    private TreeMap<Double, Set<Integer>> yBoundsMin;
    private TreeMap<Double, Set<Integer>> yBoundsMax;

    public BoundingBoxCollection() {
        items = new ArrayList<>();
        xBoundsMin = new TreeMap<>();
        xBoundsMax = new TreeMap<>();
        yBoundsMin = new TreeMap<>();
        yBoundsMax = new TreeMap<>();
    }

    public Iterable<T> inBounds(T other) {
        // returns boxes that are in bounds of the other box

        // points that are along the X axis and can intersect with the specified box
        NavigableMap<Double, Set<Integer>> minsX = xBoundsMin.subMap(other.getMinX(), true, other.getMaxX(), false);
        NavigableMap<Double, Set<Integer>> maxsX = xBoundsMax.subMap(other.getMinX(), false, other.getMaxX(), true);
        BitSet intersections = new BitSet(items.size());
//        for(Set<Integer> indexes : minsX.values()) intersections.;
//        for(Set<Integer> indexes : maxsX.values()) xInBounds.addAll(indexes);

        NavigableMap<Double, Set<Integer>> minsY = yBoundsMin.subMap(other.getMinY(), true, other.getMaxY(), false);
        NavigableMap<Double, Set<Integer>> maxsY = yBoundsMax.subMap(other.getMinY(), false, other.getMaxY(), true);
        Set<Integer> yInBounds = new HashSet<>(minsY.size() + maxsY.size());
//        for(Set<Integer> indexes : minsX.values()) xInBounds.addAll(indexes);
//        for(Set<Integer> indexes : maxsX.values()) xInBounds.addAll(indexes);
        return null;
    }

    public void addElem(T face) {
        int insertIndex = items.size();
        items.add(face);
        BiFunction<Double, Set<Integer>, Set<Integer>> addIndex = (x, set) -> {
            if(set == null) {
                set = new HashSet<>(2);
            }
            set.add(insertIndex);
            return set;
        };
        xBoundsMin.compute(face.getMinX(), addIndex);
        xBoundsMax.compute(face.getMaxX(), addIndex);
        yBoundsMin.compute(face.getMinY(), addIndex);
        yBoundsMax.compute(face.getMaxY(), addIndex);
    }
}

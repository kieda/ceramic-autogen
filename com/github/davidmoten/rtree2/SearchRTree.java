package com.github.davidmoten.rtree2;

import com.github.davidmoten.rtree2.geometry.Geometry;
import com.github.davidmoten.rtree2.geometry.Rectangle;
import io.hostilerobot.ceramicrelief.texture.Triangle2D;

import java.util.Collections;
import java.util.function.Predicate;

public final class SearchRTree {
    private SearchRTree(){}
    // for some reason this isn't exposed >.<
    // we expose it so we can just search triangles directly
    public static <T, S extends Geometry> Iterable<Entry<T, S>> search(RTree<T, S> tree, Triangle2D triangle) {
        // execute a search such that we test the triangle intersecting against the geometry in place.
        // will allow us to reduce the search space faster by checking if the triangle intersects with the geometry
        // rather than getting all possible intersections with the triangles bounds and filtering from there

        // note that our custom function fulfils our condition, as if a triangle node in the RTree intersects with the RTree, all
        // of its ancestors minimum bounding rectangles will also intersect with triangle.
        return tree.search(geom -> {
            if(geom instanceof Rectangle) {
                return triangle.intersects((Rectangle)geom);
            } else {
                return triangle.intersects(geom.mbr());
            }
        });
    }
}

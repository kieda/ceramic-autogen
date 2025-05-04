package io.hostilerobot.ceramicrelief.texture.mesh_traversal.intersection;

import com.github.davidmoten.rtree2.Entry;
import com.github.davidmoten.rtree2.RTree;
import com.github.davidmoten.rtree2.geometry.Geometry;
import com.github.davidmoten.rtree2.geometry.Rectangle;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Predicate;

public final class SearchRTree {
    private SearchRTree(){}
    // for some reason this isn't exposed >.<
    // we expose it so we can just search triangles directly

    private static Predicate<? super Geometry> test(Triangle2DY triangle) {
        return geom -> {
            if(geom instanceof Rectangle) {
                return triangle.intersects((Rectangle)geom);
            } else {
                return triangle.intersects(geom.mbr());
            }
        };
    }
    private static Predicate<? super Geometry> test(Triangle2DX triangle) {
        return geom -> {
            if(geom instanceof Rectangle) {
                return triangle.intersects((Rectangle)geom);
            } else {
                return triangle.intersects(geom.mbr());
            }
        };
    }
    private static final Method search;
    static {
        try {
            search = RTree.class.getDeclaredMethod("search", Predicate.class);
            search.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    public static <T, S extends Geometry> Iterable<Entry<T, S>> search(RTree<T, S> tree, Triangle2DY triangle) {
        // execute a search such that we test the triangle intersecting against the geometry in place.
        // will allow us to reduce the search space faster by checking if the triangle intersects with the geometry
        // rather than getting all possible intersections with the triangles bounds and filtering from there

        // note that our custom function fulfils our condition, as if a triangle node in the RTree intersects with the RTree, all
        // of its ancestors minimum bounding rectangles will also intersect with triangle.
        try {
            return (Iterable<Entry<T, S>>) search.invoke(tree, test(triangle));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T, S extends Geometry> Iterable<Entry<T, S>> search(RTree<T, S> tree, Triangle2DX triangle) {
        // execute a search such that we test the triangle intersecting against the geometry in place.
        // will allow us to reduce the search space faster by checking if the triangle intersects with the geometry
        // rather than getting all possible intersections with the triangles bounds and filtering from there

        // note that our custom function fulfils our condition, as if a triangle node in the RTree intersects with the RTree, all
        // of its ancestors minimum bounding rectangles will also intersect with triangle.
        try {
            return (Iterable<Entry<T, S>>) search.invoke(tree, test(triangle));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}

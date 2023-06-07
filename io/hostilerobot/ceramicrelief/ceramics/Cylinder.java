package io.hostilerobot.ceramicrelief.ceramics;

import javafx.scene.shape.ObservableFaceArray;
import javafx.scene.shape.TriangleMesh;
import org.apache.commons.math.fraction.Fraction;

import java.util.ArrayList;
import java.util.List;

public class Cylinder implements PlanarItem {
    private List<Ring2D> rings;
    private List<Fraction> rotations;
    private List<Fraction> xoffsets;
    private List<Fraction> yoffsets;
    private List<Fraction> zspacings;

    // represents a list of constraints that goes from one ring to another
    private List<List<Constraint>> constraints;

    @Override
    public TriangleMesh getFaces() {
        return null;
    }
    // future types of constraints we may want:
    //   * ensure wall is perpendicular to floor
    //   * ensure edge is parallel to another
    //   * ensure vertex is directly above another, or at the midpoint of an edge


}

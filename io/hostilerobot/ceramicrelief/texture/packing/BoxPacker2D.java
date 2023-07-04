package io.hostilerobot.ceramicrelief.texture.packing;

import io.hostilerobot.ceramicrelief.texture.mesh_traversal.BoundingBox2D;
import javafx.geometry.Point2D;

import java.util.List;

public interface BoxPacker2D {
    List<Point2D> pack(List<BoundingBox2D> boxes);
}

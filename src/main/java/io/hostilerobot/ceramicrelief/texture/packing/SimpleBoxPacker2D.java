package io.hostilerobot.ceramicrelief.texture.packing;

import io.hostilerobot.ceramicrelief.texture.mesh_traversal.BoundingBox2D;
import javafx.geometry.Point2D;

import java.util.List;

/**
 * incredibly simple box packer.
 * Just places each box vertically and greedily
 */
public class SimpleBoxPacker2D implements BoxPacker2D{
    @Override
    public List<Point2D> pack(List<BoundingBox2D> boxes) {
        double height = 0;

        for(BoundingBox2D box : boxes) {

        }

        return List.of();
    }
}

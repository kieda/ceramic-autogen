package io.hostilerobot.ceramicrelief.drivers.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.hostilerobot.ceramicrelief.drivers.rtee.TriangleMeshView;
import io.hostilerobot.ceramicrelief.drivers.rtee.TriangleView;
import javafx.application.Platform;
import javafx.geometry.Point2D;

import java.io.IOException;

public class Triangle2Deserializer extends StdDeserializer<TriangleMeshView> {
    public Triangle2Deserializer() {
        super(TriangleMeshView.class);
    }

    @Override
    public TriangleMeshView deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return deserialize(p, ctxt, new TriangleMeshView());
    }

    @Override
    public TriangleMeshView deserialize(JsonParser p, DeserializationContext ctxt, TriangleMeshView triangleMesh) throws IOException {
        JsonNode jsonNode = p.readValueAsTree();

        TriangleView[] items = new TriangleView[jsonNode.size()/3];

        for(int i = 0; i < jsonNode.size()-2; i+=3) {
            JsonNode a = jsonNode.get(i);
            JsonNode b = jsonNode.get(i + 1);
            JsonNode c = jsonNode.get(i + 2);
            items[i / 3] = new TriangleView(
                    new Point2D(a.get(0).asDouble(), a.get(1).asDouble()),
                    new Point2D(b.get(0).asDouble(), b.get(1).asDouble()),
                    new Point2D(c.get(0).asDouble(), c.get(1).asDouble())
            );
        }
        Platform.runLater(() -> triangleMesh.getTriangleViews().setAll(items));

        return triangleMesh;
    }
}

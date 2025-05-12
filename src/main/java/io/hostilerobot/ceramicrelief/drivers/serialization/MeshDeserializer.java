package io.hostilerobot.ceramicrelief.drivers.serialization;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.hostilerobot.ceramicrelief.qmesh.QMesh;
import io.hostilerobot.ceramicrelief.qmesh.QVertex3D;

import java.io.IOException;

public class MeshDeserializer extends StdDeserializer<QMesh> {
    protected MeshDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public QMesh deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        return deserialize(p, ctxt, new QMesh());
    }

    @Override
    public QMesh deserialize(JsonParser p, DeserializationContext ctxt, QMesh intoValue) throws IOException {
        JsonNode node = p.readValueAsTree();

        QVertex3D[] vertices = ctxt.readTreeAsValue(node.get("vertices"), QVertex3D[].class);
        int[] faces = ctxt.readTreeAsValue(node.get("faces"), int[].class);



        return intoValue;
    }
}

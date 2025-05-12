package io.hostilerobot.ceramicrelief.drivers.dfsmeshprojection;

import com.fasterxml.jackson.databind.JsonNode;
import io.hostilerobot.ceramicrelief.controller.DataController;
import io.hostilerobot.ceramicrelief.controller.JsonDataProcessor;
import io.hostilerobot.ceramicrelief.controller.TextControllerDirectory;
import io.hostilerobot.ceramicrelief.controller.TextControllerMatcher;
import io.hostilerobot.ceramicrelief.drivers.rtee.TriangleMeshView;
import io.hostilerobot.ceramicrelief.drivers.rtee.TriangleView;
import io.hostilerobot.ceramicrelief.qmesh.QMesh;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;

import java.nio.file.Paths;

public class SingleMeshProjectionTest extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    private ObjectProperty<Point2D> dragged = new SimpleObjectProperty<>();

    private QMesh mesh;

    private void fileControllerThread() throws Exception{
        var resource = getClass().getResource("/controller/").getPath();
        // hacky bullshit so we can get the right file
        resource = resource.replace("/target/classes/", "/src/main/resources/");
        TextControllerDirectory.builder()
                .and(TextControllerMatcher.nameAndExtension("simple_mesh", "json"), f ->
                        DataController.builder(
                                        mesh,
                                        JsonDataProcessor.builder(QMesh.class)
//                                                .extractFunction(jsonNode -> jsonNode.get("triangles"))
//                                                .customMapping(jsonNode -> {
//                                                    jsonNode.get("vertices");
//                                                    jsonNode.get("faces");
//                                                    return null;
//                                                })

                                                .build()
                                )
                                .addListener( System.out::println )
                                .build(f))
                .onError(Throwable::printStackTrace)
                .build(Paths.get(resource));
    }

    @Override
    public void start(Stage primaryStage) {
        Box box = new Box(100,100,100);
        box.setCullFace(CullFace.NONE);
        final PhongMaterial greyMaterial = new PhongMaterial();
        greyMaterial.setDiffuseColor(Color.DARKGREY);
        greyMaterial.setSpecularColor(Color.GREY);
        box.setMaterial(greyMaterial);
//        box.setTranslateX(250);
//        box.setTranslateY(100);
//        box.setTranslateZ(400);

        boolean fixedEyeAtCameraZero = false;
        PerspectiveCamera camera = new PerspectiveCamera(fixedEyeAtCameraZero);
        camera.setTranslateX(-150);
        camera.setTranslateY(-100);
        camera.setTranslateZ(-250);

        Group root = new Group(box);
        AmbientLight ambientLight = new AmbientLight(Color.color(0.2, 0.2, 0.2));
        PointLight light = new PointLight(Color.WHITE);
        light.setTranslateX(50);
        light.setTranslateY(-300);
        light.setTranslateZ(-400);
        root.getChildren().addAll(ambientLight, light);
        Scene scene = new Scene(root, 500, 300, true);
        scene.setCamera(camera);

        scene.onMouseDraggedProperty().set(event -> {
            dragged.set(new Point2D(event.getSceneX(), event.getSceneY()));
        });

        Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
        rotateX.setPivotX(0);
        rotateX.setPivotY(0);
        rotateX.setPivotZ(0);
        Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
        rotateY.setPivotX(0);
        rotateY.setPivotY(0);
        rotateY.setPivotZ(0);
        box.getTransforms().addAll(rotateX, rotateY);
        dragged.subscribe((before, after) -> {
            if(before == null) return;
            Point2D delta = before.subtract(after);
            rotateX.setAngle(rotateX.getAngle() - delta.getY());
            rotateY.setAngle(rotateY.getAngle() + delta.getX());
        });

        primaryStage.setScene(scene);
        primaryStage.setTitle("test lol");

        primaryStage.show();
    }
}

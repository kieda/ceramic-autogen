package io.hostilerobot.ceramicrelief.drivers.rtee;

import com.fasterxml.jackson.databind.JsonNode;
import io.hostilerobot.ceramicrelief.controller.DataController;
import io.hostilerobot.ceramicrelief.controller.JsonDataProcessor;
import io.hostilerobot.ceramicrelief.controller.TextControllerDirectory;
import io.hostilerobot.ceramicrelief.controller.TextControllerMatcher;
import io.hostilerobot.ceramicrelief.drivers.ControllerMain;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class RTreeTest extends Application {
    public static void main(String[] args) throws Exception{
        launch(RTreeTest.class, args);
    }

    private AtomicBoolean shutdown = new AtomicBoolean(false);



    private void fileControllerThread() throws Exception{
        var resource = getClass().getResource("/controller/").getPath();
        // hacky bullshit so we can get the right file
        resource = resource.replace("/target/classes/", "/src/main/resources/");
        TextControllerDirectory.builder()
                .and(TextControllerMatcher.nameAndExtension("rtree_test", "json"), f ->
                        DataController.builder(
                                root,
                                JsonDataProcessor.builder(TriangleMeshView.class)
                                        .extractFunction(jsonNode -> jsonNode.get("triangles"))
                                        .customMerging((triangleMesh, jsonNode) ->{
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

                                            Platform.runLater(() -> {
                                                triangleMesh.getTriangleViews().setAll(items);
                                            });
                                        })
                                        .build()
                                )
                                .addListener( System.out::println )
                                .build(f))
                .onError(Throwable::printStackTrace)
                .onComplete(() -> shutdown.set(true))
                .build(Paths.get(resource));
        while(!shutdown.get()) {
            Thread.sleep(1000);
        }
    }

    private TriangleMeshView root;
    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("Test RTree2");
        root = new TriangleMeshView();
//        root.getTriangleViews().add(new TriangleView(new Point2D(50, 50), new Point2D(50, 300), new Point2D(300, 300)));
        Scene scene = new Scene(root);
        scene.onMousePressedProperty().set(mouseEvent -> {
            double x = mouseEvent.getX();
            double y = mouseEvent.getY();
            root.updateTouchingVertices(7, x, y);
        });
        scene.onMouseDraggedProperty().set(mouseEvent -> {
            root.drag(mouseEvent.getX(), mouseEvent.getY());
        });

        stage.setScene(scene);
        stage.setWidth(800);
        stage.setHeight(800);
        stage.show();

        new Thread(() -> {
            try {
                fileControllerThread();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}

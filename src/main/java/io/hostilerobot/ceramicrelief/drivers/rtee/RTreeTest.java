package io.hostilerobot.ceramicrelief.drivers.rtee;

import io.hostilerobot.ceramicrelief.controller.DataController;
import io.hostilerobot.ceramicrelief.controller.JsonDataProcessor;
import io.hostilerobot.ceramicrelief.controller.TextControllerDirectory;
import io.hostilerobot.ceramicrelief.controller.TextControllerMatcher;
import io.hostilerobot.ceramicrelief.drivers.ControllerMain;
import javafx.application.Application;
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
                .and(TextControllerMatcher.fileExtension("json"), f ->
                        DataController.builder(JsonDataProcessor.builder(Map.class).build())
                                .addListener( System.out::println )
                                .disableMerging()
                                .build(f))
                .onError(Throwable::printStackTrace)
                .onComplete(() -> shutdown.set(true))
                .build(Paths.get(resource));
        while(!shutdown.get()) {
            Thread.sleep(1000);
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("Test RTree2");
        TriangleMeshView root = new TriangleMeshView();
        root.getTriangleViews().add(new TriangleView(new Point2D(50, 50), new Point2D(50, 300), new Point2D(300, 300)));
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

    }
}

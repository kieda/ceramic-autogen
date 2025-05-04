package io.hostilerobot.ceramicrelief.drivers;

import io.hostilerobot.ceramicrelief.controller.DataController;
import io.hostilerobot.ceramicrelief.controller.JsonDataProcessor;
import io.hostilerobot.ceramicrelief.controller.TextControllerDirectory;
import io.hostilerobot.ceramicrelief.controller.TextControllerMatcher;
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

public class ControllerMain extends Application {
    public static void main(String[] args) throws Exception{
        launch(ControllerMain.class, args);
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
        stage.setTitle("Test RTree");
        Group root = new Group();
        Canvas canvas = new Canvas(800, 800);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        drawTriangle(gc,
                new Point2D(0, 0),
                new Point2D(100, 0),
                new Point2D(100, 100));
        root.getChildren().add(canvas);
        stage.setScene(new Scene(root));
        stage.show();

    }
    private void drawTriangle(GraphicsContext context, Point2D a, Point2D b, Point2D c) {
        context.setStroke(Color.DARKSLATEBLUE);
        context.setLineWidth(5);
        context.strokePolygon(new double[]{a.getX(), b.getX(), c.getX()},
                new double[]{a.getY(), b.getY(), c.getY()},
                3);
    }
}

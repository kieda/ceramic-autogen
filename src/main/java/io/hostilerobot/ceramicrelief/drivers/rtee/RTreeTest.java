package io.hostilerobot.ceramicrelief.drivers.rtee;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.davidmoten.rtree2.Entry;
import com.github.davidmoten.rtree2.RTree;
import io.hostilerobot.ceramicrelief.controller.DataController;
import io.hostilerobot.ceramicrelief.controller.JsonDataProcessor;
import io.hostilerobot.ceramicrelief.controller.TextControllerDirectory;
import io.hostilerobot.ceramicrelief.controller.TextControllerMatcher;
import io.hostilerobot.ceramicrelief.drivers.serialization.Triangle2Deserializer;
import io.hostilerobot.ceramicrelief.texture.mesh_traversal.intersection.SearchRTree;
import io.hostilerobot.ceramicrelief.texture.mesh_traversal.intersection.Triangle2D;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.nio.file.Paths;
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

        SimpleModule module = new SimpleModule();
        module.addDeserializer(TriangleMeshView.class, new Triangle2Deserializer());
        JsonDataProcessor.registerModule(module);

        TextControllerDirectory.builder()
                .and(TextControllerMatcher.nameAndExtension("rtree_test", "json"), f ->
                        DataController.builder(
                                root,
                                JsonDataProcessor.builder(TriangleMeshView.class)
                                        .extractFunction(jsonNode -> jsonNode.get("triangles"))
                                        .build()
                                )
                                .addListener( System.out::println )
                                .build(f))
                .onError(Throwable::printStackTrace)
                .onComplete(() -> shutdown.set(true))
                .build(Paths.get(resource));
//        while(!shutdown.get()) {
//            Thread.sleep(1000);
//        }
    }





    private void testRTree(boolean debug) {
        RTree<TriangleView, Triangle2D> tree = RTree.create();
        if(debug) {
            System.out.println("debugging");
        }
        for(TriangleView triangle : root.getTriangleViews()) {
            Triangle2D insertionTriangle = new Triangle2D(
                triangle.getVertex(TriangleView.A),
                triangle.getVertex(TriangleView.B),
                triangle.getVertex(TriangleView.C)
            );

            Iterable<Entry<TriangleView, Triangle2D>> entries = SearchRTree.search(tree, insertionTriangle);
            if(entries.iterator().hasNext()) {
                // there is an intersection with the new triangle we're attempting to place down

                triangle.isIntersectedProperty().set(true);
//                System.out.println(triangle + " intersects with " + Iterables.toList(entries));
//                continue;
            } else {
                triangle.isIntersectedProperty().set(false);
            }

            tree = tree.add(triangle, insertionTriangle);
        }

        if(!root.getTriangleViews().isEmpty()) {
            TriangleView view = root.getTriangleViews().getFirst();
            double minX = view.getMinX(), minY = view.getMinY(), maxX = view.getMaxX(), maxY = view.getMaxY();
            for(int i = 1; i < root.getTriangleViews().size(); i++) {
                view = root.getTriangleViews().get(i);
                if(view.getMinX() < minX) minX = view.getMinX();
                if(view.getMinY() < minY) minY = view.getMinY();
                if(view.getMaxX() > maxX) maxX = view.getMaxX();
                if(view.getMaxY() > maxY) maxY = view.getMaxY();
            }


            BufferedImage bi = tree.visualize((int) (maxX - minX), (int) (maxY - minY)).createImage();
            Image img = SwingFXUtils.toFXImage(bi, null);
            if (root.getChildren().getFirst() instanceof ImageView imgv) {
                imgv.setX(minX);
                imgv.setY(minY);
                imgv.setImage(img);
            } else {
                ImageView imgv = new ImageView(img);
                imgv.setX(minX);
                imgv.setY(minY);
                root.getChildren().addFirst(imgv);
            }
        }
    }

    private TriangleMeshView root;
    private Scene scene;

    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("Test RTree2");
        root = new TriangleMeshView();
        scene = new Scene(root);

        scene.onMousePressedProperty().set(mouseEvent -> {
            double x = mouseEvent.getX();
            double y = mouseEvent.getY();
            root.updateTouchingVertices(7, x, y);
        });
        scene.onMouseDraggedProperty().set(mouseEvent -> {
            root.drag(mouseEvent.getX(), mouseEvent.getY());
        });

        root.getTriangleViews().subscribe(() -> testRTree(false));
        root.addTriangleEventHandler(e -> {
            testRTree(false);
        });

        scene.setOnKeyPressed(event -> {
            if(event.getCode() == KeyCode.SPACE) {
                testRTree(true);
            }
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

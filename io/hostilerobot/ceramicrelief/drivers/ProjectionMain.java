package io.hostilerobot.ceramicrelief.drivers;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Line;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;

public class ProjectionMain extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        /**
         * for now : use javafx for testing.
         * utilize files for updates rather than a fleshed-out UI
         *
         * things we want to visualize (independently, together, etc)
         *   * the 3d model
         *   * generic RTree visualization with triangles
         *   * the results of mesh traversal:
         *     - the individual projections from BFS traversal
         *     - the RTree from each BFS traversal
         *   * the results of box packing
         *   * the final results
         *
         * Will eventually make a server to communicate to a javascript frontend for actual modifications
         */
        Box box = new Box(100,100,100);
        box.setCullFace(CullFace.NONE);
        box.setTranslateX(250);
        box.setTranslateY(100);
        box.setTranslateZ(400);

        boolean fixedEyeAtCameraZero = false;
        PerspectiveCamera camera = new PerspectiveCamera(fixedEyeAtCameraZero);
        camera.setTranslateX(150);
        camera.setTranslateY(-100);
        camera.setTranslateZ(250);

        Group root = new Group(box);
        root.setRotationAxis(Rotate.X_AXIS);
        root.setRotate(30);

        Scene scene = new Scene(root, 500, 300, true);
        scene.setCamera(camera);
        primaryStage.setScene(scene);
        primaryStage.setTitle("test lol");

        primaryStage.show();
    }
}

package io.hostilerobot.ceramicrelief.drivers.rtee;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TriangleMeshView extends Group {
    private Group triangles = new Group();
    private Circle selectedCircle = new Circle();

    private ListProperty<SelectedVertexInfo> selectedVertices = new SimpleListProperty<>(FXCollections.observableArrayList());
    private List<Integer> selectedTriangleVertex = new ArrayList<>();
    private List<Integer> selectedCounts = new ArrayList<>();
    private List<Integer> selectedTriangles = new ArrayList<>();

    private ObservableList<TriangleView> triangleViews = FXCollections.observableArrayList();
    private ObjectProperty<SelectedVertexInfo> selected = new SimpleObjectProperty<>(null);


    public ObservableList<TriangleView> getTriangleViews() {
        return triangleViews;
    }

    public TriangleMeshView() {
        selectedCircle.setDisable(true);
        selectedCircle.setVisible(false);
        selectedCircle.setRadius(7);
        selectedCircle.setStroke(Color.BLACK);
        selectedCircle.setStrokeWidth(4);
        selectedCircle.setFill(Color.TRANSPARENT);
        selectedCircle.setStrokeType(StrokeType.INSIDE);


        selectedVertices.subscribe(newVal -> {
            int idx;
            if(newVal == null || newVal.isEmpty()) {
                selected.set(null);
            } else if(selected.get() == null || (idx = newVal.indexOf(selected.get())) < 0) {
                selected.set(newVal.getFirst());
            } else {
                // find index of selected in the newVal, then find the next idx (wrapping around)
                if(++idx >= newVal.size()){
                    idx = 0;
                }
                selected.set(newVal.get(idx));
            }
        });

        selected.subscribe((oldVertex, newVertex) -> {
            if(Objects.equals(oldVertex, newVertex))
                return;

            // enable/disable circle
            selectedCircle.setDisable(newVertex == null);
            selectedCircle.setVisible(newVertex != null);

            // unbind oldvertex
            if(oldVertex != null) {
                oldVertex.view.isSelectedProperty().set(false);
                oldVertex.view.getVertexPositionXProperty(oldVertex.vertex)
                        .unbind();
                oldVertex.view.getVertexPositionYProperty(oldVertex.vertex)
                        .unbind();
            }

            // bind new vertex
            if(newVertex != null) {
                // set circle position
                selectedCircle.setCenterX(newVertex.view.getVertexPositionXProperty(newVertex.vertex).get());
                selectedCircle.setCenterY(newVertex.view.getVertexPositionYProperty(newVertex.vertex).get());
                // bind to vertex
                newVertex.view.isSelectedProperty().set(true);
                newVertex.view.getVertexPositionXProperty(newVertex.vertex)
                                .bind(selectedCircle.centerXProperty());
                newVertex.view.getVertexPositionYProperty(newVertex.vertex)
                        .bind(selectedCircle.centerYProperty());
            }
        });

        Bindings.bindContentBidirectional(triangles.getChildren(), (ObservableList)triangleViews);

        this.getChildren().add(triangles);
        this.getChildren().add(selectedCircle);
    }


    public void drag(double pointX, double pointY) {
        if(!selectedCircle.isDisable()) {
            selectedCircle.setCenterX(pointX);
            selectedCircle.setCenterY(pointY);
        }
    }

    public void updateTouchingVertices(double epsilon, double pointX, double pointY) {
        touchingVertices(epsilon, pointX, pointY);
        SelectedVertexInfo[] updatedPoints = new SelectedVertexInfo[selectedTriangleVertex.size()];

        for(int i = 0, count = 0; i < selectedTriangles.size(); count+=selectedCounts.get(i), i++) {
            for(int j = 0; j < selectedCounts.get(i); j++) {
                final int countIdx = count + j;
                final int triangleIdx = selectedTriangles.get(i);
                updatedPoints[countIdx] = new SelectedVertexInfo(triangleIdx, selectedTriangleVertex.get(countIdx), triangleViews.get(triangleIdx));
            }
        }
        selectedVertices.setAll(updatedPoints);
    }

    private void touchingVertices(double epsilon,
                                double pointX, double pointY) {
        selectedTriangleVertex.clear();
        selectedCounts.clear();
        selectedTriangles.clear();

        for(int i = 0; i < triangleViews.size(); i++) {
            TriangleView view = triangleViews.get(i);
            int count = view.touchingVertices(epsilon, pointX, pointY, selectedTriangleVertex);
            if(count > 0) {
                selectedTriangles.add(i);
                selectedCounts.add(count);
            }
        }
    }

    public static class SelectedVertexInfo{
        private int triangleIdx;
        private int vertex;
        private TriangleView view;
        public SelectedVertexInfo(int triangleIdx, int vertex, TriangleView view) {
            this.triangleIdx = triangleIdx;
            this.vertex = vertex;
            this.view = view;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof SelectedVertexInfo that)) return false;
            return triangleIdx == that.triangleIdx && vertex == that.vertex;
        }

        @Override
        public int hashCode() {
            return Objects.hash(triangleIdx, vertex);
        }
    }
}

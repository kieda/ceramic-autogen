package io.hostilerobot.ceramicrelief.ceramics;

import javafx.scene.image.Image;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.ObservableFaceArray;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import org.apache.commons.math.fraction.Fraction;

// simple 4 sided box
public class Box implements PlanarItem {
    private Fraction xScale;
    private Fraction yScale;
    private Fraction zScale;
    public Box(Fraction x, Fraction y, Fraction z) {
        this.xScale = x;
        this.yScale = y;
        this.zScale = z;
    }

    @Override
    public TriangleMesh getFaces() {
        TriangleMesh tm = new TriangleMesh(VertexFormat.POINT_TEXCOORD);
        float x = xScale.floatValue();
        float y = yScale.floatValue();
        float z = zScale.floatValue();
        tm.getPoints().setAll(
            0, 0, 0,// 0 -- front top left
            x, 0, 0,// 1 -- front top right
            0, y, 0,// 2 -- front bot left
            x, y, 0,// 3 -- front bot right
            0, 0, z,// 4 -- back top left
            x, 0, z,// 5 -- back top right
            0, y, z,// 6 -- back bot left
            x, y, z // 7 -- back bot right
        );

        Fraction xTextureScale = ((xScale.add(zScale)).multiply(2)).reciprocal();
        Fraction yTextureScale = (yScale.add(zScale)).reciprocal();
        Fraction xWidth = xTextureScale.multiply(xScale);
        Fraction yWidth = yTextureScale.multiply(yScale);
        Fraction zWidth = xTextureScale.multiply(zScale);
            // = yTextureScale.multiply(zScale);
            // these two should be equal
            // and yWidth + zWidth = 2*xWidth + 2*yWidth = 1

        float yTex1 = yWidth.floatValue();
        float xTex1 = xWidth.floatValue();
        float xTex2 = xWidth.add(zWidth).floatValue();
        float xTex3 = xWidth.multiply(2).add(zWidth).floatValue();
        tm.getTexCoords().setAll(
            0, 0,           //  0 -- front top left
            0, yTex1,       //  1 -- front bot left/bot top left
            xTex1, 0,       //  2 -- front top right/right top left
            xTex1, yTex1,   //  3 -- front bot right/right bot left/bot top right
            xTex2, 0,       //  4 -- right top right/back top left
            xTex2, yTex1,   //  5 -- right bot right/back bot left
            xTex3, 0,       //  6 -- back top right/left top left
            xTex3, yTex1,   //  7 -- back bot right/left bot left
            1, 0,           //  8 -- left top right
            1, yTex1,       //  9 -- left bot right
            0, 1,           // 10 -- bottom front left
            xTex1, 1        // 11 -- bottom front right
        );

        tm.getFaces().setAll(
        //  v  t  v  t  v  t  v  t  v  t  v  t
            3, 3, 0, 0, 2, 1, 3, 3, 1, 2, 0, 0, // front
            2, 9, 0, 8, 4, 6, 2, 9, 4, 6, 6, 7, // left
            2,10, 7, 3, 3,11, 2,10, 6, 1, 7, 3, // bot
            3, 5, 5, 2, 1, 4, 3, 5, 7, 3, 5, 2, // right
            6, 5, 4, 4, 5, 6, 6, 5, 5, 6, 7, 7  // back
        );

        return tm;
    }
}

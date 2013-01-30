/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plotmetadataextractor;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * @author piotr
 */
public class SpatialArray<T> {

    public Rectangle2D.Double area;
    public double widthUnit;
    public double heightUnit;
    public long divisionLevel;
    public HashMap<Long, List<Pair<Shape, T>>> tiles;

    public SpatialArray(Rectangle2D.Double area, long subdivisionLevel) {
        this.area = area;
        this.divisionLevel = subdivisionLevel;
        this.widthUnit = this.area.getWidth() / this.divisionLevel;
        this.heightUnit = this.area.getHeight() / this.divisionLevel;
        this.tiles = new HashMap<Long, List<Pair<Shape, T>>>();
    }

    /**
     * add to all the intersected tiles
     *
     */
    public void put(Shape shape, T val) {
        Rectangle2D bounds2D = shape.getBounds2D();

        long minX = Math.round(Math.floor(bounds2D.getMinX() / this.widthUnit));
        long minY = Math.round(Math.floor(bounds2D.getMinY() / this.heightUnit));
        long maxX = Math.round(Math.floor(bounds2D.getMaxX() / this.widthUnit));
        long maxY = Math.round(Math.floor(bounds2D.getMaxY() / this.heightUnit));

        for (long x = minX; x <= maxX; x++) {
            for (long y = minY; y <= maxY; y++) {
                long ind = this.getIndex(x, y);
                if (!this.tiles.containsKey(ind)) {
                    this.tiles.put(ind, new LinkedList<Pair<Shape, T>>());
                }
                this.tiles.get(ind).add(new ImmutablePair<Shape, T>(shape, val));
            }
        }
    }

    public long getIndex(long x, long y) {
        return x * this.divisionLevel + y;
    }

    public List<Pair<Shape, T>> getByDistance(Point2D.Double point, double maxDistance) {
        LinkedList<Pair<Shape, T>> results = new LinkedList<Pair<Shape, T>>();
        long maxDistanceV = Math.round(Math.floor(maxDistance / this.heightUnit));
        long maxDistanceH = Math.round(Math.floor(maxDistance / this.widthUnit));

        long x = Math.round(Math.floor(point.getX() / this.widthUnit));
        long y = Math.round(Math.floor(point.getY() / this.heightUnit));

        long minX = Math.max(x - maxDistanceH, 0);
        long minY = Math.max(y - maxDistanceV, 0);
        long maxX = Math.max(x + maxDistanceH, this.divisionLevel - 1);
        long maxY = Math.max(y + maxDistanceV, this.divisionLevel - 1);

        for (long cx = minX; cx <= maxX; cx++) {
            for (long cy = minY; cy <= maxY; cy++) {
                long index = this.getIndex(cx, cy);
                if (this.tiles.containsKey(index)) {
                    for (Pair<Shape, T> a : this.tiles.get(index)) {
                        Rectangle2D.Double bounds = (Rectangle2D.Double) a.getKey().getBounds2D();
                        if (ExtLine2D.distanceRectangle(point, bounds) < maxDistance) {
                            results.add(a);
                        }
                    }
                }
            }
        }
        return results;
    }
}

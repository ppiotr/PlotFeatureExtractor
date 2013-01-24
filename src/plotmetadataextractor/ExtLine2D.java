/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plotmetadataextractor;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 *
 * @author piotr
 */
public class ExtLine2D extends Line2D.Double {

    public ExtLine2D(double x1, double y1, double x2, double y2) {
        super(x1, y1, x2, y2);
    }

    /**
     * Calculates the euclidean l2 length of the given line segment
     *
     * @param l
     * @return
     */
    public static double len(Line2D l) {
        return l.getP1().distance(l.getP2());
        //return Math.sqrt((l.getX2() - l.getX1()) * (l.getX2() - l.getX1()) + (l.getY2() - l.getY1()) * (l.getY2() - l.getY1()));

    }

    /**
     * Calcualtes the l2 length of the line segment
     *
     * @return
     */
    public double len() {
        return ExtLine2D.len(this);
    }

    /**
     * Normalises the line:
     *
     * if line is not parallel to the Y axis, P1.x &le; P2.x otherwise, P1.y
     * &le; P2.y
     *
     * @param l
     * @return
     */
    public static Line2D.Double normaliseLine(Line2D.Double l) {
        if (l.getX1() == l.getX2()) {
            // we have to order by the Y coord.

            if (l.getY1() < l.getY2()) {
                return l;
            } else {
                return new Line2D.Double(l.getP2(), l.getP1());
            }

        } else {
            if (l.getX1() < l.getX2()) {
                return l;
            } else {
                return new Line2D.Double(l.getP2(), l.getP1());
            }
        }
    }

    /**
     * Returns the intersection point between two lines. The point can lie
     * outside of line intervals in the case when the intervals do not intersect
     * but belong to intersecting lines or are collinear
     *
     * @param aThis
     * @param line
     * @return
     */
    public static Point2D.Double getIntersection(Line2D.Double l1, Line2D.Double l2) {
        LineParameters lp1 = ExtLine2D.getLineEquation(l1);
        LineParameters lp2 = ExtLine2D.getLineEquation(l2);
        double den = lp2.a * lp1.b - lp1.a * lp2.b;

        if (den == 0) {
            // there are no answers - lines are parallel or colinear
            if (lp1.a == lp2.a && lp1.b == lp2.b && lp1.c == lp2.c) {
                // collinear ... determine if there exists a common point and return it
                return new Point2D.Double(l1.getX1(), l1.getY1());
            } else {
                return null;
            }
        }

        double x = (lp1.c * lp2.b) - (lp2.c * lp1.b) / den;
        double y = (-lp2.c - lp2.a * x) / lp2.b;

        return new Point2D.Double(x, y);
    }

    /**
     * This class encodes parameters of a line Every line can be described in a
     * form: ax + by + c = 0
     *
     * The parameters a,b and c are represented by this class
     */
    public static class LineParameters {

        public double a;
        public double b;
        public double c;

        public LineParameters(double ta, double tb, double tc) {
            this.a = ta;
            this.b = tb;
            this.c = tc;

        }
    }

    /**
     * Returns the coefficient of the current line in a canonical form ax + by +
     * c = 0
     *
     * the b coefficient will always be equal 0 or -1 (making the representation
     * easier to transform to the y = ax + c form
     *
     * @return
     */
    public static LineParameters getLineEquation(Line2D line) {
        Point2D p1 = line.getP1();
        Point2D p2 = line.getP2();

        /*if (p1.getX() > p2.getX()) {
         Point2D tmp = p1;
         p1 = p2;
         p2 = tmp;
         }*/

        if (p1.getX() == p2.getX()) {
            // the line is paralel to the Y axis
            return new LineParameters(1, 0, -p1.getX());
        }

        double a = (p1.getY() - p2.getY()) / (p1.getX() - p2.getX());
        double c = p2.getY() - a * p2.getX();

        return new LineParameters(a, -1, c);
    }

    public LineParameters getLineEquation() {
        return ExtLine2D.getLineEquation(this);
    }

    /**
     * Returns the intersection point of the current line with another line The
     * intersection point can fall outside of the line interval which means that
     * lines would intersect in a given point if extended.
     *
     * @param line
     * @return
     */
    public Point2D.Double getIntersection(Line2D.Double line) {
        return ExtLine2D.getIntersection(this, line);
    }

    /**
     * Returns the angle under which th line is inclined. The angle is expressed
     * in angles and normalised to fit in the range of (-90, 90]
     *
     * @return
     */
    public float getAngle() {

        float xLen = Math.abs((float) (this.getX2() - this.getX1()));
        float yLen = Math.abs((float) (this.getY2() - this.getY1()));

        float alpha; // we use grades for debugging purposes ... detection of PI/2 etc.. is difficult when seeing a number

        if (xLen == 0) {
            alpha = 90;
        } else {
            float den = (float) this.getP1().distance(this.getP2());
            float sinAlpha = yLen / den;

            alpha = (float) Math.asin(sinAlpha) * 360 / (2 * (float) Math.PI);

        }
        if (this.getX1() > this.getX2()) {
            alpha = -alpha;
        }
        if (this.getY2() < this.getY1()) {
            alpha = -alpha;
        }
        return alpha;

    }

    /// these methods should be moved to a different class ... in the case of need to reuse
    /**
     * Returns a unit vector colinear such that (P1 -> P2) * this.len() = P2 -
     * P1
     *
     * @return
     */
    public Pair<java.lang.Double, java.lang.Double> getUnitVector() {
        return new ImmutablePair<java.lang.Double, java.lang.Double>((this.getX2() - this.getX1()) / this.len(), (this.getY2() - this.getY1()) / this.len());

    }

    public static Pair<java.lang.Double, java.lang.Double> getVector(Point2D.Double p1, Point2D.Double p2) {
        return new ImmutablePair<java.lang.Double, java.lang.Double>(p2.getX() - p1.getX(), p2.getY() - p1.getY());
    }

    /**
     *
     * @param vec1
     * @param vec2
     * @return
     */
    public static double vecScalarProd(Pair<java.lang.Double, java.lang.Double> vec1, Pair<java.lang.Double, java.lang.Double> vec2) {
        return vec1.getKey() * vec2.getKey() + vec1.getValue() * vec2.getValue();
    }

    public static double vecLen(Pair<java.lang.Double, java.lang.Double> vec) {
        return Math.sqrt(vecScalarProd(vec, vec));
    }

    public static Pair<java.lang.Double, java.lang.Double> vecAdd(Pair<java.lang.Double, java.lang.Double> v1, Pair<java.lang.Double, java.lang.Double> v2) {
        return new ImmutablePair<java.lang.Double, java.lang.Double>(v1.getKey() + v2.getKey(), v1.getValue() + v2.getValue());
    }

    public static Pair<java.lang.Double, java.lang.Double> vecSub(Pair<java.lang.Double, java.lang.Double> v1, Pair<java.lang.Double, java.lang.Double> v2) {
        return new ImmutablePair<java.lang.Double, java.lang.Double>(v1.getKey() - v2.getKey(), v1.getValue() - v2.getValue());
    }

    public static Pair<java.lang.Double, java.lang.Double> vecMul(java.lang.Double a, Pair<java.lang.Double, java.lang.Double> v2) {
        return new ImmutablePair<java.lang.Double, java.lang.Double>(a * v2.getKey(), a * v2.getValue());
    }

    /**
     * Calculates the distance from the given line to a point. By line we
     * understand not the
     */
    public double distance(Point2D.Double point) {

        Pair<java.lang.Double, java.lang.Double> vn = this.getUnitVector();
        Pair<java.lang.Double, java.lang.Double> w = getVector(point, (Point2D.Double) this.getP2());
        Pair<java.lang.Double, java.lang.Double> ortho = vecSub(w, vecMul(vecScalarProd(w, vn), vn));
        return vecLen(ortho);

    }
}

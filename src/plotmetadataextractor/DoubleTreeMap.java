/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plotmetadataextractor;

import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * An implementation of the TreeMap supporting floating point (Double) numbers
 * as Keys. The keys are not matched exactly but using a margin of tolerance,
 * allowing to treat very similar, yet not identical floating point numebrs as
 * identical
 *
 * @author piotr
 */
public class DoubleTreeMap<T> extends TreeMap<Double, T> {

    public double precission;

    public DoubleTreeMap(double precission) {
        super();
        this.precission = precission;
    }

    public DoubleTreeMap(double precission, Comparator<? super Double> comparator) {
        super(comparator);
        this.precission = precission;

    }

    public DoubleTreeMap(double precission, Map<? extends Double, ? extends T> m) {
        super(m);
        this.precission = precission;
    }

    public DoubleTreeMap(double precission, SortedMap<Double, ? extends T> m) {
        super(m);
        this.precission = precission;        
    }

    /**
     * We retrieve the value with the closest key, lying inside of the tolerance
     * region
     *
     * @param key
     * @return
     */
    @Override
    public T get(Object key) {
        Double dkey = (Double) key;
        Double winningKey = this.getApproxKey(dkey);
        if (winningKey != null) {
            return super.get(winningKey);
        }
        return null;
    }

    /**
     * If in the mapping there exists a key that is close to the provided one,
     * we use it to set the value in the map
     *
     * @param k
     * @param v
     * @return
     */
    @Override
    public T put(Double k, T v) {
        Double winningKey = this.getApproxKey(k);
        if (winningKey == null) {
            return super.put(k, v);
        } else {
            return super.put(winningKey, v);
        }
    }

    /**
     * Returning the existing key being as close as possible from the given one
     *
     * @param key
     * @return
     */
    private Double getApproxKey(Double key) {
        Double winningKey = null;
        Double fk = this.floorKey(key);
        Double ck = this.ceilingKey(key);
        if (fk != null && Math.abs(fk - key) <= this.precission) {
            winningKey = fk;
        }

        if (ck != null && (Math.abs(ck - key) <= this.precission)) {
            if ((winningKey == null) || Math.abs(ck - key) < Math.abs(winningKey - key)) {
                winningKey = ck;
            }
        }
        return winningKey;
    }
}

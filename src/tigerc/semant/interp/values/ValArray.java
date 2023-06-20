/**
 * ValArray.java
 * 
 * Author:  jhelasseter
 * 
 * NOTE:  This representation of arrays is really that of a heterogeneously-
 * typed list, which is to say that there is no requirement that every element 
 * be of the same type.  A type error will occur dynamically, at the point 
 * where some element is used in a manner inconsistent with its type.
 * 
 */

package tigerc.semant.interp.values;


public class ValArray implements IValue {
    private final IValue[] elts;

    public ValArray(int sz, IValue init) {
        elts = new IValue[sz];
        for (int i = 0; i < sz; i++) {
            elts[i] = init; // Is this right? Should there be a deep copy of
                            // init?
        }
    }

    public IValue get(int i) {
        return elts[i];
    }

    public void set(int i, IValue v) {
        elts[i] = v;
    }

    public int size() {
        return elts.length;
    }
    
    public String toString() {
        String listing = "[";
        
        for (IValue v:elts) {
            listing = listing + v + ",";
        }
        
        listing = listing.substring(0,listing.length()-1);  // trim off final ','
        listing += "]";
        
        return listing;
    }
}

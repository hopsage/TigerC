/**
 * ValRecord.java
 * 
 * Author:  jhelasseter
 * 
 * NOTE:  As ValArray does with arrays, this representation of records 
 * implements duck typing.  The only way to generate a type error is to access
 * a field symbol not defined for this ValRecord object.  As long as the symbol
 * is there and the associated value is of the correct type, the access won't 
 * fail.
 * 
 */

package tigerc.semant.interp.values;

import java.util.HashMap;
import java.util.List;

import tigerc.util.Pair;
import tigerc.util.Symbol;

public class ValRecord implements IValue {
    private HashMap<Symbol, IValue> fields;

    public ValRecord(List<Pair<Symbol, IValue>> init) {
        fields = new HashMap<>();
        for (Pair<Symbol, IValue> p : init) {
            Symbol s = p.fst;
            IValue v = p.snd;
            if (!fields.containsKey(s))
                fields.put(s, v);
            else
                throw new java.lang.IllegalArgumentException(
                        "Duplicate field initialization for field " + s);
        }
    }

    public IValue get(Symbol s) {
        IValue v = fields.get(s);
        if (v == null)
            throw new java.lang.IllegalArgumentException(
                    "Record has no field named " + s);
        else
            return v;
    }

    public void set(Symbol s, IValue v) {
        if (!fields.containsKey(s))
            throw new java.lang.IllegalArgumentException(
                    "Record has no field named " + s);
        else
            fields.put(s, v);
    }
    
    public String toString() {
       return fields.toString();
    }
}

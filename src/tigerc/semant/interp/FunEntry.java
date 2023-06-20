/**************************************************************
 *  tigerc/src/semant/interp/FunEntry.java
 *
 *  Author:  John Lasseter
 *  Created:  01/3/2016
 *  Last Modified: 01/13/216
 *  History: 01/13/2016 (jhel) created
 *           
 *
 *  The value environment has two kinds of entries: variable entries and function
 *  entries.  For interpretation, the information we need to represent a function
 *  consists of the parameter list, the function body, and the environment that 
 *  was present at the time of the function's definition.  This structure is more
 *  commonly known as a "closure".
 *  
 **************************************************************/

package tigerc.semant.interp;

import tigerc.syntax.absyn.Exp;
import tigerc.util.ErrorMsg;
import tigerc.util.Symbol;
import tigerc.semant.*;
import tigerc.semant.interp.values.IValue;

import java.util.List;

public class FunEntry implements Entry {

    public final List<Symbol> parameters;
    public final Exp body;
    public final Env<Entry> env;

    public FunEntry(List<Symbol> ps, Exp b, Env<Entry> e) {
        parameters = ps;
        body = b;
        env = e;
    }
    
    protected FunEntry() {
        this(null,null,null);
    }

    public IValue apply(ErrorMsg err, List<IValue> args) {
        
        assert this.parameters.size() == args.size();
        this.env.beginScope();
        for (int i = 0; i < args.size(); i++) {
            Symbol x = this.parameters.get(i);
            VarEntry v = new VarEntry(args.get(i));
            // If we had first-class functions, we'd need to test the value of
            // this.result first, since we might need a FunEntry, instead.

            // Extend the saved environment
            boolean newBinding = this.env.extend(x, v);
            assert newBinding;
        }
        
        // Interpret the body of f, using the saved environment
        InterpV interp = new InterpV(err, this.env);
        this.body.accept(interp);
        
        // Discard bindings for this call (conceptually, we're removing the top
        // frame from the call stack)
        this.env.endScope();
        return interp.getResult();
    }
}

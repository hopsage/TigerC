/**************************************************************
 *  tigerc/src/translate/VarEntry.java
 *
 *  Author:  John Lasseter
 *  Created:  04/25/2014
 *  Last Modified: 04/25/2014
 *
 *  History: 11-21-2011 (jhel) created
 *           05/02/2014 (jhel) added access field, for code generation
 *           09/05/2014 (jhel) major refactoring, to separate from interp 
 *                             and translate versions
 *           
 *  The value environment has two kinds of entries: variable entries and function
 *  entries. 
 *  
 *  Source code taken from Andrew Appel's _Modern Compiler Implementation
 *  Updated (J. Lasseter) to use generics and the tigerc.util.List class.
 *  
 **************************************************************/

package tigerc.translate;

import tigerc.semant.analysis.types.Type;
import tigerc.translate.access.IAccess;

public class VarEntry implements Entry {

    // For translation, the only information we need about a variable is
    // its type and a copy of its access record:

    public final Type ty;
    public final IAccess access;
    public final boolean escapes;
    /*
     * Is this variable used outside of its scope?  If not, we can keep it in a
     * register.  This is not necessarily relevant for JVM generation, but it's
     * a property that gives rise to a couple of special cases in other kinds of
     * back end construction.
     */

    public VarEntry(Type t) {
        this(t, true, null);
    }

    protected VarEntry(Type t, boolean a) {
        this(t, a, null);
    }

    public VarEntry(Type t, boolean a, IAccess acc) {
        this.ty = t;
        this.access = acc;
        this.escapes = a;
    }

    public VarEntry(Type t, IAccess acc) { // <-- For JVM, use this one
        this(t, true, acc);
    }
}

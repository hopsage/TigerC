/**************************************************************
 *  tigerc/src/semant/VarEntry.java
 *
 *  Author:  John Lasseter
 *  Created:  10/01/2014
 *  Last Modified: 10/01/2014
 *
 *  History: 10/01/2014 (jhel) created
 *           
 *  The value environment has two kinds of entries: variable entries and function
 *  entries. 
 *  
 *  Source code taken from Andrew Appel's _Modern Compiler Implementation
 *  Updated (J. Lasseter) to use generics and the tigerc.util.List class.
 *  
 **************************************************************/

package tigerc.semant.interp;

import tigerc.semant.interp.values.IValue;

public class VarEntry implements Entry {

    // For interpretation, the only thing we need for a VarEntry is its value

    public final IValue val;

    public VarEntry(IValue v) {
        val = v;
    }
}

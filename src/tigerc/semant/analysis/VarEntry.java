/**************************************************************
 *  tigerc/src/semant/VarEntry.java
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

package tigerc.semant.analysis;

import tigerc.semant.analysis.types.Type;

public class VarEntry implements Entry {

	// For semantic analysis, the only information we need about a variable is
	// its type and whether it is assignable:
	public final Type ty;
	public final boolean assignable;

	protected VarEntry(Type t) {
		this(t, true);
	}

	protected VarEntry(Type t, boolean a) {
		this.ty = t;
		this.assignable = a;
	}

}

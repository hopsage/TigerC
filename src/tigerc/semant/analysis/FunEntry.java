/**************************************************************
 *  tigerc/src/semant/analysis/FunEntry.java
 *
 *  Author:  John Lasseter
 *  Created:  04/25/2014
 *  Last Modified: 04/25/2014
 *  History: 11-21-2011 (jhel) created
 *           09/05/2014 (jhel) major refactoring, to separate from interp 
 *                             and translate versions
 *
 *  The value environment has two kinds of entries: variable entries and 
 *  function  entries. 
 *  
 *  Source code taken from Andrew Appel's _Modern Compiler Implementation
 *  Updated (J. Lasseter) to use generics and Java's standard List interface.
 *  
 **************************************************************/

package tigerc.semant.analysis;

import tigerc.semant.analysis.types.Type;
import tigerc.util.Pair;
import tigerc.util.Symbol;

import java.util.List;

public class FunEntry implements Entry {

	// For semantic analysis, the only information we need about a function is
	// the types of the formals and the type of the result:
	public final List<Pair<Symbol, Type>> formals;
	public final Type result;


	public FunEntry(List<Pair<Symbol, Type>> fs, Type rTy) {
		formals = (fs==null? new java.util.LinkedList<Pair<Symbol, Type>>(): fs);
		result = rTy;
	}
}

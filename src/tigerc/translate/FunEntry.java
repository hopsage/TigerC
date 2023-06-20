/**************************************************************
 *  tigerc/src/translate/FunEntry.java
 *
 *  Author:  John Lasseter
 *  Created:  04/25/2014
 *  Last Modified: 04/25/2014
 *  History: 11-21-2011 (jhel) created
 *           05/05/2014 (jhel) added extern field, for linking to externally-
 *                             defined methods (code generation); 
 *                             made class publicly-visible;  
 *                             added extern and affiliated methods
 *                             added label and affiliated methods
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
import tigerc.translate.jvm.Label;
import tigerc.util.Pair;
import tigerc.util.Symbol;

import java.util.List;

public class FunEntry implements Entry {

	// For semantic analysis, the only information we need about a function is
	// the types of the formals and the type of the result:
	public final List<Pair<Symbol, Type>> formals;
	public final Type result;

	// Once we consider actual code generation, we'll also want to record a
	// procedure's label, nesting level, and external linkage 
	private Label label;
	private Class<?> enclosingClass;
	
	// private Level level;  // The static nesting depth of this procedure
	// -- TODO:  This abstraction or something like it is necessary to support the
	// lexical scoping rules that arise in the presence of nested procedure definitions.

	public FunEntry(List<Pair<Symbol, Type>> fs, Type rTy) {
		formals = (fs==null? new java.util.LinkedList<Pair<Symbol, Type>>(): fs);
		result = rTy;
		enclosingClass = null;
		label = null;
	}

	public void setEnclosingClass(Class<?> ext) {
		enclosingClass = ext;
	}

	public Class<?> getEnclosingClass() {
		return enclosingClass;
	}

	public boolean isExternal() {
		return enclosingClass != null;
	}

	public void setLabel(Label lab) {
		label = lab;
	}

	public Label getLabel() {
		return label;
	}
}

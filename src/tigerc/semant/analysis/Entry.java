/**************************************************************
 *  tigerc/src/semant/Entry.java
 *
 *  Author:  John Lasseter
 *  Created:  10/27/2011
 *  Last Modified: 04/25/2014
 *
 * The value environment usually has two kinds of entries: variable entries and function
 * entries.  Entry definitions vary by package: analysis, interp, and translate, and in
 * each case, we need a hierarchy specific to that package (in order to keep the use of
 * Env type-safe).  Yuck.
 *   
 * <s>We're going to keep everything at package visibility, as this is an 
 * implementation detail of semantic analysis and should not be touched outside
 * of that</s> 
 *  
 * [UPDATE (04/25/2014)] Nope:  We need to hang on to some type information during code generation,
 * or else (following Appel), combine semantic analysis and translation in
 * one pass.
 * 
 *  Boy, it would be nice to have traits or union types right now.
 *  
 *  Source code taken from Andrew Appel's _Modern Compiler Implementation
 *  Updated (J. Lasseter) to use generics and the tigerc.util.List class.
 *  
 **************************************************************/

package tigerc.semant.analysis;

/************* Entry interface **********************/

public interface Entry {
	// something about these empty interfaces strikes me as a cheap hack
}

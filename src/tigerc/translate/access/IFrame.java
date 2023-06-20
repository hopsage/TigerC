/**************************************************************
 *  tigerc/translate/Entry.java
 *
 *  Author:  John Lasseter
 *  Created:  04/15/2014
 *  Last Modified: 05/02/2016 (added staticLink method interface)
 *
 *  
 **************************************************************/

package tigerc.translate.access;

import tigerc.semant.analysis.types.Type;

public interface IFrame {

	IAccess allocLocal(Type t);

	// TODO: The type parameter exists to guide the return type of the 
	// IAccess's lvalue() and rvalue() methods.
	// Something about this design kind of sucks. I think this is why
	// Appel has the unEx() and unNx() methods in his Tree hierarchy

	/*
	 * Removes and returns the resource allocated at the end of this frame.
	 * Decrements the value of frameEnd appropriately
	 * 
	 * @return The IAccess allocated at the end of this frame, or null, if the
	 * frame is empty
	 */
	IAccess popLocal();

	/*
	 * Returns the offset from the beginning of this frame of the first
	 * available word.
	 */
	int frameEnd();
	
	
	/*
	 * The following represents support for a topic we did not cover in this 
	 * class:  static links.  This is one of several techniques used to support
	 * nested procedure definitions, in particular the need of an inner 
	 * procedure to access variables from the enclosing procedure definition
	 * (other techniques include "lambda lifting", in which all local variables 
	 * are passed as arguments to each inner procedure call, and "display", which
	 * involves the use of a global array that keeps a pointer to the frame of 
	 * the most recently invoked procedure at each static nesting depth).
	 * 
	 * Until you tackle nested procedures, you can implement this as a stub.  In
	 * fact, you don't even need it, unless you use the static links approach to 
	 * nested procedure definitions.
	 */
	IFrame staticLink();  
}

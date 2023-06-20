/**************************************************************
 *  tigerc/src/semant/Entry.java
 *
 *  Author:  John Lasseter
 *  Created:  10/01/2014
 *  Last Modified: 01/13/2016
 *
 * The value environment usually has two kinds of entries: variable entries and function
 * entries.  Entry definitions vary by package: analysis, interp, and translate, and in
 * each case, we need a hierarchy specific to that package (in order to keep the use of
 * Env type-safe).  Yuck.
 *  
 **************************************************************/

package tigerc.semant.interp;

/************* Entry interface **********************/

public interface Entry {
	// something about these empty interfaces strikes me as a cheap hack
}

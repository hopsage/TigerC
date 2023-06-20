/**************************************************************
 ** Symbol.java
 *
 *  The Symbol class provides a representation of Tiger symbols that supports 
 *  fast equality and comparison checking.
 * 
 *  Source code taken from Andrew Appel's _Modern Compiler Implementation in Java_, 
 *  updated (J. Lasseter) to use the new java.util.Map interface and generics.
 *  
 **************************************************************/

package tigerc.util;

public class Symbol {
	private String name;
	private static java.util.Map<String, Symbol> dict = new java.util.Hashtable<String, Symbol>();
	private static int serialNum = 0;

	private Symbol(String n) {
		name = n;
	}

	public String toString() {
		return name;
	}

	/**
	 * Make and return the unique symbol associated with a string. Repeated
	 * calls to <tt>symbol("abc")</tt> will return the same Symbol.
	 */

	public static Symbol sym(String n) {
		String u = n.intern();
		Symbol s = dict.get(u);
		if (s == null) {
			s = new Symbol(u);
			dict.put(u, s);
		}
		return s;
	}

	/**
	 * Generates a new identifier, guaranteed distinct from all other
	 * identifiers used in the current compilation.
	 * 
	 * @return The newly-generated identifier.
	 */
	public static Symbol fresh() {
		while (dict.get("_t$" + serialNum) != null) {
			serialNum += 1;
		}

		String u = "_t$" + serialNum++;
		Symbol s = new Symbol(u);
		// Unlike "ordinary" calls to Symbol.symbol(), the symbols generated
		// here are not stored in the underlying hashtable.

		return s;
	}
}

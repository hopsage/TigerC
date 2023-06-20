/*
	RECORD.java
	
	Author:  John Lasseter (based on a version by Andrew Appel)
	
	History: (10/01/2011)  created
	         (06/02/2014)  modified to use a hashtable, instead of linked list
*/

package tigerc.semant.analysis.types;

import tigerc.util.Symbol;
import tigerc.util.Pair;
import java.util.List;

public class RECORD implements Type {
	public final List<Pair<Symbol,Type>> fields;	
	// TODO:  this should be a HashMap<Symbol,Type>, instead
	
	public Type actual() {
		return this;
	}

	public RECORD(List<Pair<Symbol,Type>> fs) {
		fields = fs;
	}

	public boolean coerceTo(Type t) {
		return this == t.actual();
	}

	public String toString() {
		return "{ " + fields + " }";
	}
}

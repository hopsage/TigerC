package tigerc.semant.analysis.types;

import tigerc.util.Symbol;

public class THUNK implements Type {
	public Symbol name;
	private Type binding;

	public THUNK(Symbol n) {
		name = n; // The value of binding must be set later, once every name in
					// this group has been added
	}

	public boolean isLoop() {
		Type b = binding;
		boolean any;
		binding = null;
		if (b == null)
			any = true; // A name without binding is a circular definition
		else if (b instanceof THUNK)
			any = ((THUNK) b).isLoop(); // This definition is circular if its
										// binding is
		else
			any = false;
		binding = b;
		return any;
	}

	public Type actual() {
		return binding.actual();
	}

	public boolean coerceTo(Type t) {
		return this.actual().coerceTo(t);
	}

	public void bind(Type t) {
		binding = t;
		// Set during the second phase of processing a group of type defns
	}

	public String toString() {
		return name.toString();
	}
}

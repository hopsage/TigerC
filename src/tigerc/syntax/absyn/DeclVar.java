package tigerc.syntax.absyn;

import tigerc.util.Symbol;

public class DeclVar extends Decl {
	public final Symbol name; // The variable name
	public final Symbol typ;  // The variable's declared type (optional)
	public final Exp init;    // The initial value bound to the variable

	public DeclVar(int p, Symbol n, Symbol t, Exp i) {
		super(p);
		name = n;
		typ = t;
		init = i;
	}

	public void accept(IAbsynVisitor v) {
		v.visit(this);
	}
}

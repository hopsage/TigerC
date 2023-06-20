package tigerc.syntax.absyn;

import tigerc.util.Symbol;

public class VarField extends Var {
	public final Var var;      // the record object
	public final Symbol field; // the object's field

	public VarField(int p, Var v, Symbol f) {
		super(p);
		var = v;
		field = f;
	}

	public void accept(IAbsynVisitor v) {
		v.visit(this);
	}
}

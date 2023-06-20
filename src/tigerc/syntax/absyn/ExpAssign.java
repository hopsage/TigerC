package tigerc.syntax.absyn;


public class ExpAssign extends Exp {
	public Var lhs;
	public Exp rhs;

	public ExpAssign(int p, Var v, Exp e) {
		super(p);
		lhs = v;
		rhs = e;
	}

	public void accept(IAbsynVisitor v) {
		v.visit(this);
	}
}

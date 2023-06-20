package tigerc.syntax.absyn;


public class ExpIfElse extends Exp {
	public final Exp test;
	public final Exp thenclause;
	public final Exp elseclause;

	public ExpIfElse(int p, Exp x, Exp y, Exp z) {
		super(p);
		test = x;
		thenclause = y;
		elseclause = z;
	}

	public void accept(IAbsynVisitor v) {
		v.visit(this);
	}
}

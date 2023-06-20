package tigerc.syntax.absyn;


public class ExpIf extends Exp {
	public final Exp test;
	public final Exp thenclause;

	public ExpIf(int p, Exp x, Exp y) {
		super(p);
		test = x;
		thenclause = y;
	}

	public void accept(IAbsynVisitor v) {
		v.visit(this);
	}
}

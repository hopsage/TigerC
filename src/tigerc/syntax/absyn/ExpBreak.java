package tigerc.syntax.absyn;


public class ExpBreak extends Exp {
	public ExpBreak(int p) {
		super(p);
	}

	public void accept(IAbsynVisitor v) {
		v.visit(this);
	}
}

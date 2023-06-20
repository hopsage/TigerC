package tigerc.syntax.absyn;


public class ExpOp extends Exp {
	public enum Op {
		PLUS(0), MIN(1), MUL(2), DIV(3), EQ(4), NE(5), LT(6), LE(7), GT(8), GE(9), AND(10), OR(11);
		
		public final int val;
		
		Op(int v) { val = v;}
	};

	public Exp left, right;
	public Op oper;

	public ExpOp(int p, Exp l, Op o, Exp r) {
		super(p);
		left = l;
		oper = o;
		right = r;
	}
	
	public void accept(IAbsynVisitor v) {
		v.visit(this);
	}
}


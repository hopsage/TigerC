package tigerc.syntax.absyn;

abstract public class Var implements IAbsyn {
	protected final int pos;
	
	protected Var(int p) {pos = p; }
	
	public int getPos() { return pos; }
}

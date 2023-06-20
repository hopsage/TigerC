package tigerc.syntax.absyn;

abstract public class Exp implements IAbsyn {
	protected final int pos;

	protected Exp(int p) {
		pos = p;
	}

	public int getPos() {
		return pos;
	}
}

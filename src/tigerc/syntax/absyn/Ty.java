package tigerc.syntax.absyn;

abstract public class Ty implements IAbsyn {
	protected final int pos;

	protected Ty(int p) {
		pos = p;
	}

	public int getPos() { return pos; }

}

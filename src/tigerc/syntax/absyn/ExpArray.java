package tigerc.syntax.absyn;

import tigerc.util.Symbol;

public class ExpArray extends Exp {
    // array creation: typ [ size ] of init
    // Java: tmp = new typ [ size ];  for (int i = 0; i < tmp.length; i++) { tmp[i] = init; }

    public final Symbol typ;
    public final Exp size, init;

    public ExpArray(int p, Symbol t, Exp s, Exp i) {
        super(p);
        typ = t;
        size = s;
        init = i;
    }

    public void accept(IAbsynVisitor v) {
        v.visit(this);
    }
}

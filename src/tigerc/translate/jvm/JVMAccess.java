package tigerc.translate.jvm;

import tigerc.semant.analysis.types.*;
import tigerc.translate.access.IAccess;

public class JVMAccess implements IAccess {
	private int offset;
	private Type type;

	public JVMAccess(Type t, int off) {
		// offset is calculated by the enclosing JVMFrame
		// type is recorded during code generation
		this.type = t;
		this.offset = off;

	}

	@Override
	public int offset() {
		return offset;
	}

	@Override
	public Type getType() {
		return type;
	}
}

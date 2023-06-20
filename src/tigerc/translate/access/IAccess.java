package tigerc.translate.access;

import tigerc.semant.analysis.types.Type;

public interface IAccess {
	// T lvalue();
	// T rvalue();

	/*
	 * (TODO) Bah. Lousy design. It's not possible to calculate the full instruction
	 * sequence necessary for lvalues or rvalues here, since an IAccess only
	 * describes a single VarSimple value. I.e., it can't capture the more
	 * sophisticated instruction sequences necessary for array subscripting and
	 * record field accesses.
	 * 
	 * Those need to be done in the visit(Var*) methods, with the handling of
	 * accesses as lvalues/rvalues done in visit(ExpAssign) and visit(ExpVar),
	 * using a combination of the encountered type and offset information (both
	 * of which must be recorded as the "return values" of visit(Var*) methods).
	 */

	int offset();
	
	Type getType();
}

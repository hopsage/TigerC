package tigerc.semant.analysis.types;

public interface Type {

	Type actual();

	boolean coerceTo(Type t);
}

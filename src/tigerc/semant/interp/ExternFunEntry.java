package tigerc.semant.interp;

import java.util.List;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import tigerc.semant.interp.values.IValue;
import tigerc.util.ErrorMsg;
import tigerc.semant.interp.values.*;

public class ExternFunEntry extends FunEntry {

    private Class<?> external; // The class containing a method corresponding to
                               // this FunEntry
    private Method fn; // Java reflection object, representing the corresponding
                       // method.

    /**
     * 
     * @param ext - the external class containing the method for this binding
     * @param mname - the name of the method, as defined in ext
     * @param ps - the classes (types) of each parameter for this method
     */
    public ExternFunEntry(Class<?> ext, String mname, Class<?>... ps) {
        super(null, null, null);
        /* For external bindings, the procedures, parameters, body expression,
         * and enclosing environment are irrelevant
         */

        this.external = ext;

        try {
            this.fn = ext.getMethod(mname, ps);
            this.fn.setAccessible(true);
        } catch (NoSuchMethodException e) {
            System.err.println(
                    "INTERNAL BUG:  External class " + external.getName() + " has no method matching this signature");
            System.err.println("Parameter types:");
            for(Class<?> p:ps) {
                System.err.println(p.getName() + ",");
            }
            e.printStackTrace();
            throw new Error();
        } catch (SecurityException e) {
            System.err.println("INTERNAL BUG:  getDeclaredMethod threw a security exception");
            throw e;
        }
    }

    public IValue apply(ErrorMsg __whocares, List<IValue> args) {
        Object[] externArgs = args.stream().map(x -> ival2Ojb(x)).toArray();

        try {
            return obj2iVal(fn.invoke(null, externArgs));
        } catch (IllegalAccessException e) {
            System.err.println("INTERNAL BUG: method " + fn.getName() + " invoked with illegal access");
            System.err.println("method signature: " + fn);
            System.err.println("arguments: " + java.util.Arrays.toString(externArgs));
            e.printStackTrace();
            throw new Error();
        } catch (IllegalArgumentException e) {
            System.err.println("INTERNAL BUG: method " + fn.getName() + " invoked with bad argument");
            e.printStackTrace();
            throw new Error();
        } catch (InvocationTargetException e) {
            System.err.println("Runtime error:  method " + fn.getName() + " threw exception " + e.getCause());
            e.printStackTrace();
            throw new Error();
        }
    }

    private IValue obj2iVal(Object obj) {
        if (obj == null || obj instanceof Void)
            return ValNil.inst;
        else if (obj instanceof Integer)
            return new ValInt(((Integer) obj).intValue());
        else if (obj instanceof String)
            return new ValStr((String) obj);
        else
            throw new UnsupportedOperationException("Conversion to array and record types are not implemented");

    }

    private Object ival2Ojb(IValue x) {
        if (x instanceof ValInt)
            return new Integer(((ValInt) x).val);
        else if (x instanceof ValStr)
            return new String(((ValStr) x).val);
        else if (x instanceof ValUnit)
            return null;
        else
            throw new UnsupportedOperationException("Conversion from array and record types are not implemented");
    }

}

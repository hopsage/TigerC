/*
 *   tigerc/src/semant/Env.java
 *
 *   The Env class implements the idea of an "Environment", the collection of
 *   bindings from names to associated values for a given scope.  Each binding 
 *   is to an "Entry", distinguished according to whether the associated is an 
 *   entry from a function/procedure or to a variable.
 *
 *   What these values are depends on how we're using the environment!  If we're
 *   performing interpretation, then the values are the interpreter's 
 *   representation of runtime values.  This could be something abstract such as 
 *   class definitions representing the actual values, or something as simple as 
 *   an int (used in conventions consistent with the value being treated as an 
 *   integer, the address of a string of characters, an array, a record, or the 
 *   entry point for a procedure).  
 *
 *   On the other hand, if we're using an environment for type-checking, the 
 *   bindings will be to representations of each variable's type.  For code 
 *   generation, we'll need to store bindings to stack frame offsets and blocks 
 *   of code for procedure entries and exits.  And so on.
 *
 *   Author:  John Lasseter
 *   History: 01/15-2016 (jhel) created
 *       
 */

package tigerc.semant;

import tigerc.util.Symbol;

public class Env<T> {
    Scope<T> env;

    protected static Symbol sym(String s) {
        return Symbol.sym(s);
    }

    private Env() { // Used only by instance_noparent().
        env = new Scope<T>();
    }

    public Env(Env<T> levelAbove) {
        env = new Scope<T>(levelAbove.env);
    }

    /*
     * Use this for top-level environment creation only. Probably overkill, but
     * it does a better job of self-documentation
     */
    public static <U> Env<U> instance_noparent() {
        return new Env<>();
    }

    /**
     * Returns a shallow copy of this environment, which is used for
     * implementing closures. The returned Env should be a distinct reference
     * from this object, but both Env objects should hold the same Scope value.
     * In other words, the method ensures that the return value, e, satisfies
     * the following:
     * 
     * this != e && this.env == e.env
     * 
     * This allows further nested scopes to modify this.env (via calls to
     * beginScope() and endScope()) without changing the Scope that was present
     * at the time of the closure's creation.
     * 
     * @return an Env<T> object
     */
    public Env<T> copyOf() {
        Env<T> copy = new Env<>();
        copy.env = this.env;
        return copy;
    }

    /**
     * Gets the object associated with the specified symbol in the Table. If
     * there is no binding for key in the current scope or in an enclosing one,
     * the method returns null
     * 
     * @param x
     *            - the symbol whose value we're looking up
     * @return the value of the lexically-closest binding for key, if that
     *         exists; otherwise, null
     */
    public T lookup(Symbol x) {
        Scope<T> cur = env;

        while (cur != null) {
            T v = cur.bindings.get(x);
            if (v != null) // x is defined in this scope
                return v;
            cur = cur.enclosing;
        }

        return null; // x is not in this or any enclosing scope
    }

    /**
     * Adds the specified Symbol/value binding in to the current scope. Returns
     * true if and only if this call results in a new binding in the current
     * scope. Since non-null keys and values are not permitted, a return value
     * of false does not necessarily indicate that a binding already exists.
     * 
     * @param key
     *            - the symbol for which we're creating a binding
     * @param value
     *            - the value we're binding to key
     * @return true if a new binding has been created, false otherwise
     * @requires - n/a
     * @ensures - (return value == true) => the current scope holds a binding of
     *          (key -> value); (return value == false) => the current scope
     *          contains the binding for key that existed before this call
     *          (which may mean no binding at all, if this method is called with
     *          null values)
     * @effects - there may be a new entry in bindings
     */
    public boolean extend(Symbol key, T value) {
        if (env.bindings.containsKey(key) || key == null || value == null)
            return false;

        env.bindings.put(key, value);

        return true;
    }

    /**
     * Changes the value of the lexically-closest binding for key, if such a
     * binding exists. If there is no binding for key in the current scope or
     * any of the enclosing ones, or if the method is called with null
     * arguments, this call has no effect. Returns true if and only if a binding
     * was found (and hence updated).
     * 
     * @param key
     *            - the symbol for which we're updating a binding
     * @param value
     *            - the new value we're binding to key
     * @return true if the binding for key was updated
     * @requires - n/a
     * @ensures - (return value == true) => the current scope holds a binding of
     *          (key -> value); (return value == false) => the current scope
     *          contains the binding for key that existed before this call
     *          (which may mean no binding at all, if this method is called with
     *          null values)
     * @effects - there may be a new entry in bindings
     */
    public boolean update(Symbol key, T value) {
        if (key == null || value == null)
            return false;
        else {
            Scope<T> cur = env;
            while (cur != null) {
                if (cur.bindings.replace(key, value) != null) // binding exists
                    return true;

                // No binding for key in this scope, so try the enclosing one
                cur = cur.enclosing;
            }

            // There's no binding for key in the current or an enclosing scope
            return false;
        }
    }

    /**
     * Remembers the current state of the Table.
     */
    public void beginScope() {
        this.env = new Scope<>(this.env);
    }

    /**
     * Restores the table to what it was at the most recent beginScope that has
     * not already been ended.
     */
    public void endScope() {
        this.env = this.env.enclosing;
    }

    // /**
    // * Returns an enumeration of the Table's symbols.
    // */
    // public java.util.Iterator<Symbol> keys() {
    // // TODO: extend this to include enclosing scopes
    // return env.bindings.keySet().iterator();
    // }

}

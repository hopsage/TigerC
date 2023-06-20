/**************************************************************
 *  Scope.java
 *
 *  Author:  John Lasseter
 *   
 *  This is a "sheaf of tables" implementation of a symbol table, in which each
 *  scope consists of a hashmap of symbols to entries and a pointer to the 
 *  lexically enclosing scope.  (See, for example, Cooper and Torczon, p. 259.).
 *  For an industrial-scale, imperative language, you might have source code
 *  files with thousands of identifiers, and in that case, a more efficient
 *  implementation scheme might be useful.  This one will do for our purposes,
 *  however (and for a lot of real-world settings, too).  Moreover, it makes it 
 *  easy to support the representation of functions-as-data, which may require  
 *  us to hang on to a function's lexically-enclosing scope beyond the point 
 *  where that scope ends.
 *  
 *  History: 01-15-2016 (jhel) created
 *           03-03-2018 (jhel) added clone() method and Cloneable declaration
 *                             changed type of bindings from Map to Hashtable
 *                             added private copy constructor
 *   
 **************************************************************/

package tigerc.semant;

import tigerc.util.Symbol;

public class Scope<T> {
    /*
     * This represents the Symbol/"value" bindings defined in a scope. It also
     * stores one additional piece of information: enclosing is the scope that
     * lexically encloses this one. Only in the top-level scope should this
     * value be null.
     */
    final java.util.Hashtable<Symbol, T> bindings;
    final Scope<T> enclosing;

    Scope(Scope<T> tail) {
        this(tail, new java.util.Hashtable<>());
    }

    Scope() {
        this(null);
    }

    private Scope(Scope<T> tail, java.util.Hashtable<Symbol, T> b) {
        this.enclosing = tail;
        this.bindings = b;
    }

    public Scope<T> copyOf() {
        Scope<T> copy = new Scope<>(this.enclosing,
                (java.util.Hashtable<Symbol, T>) this.bindings.clone());

        return copy;
    }
}

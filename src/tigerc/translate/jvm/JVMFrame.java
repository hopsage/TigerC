package tigerc.translate.jvm;

import tigerc.semant.analysis.types.*;
import tigerc.translate.access.IAccess;
import tigerc.translate.access.IFrame;

import java.util.List;

public class JVMFrame implements IFrame {
    private List<IAccess> locals = new java.util.ArrayList<>();
    private int maxLocals = 0;    // largest size recorded for locals
    private int freedLocals = 0;  
    // number of once-allocated variables that are currently free

    int maxLocals() { return maxLocals; }
   
    @Override
    public IAccess allocLocal(Type t) {
        // All types in Tiger are 4 bytes wide, assuming 32-bit addressing,
        // so t is actually irrelevant here.
        JVMAccess a = new JVMAccess(t, locals.size());
        locals.add(a);
        
        if (freedLocals == 0) {
            maxLocals += 1;
        } else {
            freedLocals -= 1;
            assert freedLocals >= 0;
        }
        
        return a;
    }

    @Override
    public IAccess popLocal() {
        IAccess a = locals.remove(locals.size() - 1);
        freedLocals += 1;
        assert freedLocals <= maxLocals;
        return a;
    }

    /*
     * Returns the offset from the beginning of this frame of the first
     * available word.
     */
    @Override
    public int frameEnd() {
        return locals.size();
    }

    /*
     * Returns the most recently-invoked frame of the procedure that lexically
     * encloses this one. Note that this is not necessarily the same thing as
     * the frame of the procedure that *called* this one (that's the "dynamic
     * link").
     * 
     * @see tigerc.translate.IFrame#staticLink() for further comments on the
     * idea behind this. Appel's "red book" has a discussion at the end of
     * Section 6.1 (pp. 128--130).
     */
    @Override
    public IFrame staticLink() {
        // TODO Implement static link support (necessary for nested procedure definitions)
        throw new UnsupportedOperationException(
                "static links not implemented.");
    }

}

/*************************************************************************
 **  Lexer.java
 **  tigerc
 **
**  This interface is part of Prof. Andrew Appel's original design and
 **  is used here with his kind permission.  I've modified it to include a
 **  peek() method, which is handy in recursive descent parsing (it implements
 **  the necessary one token lookahead).
 **
 **************************************************************************/

package tigerc.syntax.parse;

public interface Lexer {
    public java_cup.runtime.Symbol next_token() throws java.io.IOException;
    public java_cup.runtime.Symbol peek() throws java.io.IOException;
}

/*************************************************************************
 **  TestLexer.java
 **  tigerc
 **
 **  This driver file is suitable for testing the lexical analyzer generated 
 **  by your Tiger.jflex specification.  With only a few midifcations, it is 
 **  essentially the Main.java file that is part of Prof. Andrew Appel's 
 **  original design and is used here with his kind permission.
 ** 
 ************************************************************************/

package test;

import tigerc.syntax.parse.TigerLex;
import tigerc.syntax.parse.Lexer;
import tigerc.syntax.parse.TigerSyms;
import tigerc.util.ErrorMsg;

public class TestLexer {

    private static String[] symnames;
    
    static {
        symnames = new String[47];  
        /* Because CUP 11a was built before Java had enum types, we're stuck
         * with building our own enum-like features.
         */
        initSym();
    }
    public static void main(String[] args) throws Exception {
        String fname;
        java.io.InputStream inp;

        try {
            fname = args[0];
            ErrorMsg errorMsg = new ErrorMsg(fname);
            inp = new java.io.FileInputStream(fname);

            /* java_cup.runtime.Scanner */
            Lexer lexer = new TigerLex(inp, errorMsg);
            java_cup.runtime.Symbol tok;
            do {
                tok = lexer.next_token();
                System.out.println(tokenVal(symnames, tok) + " " + tok.left);
            } while (tok.sym != TigerSyms.EOF);

            inp.close();
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("USAGE: java TestLexer <srcfile>");
            System.exit(1);
        } catch (java.io.FileNotFoundException e) {
            System.err.println("Cannot open input file " + args[0] + "");
            System.exit(1);
        }

    }

    private static String tokenVal(String[] syms, java_cup.runtime.Symbol t) {
        String v = syms[t.sym];
        if (t.sym == TigerSyms.INT)
            v += ("(" + t.value + ")");
        else if (t.sym == TigerSyms.ID || t.sym == TigerSyms.STRING)
            v += ("(\"" + t.value + "\")");

        return v;
    }

    private static void initSym() {
        symnames[TigerSyms.AND] = "AND";
        symnames[TigerSyms.ARRAY] = "ARRAY";
        symnames[TigerSyms.ASSIGN] = "ASSIGN";
        symnames[TigerSyms.BREAK] = "BREAK";
        symnames[TigerSyms.COLON] = "COLON";
        symnames[TigerSyms.COMMA] = "COMMA";
        symnames[TigerSyms.DIVIDE] = "DIVIDE";
        symnames[TigerSyms.DO] = "DO";
        symnames[TigerSyms.DOT] = "DOT";
        symnames[TigerSyms.ELSE] = "ELSE";
        symnames[TigerSyms.END] = "END";
        symnames[TigerSyms.EOF] = "EOF";
        symnames[TigerSyms.EQ] = "EQ";
        symnames[TigerSyms.error] = "error";
        symnames[TigerSyms.FOR] = "FOR";
        symnames[TigerSyms.FUNCTION] = "FUNCTION";
        symnames[TigerSyms.GE] = "GE";
        symnames[TigerSyms.GT] = "GT";
        symnames[TigerSyms.ID] = "ID";
        symnames[TigerSyms.IF] = "IF";
        symnames[TigerSyms.IMPORT] = "IMPORT";
        symnames[TigerSyms.IN] = "IN";
        symnames[TigerSyms.INT] = "INT";
        symnames[TigerSyms.LBRACE] = "LBRACE";
        symnames[TigerSyms.LBRACK] = "LBRACK";
        symnames[TigerSyms.LE] = "LE";
        symnames[TigerSyms.LET] = "LET";
        symnames[TigerSyms.LPAREN] = "LPAREN";
        symnames[TigerSyms.LT] = "LT";
        symnames[TigerSyms.MINUS] = "MINUS";
        symnames[TigerSyms.NEQ] = "NEQ";
        symnames[TigerSyms.NIL] = "NIL";
        symnames[TigerSyms.OF] = "OF";
        symnames[TigerSyms.OR] = "OR";
        symnames[TigerSyms.PLUS] = "PLUS";
        symnames[TigerSyms.RBRACE] = "RBRACE";
        symnames[TigerSyms.RBRACK] = "RBRACK";
        symnames[TigerSyms.RPAREN] = "RPAREN";
        symnames[TigerSyms.SEMICOLON] = "SEMICOLON";
        symnames[TigerSyms.STRING] = "STRING";
        symnames[TigerSyms.THEN] = "THEN";
        symnames[TigerSyms.TIMES] = "TIMES";
        symnames[TigerSyms.TO] = "TO";
        symnames[TigerSyms.TYPE] = "TYPE";
        symnames[TigerSyms.UMINUS] = "UMINUS";
        symnames[TigerSyms.VAR] = "VAR";
        symnames[TigerSyms.WHILE] = "WHILE";
    }
}

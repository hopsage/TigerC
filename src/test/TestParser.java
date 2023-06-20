package test;

import tigerc.syntax.parse.*;
import tigerc.syntax.absyn.*;

import tigerc.util.ErrorMsg;
import tigerc.util.AbsynPrintVisitor;

public class TestParser {
    public static void main(String[] args) throws java.io.IOException {
        String fname;
        java.io.InputStream inp;
        ErrorMsg errorMsg;
        try {
            if (args.length >= 1) {
                fname = args[0];
                errorMsg = new ErrorMsg(fname);
                inp = new java.io.FileInputStream(fname);
            } else {
                errorMsg = new ErrorMsg(null);
                inp = System.in;
            }

            TigerParse parser = new TigerParse(new TigerLex(inp, errorMsg),
                    errorMsg);

            // IAbsyn prog = (IAbsyn) parser.debug_parse().value;
            // to eliminate debugging messages, comment out the line above
            IAbsyn prog = (IAbsyn) parser.parse().value; // and uncomment this
                                                         // one

            if (prog != null) {
                AbsynPrintVisitor prettyprint = new AbsynPrintVisitor(
                        System.out);
                prog.accept(prettyprint);

                // "call prettyprint(prog)"

                System.out.println();
            } else { // Support for parsers without semantic actions
                System.out.println("Accepted.");
            }

            inp.close();
        } catch (java.io.FileNotFoundException e) {
            System.err.println("Cannot open input file " + args[0] + "");
            System.exit(1);
        } catch (Throwable e) {
            e.printStackTrace();
            throw new Error(e.toString());
        }
    }
}

/*
    Tiger.flex

    Lexical specification file for Andrew Appel's Tiger language.

    Author:  John Lasseter

    History:
        09/01/2011 (jhel): created
        02/13/2014 (jhel): added peek() method to code section
        02/19/2014 (jhel): fixed peek()
        02/29/2016 (jhel): fixed the constructor call
        04/25/2018 (jhel):  fixed a subtle bug in handling of escape codes
*/

package tigerc.syntax.parse;
import java.io.IOException;

%%

%public
%class TigerLex
%implements Lexer
// Lexer for the peek() function

%cup
// also java_cup.runtime.Scanner, from the %cup option

%{
    private tigerc.util.ErrorMsg errorMsg;
    private int commentDepth = 0;
    private int commentOrStringBegins = -1;
    private boolean readingString = false;
    private String currentString;

    private int charsReadForStr = 0;

    public TigerLex(java.io.InputStream s, tigerc.util.ErrorMsg e) {
        this(new java.io.InputStreamReader(new java.io.BufferedInputStream(s)));
        this.errorMsg=e;
    }

	public TigerLex(java.io.Reader r, tigerc.util.ErrorMsg e) {
	    this(r);
		this.errorMsg = e;
	}

    private void newline() { errorMsg.newline(); }

    private void err (String s) { err(yycolumn,s); }
    private void err (int pos, String s) { errorMsg.error(pos,s); }


    private java_cup.runtime.Symbol tok (int kind) { return tok(kind, null); }
    private java_cup.runtime.Symbol tok (int kind, Object value) {
        // Uncomment the following line, if you want a debugging message:
    	//System.err.println("LEX: " + yytext() + "[" + yychar + ","+ (yychar+yylength()) +"]"  );

        return new java_cup.runtime.Symbol(kind, yychar, yychar+yylength(), value);
    }

    public java_cup.runtime.Symbol peek() throws java.io.IOException {
        java_cup.runtime.Symbol sym = this.next_token();

        if (sym.sym == TigerSyms.STRING) {
            zzStartRead -= (charsReadForStr + 1);
		}
        yypushback(yylength());
        return sym;
    }
%}


%char
%line
%column
%state COMMENT
%state STRING

%eofval{
// Code to go in the lexical analyzer class; to be executed when eof is
// reached
  {
    if (commentDepth != 0) {
      err("Unclosed comment detected at end of file "
	  + "(begins on line " + commentOrStringBegins + ").");
    }
    else if (readingString) {
      err("Unclosed string detected at end of file "
	  + "(begins on line " + commentOrStringBegins + ").");
    }

     return tok(TigerSyms.EOF);
  }
%eofval}

%yylexthrow IOException

alpha      = [A-Za-z]
digit      = [0-9]
id         = {alpha}({alpha}|{digit}|_)*
backslashlit = \\
quotelit     = \"

nonnewline_whitespace_char = [\ \t]
linebreak = \r|\n|\r\n
whitespace =  (linebreak |{nonnewline_whitespace_char})+


%%
<YYINITIAL> {linebreak} { newline(); }
<YYINITIAL> {nonnewline_whitespace_char} { }

<YYINITIAL> "<--"      { return tok(TigerSyms.EOF); }

<YYINITIAL> "/*"       {
  if (commentDepth != 0) throw new IOException(
      "Internal error: bogus commentDepth value of " + commentDepth +
      " in state YYINITIAL, \n while scanning line " + yyline);
  // This is strictly for debugging purposes:  should never appear in real use
  // System.out.println("Beginning COMMENT...");
  commentOrStringBegins = yyline;
  commentDepth++;
  yybegin(COMMENT);
}

<YYINITIAL> "import"    { return tok(TigerSyms.IMPORT); }
<YYINITIAL> "while"    { return tok(TigerSyms.WHILE); }
<YYINITIAL> "for"      { return tok(TigerSyms.FOR); }
<YYINITIAL> "to"       { return tok(TigerSyms.TO); }
<YYINITIAL> "break"    { return tok(TigerSyms.BREAK); }
<YYINITIAL> "let"      { return tok(TigerSyms.LET); }
<YYINITIAL> "in"       { return tok(TigerSyms.IN); }
<YYINITIAL> "end"      { return tok(TigerSyms.END); }
<YYINITIAL> "function" { return tok(TigerSyms.FUNCTION); }
<YYINITIAL> "var"      { return tok(TigerSyms.VAR); }
<YYINITIAL> "type"     { return tok(TigerSyms.TYPE); }
<YYINITIAL> "array"    { return tok(TigerSyms.ARRAY); }
<YYINITIAL> "if"       { return tok(TigerSyms.IF); }
<YYINITIAL> "then"     { return tok(TigerSyms.THEN); }
<YYINITIAL> "else"     { return tok(TigerSyms.ELSE); }
<YYINITIAL> "do"       { return tok(TigerSyms.DO); }
<YYINITIAL> "of"       { return tok(TigerSyms.OF); }
<YYINITIAL> "nil"      { return tok(TigerSyms.NIL); }

<YYINITIAL> ":="       { return tok(TigerSyms.ASSIGN); }
<YYINITIAL> "<>"       { return tok(TigerSyms.NEQ); }
<YYINITIAL> "<="       { return tok(TigerSyms.LE); }
<YYINITIAL> ">="       { return tok(TigerSyms.GE); }
<YYINITIAL> "="        { return tok(TigerSyms.EQ); }
<YYINITIAL> "<"        { return tok(TigerSyms.LT); }
<YYINITIAL> ">"        { return tok(TigerSyms.GT); }
<YYINITIAL> ","        { return tok(TigerSyms.COMMA); }
<YYINITIAL> ":"        { return tok(TigerSyms.COLON); }
<YYINITIAL> ";"        { return tok(TigerSyms.SEMICOLON); }
<YYINITIAL> "("        { return tok(TigerSyms.LPAREN); }
<YYINITIAL> ")"        { return tok(TigerSyms.RPAREN); }
<YYINITIAL> "["        { return tok(TigerSyms.LBRACK); }
<YYINITIAL> "]"        { return tok(TigerSyms.RBRACK); }
<YYINITIAL> "{"        { return tok(TigerSyms.LBRACE); }
<YYINITIAL> "}"        { return tok(TigerSyms.RBRACE); }
<YYINITIAL> "."        { return tok(TigerSyms.DOT); }
<YYINITIAL> "+"        { return tok(TigerSyms.PLUS); }
<YYINITIAL> "-"        { return tok(TigerSyms.MINUS); }
<YYINITIAL> "*"        { return tok(TigerSyms.TIMES); }
<YYINITIAL> "/"        { return tok(TigerSyms.DIVIDE); }
<YYINITIAL> "&"        { return tok(TigerSyms.AND); }
<YYINITIAL> "|"        { return tok(TigerSyms.OR); }

<YYINITIAL> {id}       { return tok(TigerSyms.ID, yytext()); }
<YYINITIAL> {digit}+   { return tok(TigerSyms.INT, new Integer(yytext())); }

<YYINITIAL> {quotelit}       {
  currentString = new String();
  readingString = true;
  commentOrStringBegins = yyline;
  charsReadForStr = 0;
  yybegin(STRING);
}
<STRING>  {quotelit}       {
  readingString = false;
  commentOrStringBegins = -1;  yybegin(YYINITIAL);
  return tok(TigerSyms.STRING, currentString);
}

<STRING>  {linebreak}        {
  err("String ended by newline character at line " + yyline);
}


<STRING>    {backslashlit}n    {
    currentString = currentString.concat("\n");
    charsReadForStr += yylength();
}
<STRING>    {backslashlit}t    {
    currentString = currentString.concat("\t");
    charsReadForStr += yylength();
}

<STRING> ({backslashlit}{digit}{digit}{digit}{nonnewline_whitespace_char})  {
    //Character c = new Character(
		//     (char) (Integer.parseInt(yytext().substring(1)) % 256));
		     // Because Java uses Unicode, and we only want to support
		     // extended ASCII
    //currentString = currentString.concat(c.toString());
    int c = Integer.parseInt(yytext().substring(1)) % 256;
    currentString = currentString.concat("\\" + c + " ");
    charsReadForStr += yylength();
}

<STRING> ({backslashlit}{quotelit}) {
    currentString = currentString.concat("\\\"");
    charsReadForStr += yylength();
}
<STRING> ({backslashlit}{backslashlit}) {
    currentString = currentString.concat("\\\\");
    charsReadForStr += yylength();
}

<STRING> {backslashlit}({nonnewline_whitespace_char}*){linebreak}({nonnewline_whitespace_char}*){backslashlit}  {
    // Line continuation -- ignore everything in between the backslashes
    newline();
    charsReadForStr += yylength();
}

<STRING> ({backslashlit}(.|\n)) {
  err("String contains illegal escape character at line " + yyline);
}

<STRING>  . {
    currentString = currentString.concat(yytext());
    // Ordinary letters, spaces, and digits
    charsReadForStr += yylength();
}



<COMMENT> "/*"         { commentDepth++;}
<COMMENT> {linebreak}         { newline(); }
<COMMENT> "*/"         {
    //System.out.println("Possible close of comment...");
  if ((--commentDepth) == 0) {
   // System.out.println("...confirmed.  Resuming normal scan.");
    commentOrStringBegins = -1;
    yybegin(YYINITIAL);
  }
}
<COMMENT> . {
    // Hack alert:  the success of this is totally dependent on the order of
    // its specification in the LEX file.  It should be the last matchable
    // regexp in COMMENT.
}

<YYINITIAL> .          { // Handling of bogus input
  char c = yytext().charAt(0);
  if (! Character.isWhitespace(c))
    err("Ignoring unknown character " + c + " at line " + yyline);
}

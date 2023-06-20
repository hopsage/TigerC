
/**
 * "Grammars defined without a prefix on the grammar header are combined grammars
 * that can contain both lexical and parser rules. To make a parser grammar that 
 * only allows parser rules, use the following header:"
*/
parser grammar Tiger

@header{
package tigerc.syntax.parse
import java.io.IOException
}

@tokens{DIVIDE, FUNCTION, GE, UMINUS, LPAREN, INT, ARRAY, FOR, MINUS, RPAREN, 
SEMICOLON, AND, LT, TYP, NIL, IN, OR, COMMA, PLUS, ASSIGN, IF, DOT, ID, LE, OF, 
EOF, error,
NEQ, BREAK, EQ, LBRACK, TIMES, COLON, LBRACE, ELSE, RBRACK, TO, WHILE, LET,
THEN, RBRACE, END, STRING, GT, VAR, DO
}


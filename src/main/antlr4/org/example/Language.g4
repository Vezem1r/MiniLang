grammar Language;

program
           : statement+
           ;

statement
           : primitiveType ID (COMMA ID)* SEMI                 # variableDeclaration
           | READ ID (COMMA ID)* SEMI                          # read
           | WRITE expression (COMMA expression)* SEMI         # write
           | FWRITE expression COMMA expression (COMMA expression)* SEMI # fwrite
           | expression SEMI                                   # expressionEval
           | '{' statement+ '}'                                # block
           | IF '(' condition ')' statement (else_)?           # if
           | WHILE '(' condition ')' statement                 # while
           | SEMI                                              # nop
           ;

expression
           : '-' expression                                    # unary
           | '!' expression                                    # not
           | expression op=('*'|'/'|'%') expression            # mul
           | expression op=('+'|'-'|'.') expression            # add
           | expression op=('<'|'>') expression                # relation
           | expression op=('=='|'!=') expression              # comparison
           | expression '&&' expression                        # and
           | expression '||' expression                        # or
           | DEC                                               # decValue
           | OCT                                               # octValue
           | HEXA                                              # hexValue
           | FLOAT_VALUE                                       # floatValue
           | boolValues                                        # boolValue
           | STRING_VALUE                                      # stringValue
           | ID                                                # idValue
           | '(' expression ')'                                # parentheses
           | <assoc=right> ID '=' expression                   # assignment
           ;

condition: expression;
else_: ELSE statement;
primitiveType : INT | FLOAT | BOOL | STRING | FILE;
boolValues : TRUE | FALSE;

// Keywords
INT : 'int';
FLOAT : 'float';
BOOL : 'bool';
STRING : 'string';
READ : 'read';
WRITE : 'write';
IF : 'if';
ELSE: 'else';
WHILE : 'while';
TRUE : 'true';
FALSE : 'false';
FWRITE : 'fwrite';
FILE: 'file';

// Literals
ID : [a-zA-Z][a-zA-Z0-9]* ;
DEC : [1-9][0-9]* ;
OCT : '0'[0-7]* ;
HEXA : '0x'[0-9a-fA-F]+ ;
FLOAT_VALUE : [0-9]+'.'[0-9]+ ;
STRING_VALUE : '"' ( ~[\\"\n\r] | '\\' [\\"] )* '"';
WS : [ \t\r\n]+ -> skip ;
COMMENT : '//' ~[\r\n]* -> skip;

SEMI: ';';
COMMA: ',';
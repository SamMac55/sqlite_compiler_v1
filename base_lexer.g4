lexer grammar base_lexer;


DOUBLE: DIGIT* '.' DIGIT+;
INTEGER: DIGIT+;
COMMENT: '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT: '/*' ( . | '\r' | '\n' )*? '*/' -> skip ;
STRING: '\'' ( ~'\'' | '\'\'' )* '\'';
ID: [a-z][a-z0-9_]*;
WS: [ \t\r\n]+ -> skip ;


fragment DIGIT : [0-9];
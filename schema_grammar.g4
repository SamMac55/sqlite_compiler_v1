grammar schema_grammar;
import base_lexer;
//note to self the next step is to create the visitor for this and generate the symbol table + add semantic checking
//used for parsing the .fullschema command from sqlite so we can use it for semantic processing
//program: fullschema EOF;
program: createTable* EOF;

createTable: CREATE TABLE tablename=ID '(' columnList ')' ';';



columnList: columnDef (',' columnDef)*;


columnDef: attributeName=ID dataType? constraint*
        | foreignKey;

dataType: INTEGERKEYWORD
        |   REAL
        |   TEXT
        ;

constraint: NOT NULL
        |   PRIMARY KEY
        ;

foreignKey: FOREIGN KEY '(' tableattr=ID ')' REFERENCES refTable=ID '(' refColumn=ID ')' ;

CREATE: 'CREATE';
TABLE: 'TABLE';
NOT: 'NOT';
NULL: 'NULL';
PRIMARY: 'PRIMARY';
KEY: 'KEY';
INTEGERKEYWORD: 'INTEGER';
REAL: 'REAL';
TEXT: 'TEXT';
FOREIGN: 'FOREIGN';
REFERENCES: 'REFERENCES';
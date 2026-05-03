grammar liteQL;
import base_lexer;

//this grammar removes aliasing and has a declare block to use for semantic processing
program: stmt* EOF;

stmt:     deleteTable   
        | deleteRow
        | insert
        | updateRow
        | select
        | createTable
        | fullschema
        | tables
        ;


//OPERATIONS
deleteTable: REMOVE TABLE tablename=ID ';';
deleteRow: REMOVE tableSource whereClause ';';
insert: ADD tableSource '('assignList ')' (',''('assignList')')*  ';';
updateRow: CHANGE tableSource SET assignList whereClause ';';
select: limitClause? tableSource selectList? joinClause* orderClause? whereClause? (groupClause havingClause?)? ';';
createTable: NEW TABLE tablename=ID '{' createAttrList '}' ';';
//declareBlock: DECLARE tableSource declaredTablesList ';';

//CLAUSES
limitClause: FIND num=INTEGER;
orderClause: SORT BY attributeList order='desc'       
           | SORT BY attributeList order='asc'        
           | SORT BY attributeList              
           ;
//join clause requires a table to join, what you want to select from said table, and what attribute you will be using from tables in the scope to join on
joinClause: WITH joinTable=tableSource selectList? USING othertable=tableSource'.'attribute; 
whereClause: WITH conjoinedAttrComparison;
groupClause: GROUP BY attributeList;
havingClause: HAVING conjoinedAttrComparison;

//CONSTRAINTS
constraintList: constraint (',' constraint)*;
constraint: NOT NULL #notnull
        |   'pk' #pk
        | 'reference' tablename=ID #fk
        ;

//SELECTING ATTRIBUTES
//a select list is a list of select items
selectList: attributeList #list
        | COUNT? ALL #All
        ;

//LIST OF ATTRIBUTES (used in group by and order by clauses)              
attributeList: attribute (',' attribute)*;
        
//CREATING ATTRIBUTES
//how do we create a new attribute?
createAttr: type name=ID '('constraintList')'           #createAttrWithConstraint
        |   type name=ID                                #createAttrNoConstraint
        ;

//how will a list of new attributes look like when creating table?
createAttrList: createAttr (',' createAttr)*;  

//COMPARING ATTRIBUTES
//handles and, or ex first_name is 'sam' and last_name is 'mac'
conjoinedAttrComparison: attrComparison conjunction conjoinedAttrComparison
        |   attrComparison
        ;

//simple comparison like first_name is 'sam'
attrComparison: attribute comparison value;

//ASSIGNMENTS
assignList: assignmentStmt (',' assignmentStmt)*;
assignmentStmt: attr=attribute '=' val=value; // assignments look like first_name='Sam'

//NAMES
tableSource: tablename=ID ; //a table source is just a name
attribute: function? attr=ID | function? tablename=ID'.'attr=ID; //an attribute is also just an name

//BASICS
//comparisons
comparison: GREATER THAN #greaterThan
        |   LESS THAN   #lessThan       
        |   AT LEAST    #greaterEqual
        |   AT MOST     #lessEqual
        |   IS NOT      #notEqual      
        |   IS  #equal
        ;
        
//values
value: INTEGER
    |   DOUBLE
    |   STRING
    |   NULL
    |   attribute
    ;

//conjunctions
conjunction: AND | OR;

//allowed types
type: 'int'
    | 'double'
    | 'String'
    | 'boolean'
    ;
function: MIN | MAX | AVG | COUNT | TOTAL;
//direct commands
fullschema: 'get schema;';
tables: 'get tables;';

//KEYWORDS
WITH: 'with';
CHANGE: 'change';
SET: 'set';
REMOVE: 'remove';
FIND: 'find';
SORT: 'sorted';
LINK: 'link';
USING: 'using';
GROUP: 'group';
HAVING: 'having';
ADD: 'add';
AS: 'as';
TABLE: 'table';
NEW: 'new';
REFERENCE: 'reference';
BY: 'by';
AND: 'and';
OR: 'or';
IS: 'is';
GREATER: 'greater';
LESS: 'less';
THAN: 'than';
NOT: 'not';
AT: 'at';
LEAST: 'least';
MOST: 'most';
NULL: 'null';
DECLARE: 'declare';
ALL: 'all';
COUNT: 'count';
MIN: 'min';
MAX: 'max';
AVG: 'average';
TOTAL: 'total';
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
insert: ADD tableSource assignList  ';';
updateRow: CHANGE tableSource SET assignList whereClause ';';
select: limitClause? tableSource selectList joinClause? orderClause? whereClause? (groupClause havingClause?)? ';';
createTable: NEW TABLE tablename=ID '{' createAttrList '}' ';';
//declareBlock: DECLARE tableSource declaredTablesList ';';

//CLAUSES
limitClause: FIND num=INTEGER;
orderClause: SORT BY attributeList order='desc'       
           | SORT BY attributeList order='asc'        
           | SORT BY attributeList              
           ;
joinClause: WITH tableSource selectList USING attribute; //simplified join clause to only allow joining on one attribute (means that both tables involved in join will need to have said attribute)
whereClause: WITH conjoinedAttrComparison;
groupClause: GROUP BY attributeList;
havingClause: HAVING conjoinedAttrComparison;

//new rules for declare block
// declaredTablesList: declareTable ',' declaredTablesList           #listOfDeclareTable
//             |   declareTable                                  #singleDeclareTable
//             ;
// declareTable: tableSource '{' createAttrList '}' #declareTableDef
//             ;



//CONSTRAINTS
constraintList: constraint ',' constraintList           #listOfConstraint
            |   constraint                             #singleConstraint
            ;
constraint: NOT NULL
        |   'pk'
        ;

//SELECTING ATTRIBUTES
//a select list is a list of select items
selectList: attributeList
        | ALL;

//LIST OF ATTRIBUTES (used in group by and order by clauses)              
attributeList
  : attribute ',' attributeList                        #attrList
  | attribute                                          #singleAttr     
  ;
        
//CREATING ATTRIBUTES
//how do we create a new attribute?
createAttr: type name=ID '('constraintList')'           #createAttrWithConstraint
        |   type name=ID                                #createAttrNoConstraint
        |   'reference' tablename=ID USING type fkID=ID #createAttrReference
        ;

//how will a list of new attributes look like when creating table?
createAttrList: createAttr ',' createAttrList           #listOfCreateAttr  
        |   createAttr                                  #singleCreateAttr
        ;
//COMPARING ATTRIBUTES
//handles and, or ex first_name is 'sam' and last_name is 'mac'
conjoinedAttrComparison: attrComparison conjunction conjoinedAttrComparison    
                        | attrComparison                                       
                        ;
//simple comparison like first_name is 'sam'
attrComparison: attribute comparison value;

//ASSIGNMENTS
assignList: assignmentStmt ',' assignList               #listOfAssignment
        |   assignmentStmt                              #singleAssignment
        ;
assignmentStmt: attr=attribute '=' val=value; // assignments look like first_name='Sam'

//NAMES
tableSource: tablename=ID ; //a table source is just a name
attribute: attr=ID; //an attribute is also just an name

//BASICS
//comparisons
comparison: GREATER THAN
        |   LESS THAN
        |   AT LEAST
        |   AT MOST
        |   IS NOT
        |   IS
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
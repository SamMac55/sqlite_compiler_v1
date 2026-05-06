# Scripted Language for SQLite + Compiler

## Overview
The goal of the project is to create a scripted lanaguage (known as liteQL) that compiles down into SQLite-safe queries that are then executed against a database of your choosing. In the previous version, the liteQL grammar was created, and a visitor was implemented to perform syntax-driven translation that converted the inputted statements into SQL statments. This version did not, however, check for semantic meaning.  
v1 implements semantic processing by taking the schema of the desired database to construct a symbol table (Schema object) that can be used to verify queries against the tables, attributes, and types of the data. It also executes the compiled statements against the SQLite database for you, so you never have to touch the SQLite terminal!  
The full code pipeline for this project is: get .fullschema from desired database --> parse output and construct Schema object --> parse and validate all inputted statements --> write SQL version of statements into output/output.sql --> use outputted SQL statements and run against the desired database --> put outputs from database into .csv files.  

## Project scope
The project allows for select statements, limit, order, where, group by, having, and multiple join clauses. It allows insert/delete table/delete row/update/ create table statements. The only constraints allowed are primary and forign keys and not null constraints. The only data types are ints, Strings, doubles, booleans, and the null value. The project does not cover indexes, views, procedures, functions, or triggers. Please refer to the liteQL.g4 grammar to understand the scope of the project further.   
## Dependencies
1. antlr4
- Ensure you download [antlr4](https://www.antlr.org/)
2. SQLite must be installed and saved to path
- (Linux/Ubuntu)
```sudo apt update```
```sudo apt install sqlite3```
- Verify: ```sqlite3 --version```
- For steps for Windows/MacOS please read README.txt
3. Python3 must be available, and you need to be able to call a python script using "python3 SCRIPTNAME.py".
- Linux/WSL
``` sudo apt update```
```sudo apt install python3 python3-pip```
-Verify using:
```python3 --version```
- For steps for Windows/MacOS please read README.txt
- **IMPORTANT STEP:** Run "python3 test.py" to test if your python is configured properly for the program. If this does not work, the ProcessBuilder will not be able to call python properly and you may need to modify ProcessBuilder calls from python3 --> python.

## How to build and run (WSL/Linux)
**Ensure you are in the root of the project before building/running**
1. ```antlr4 liteQL.g4 -no-listener -visitor```
2. ```antlr4 schema_grammar.g4 -no-listener -visitor```
**Steps 1 and 2 generate the antlr-related files like the .interp,.tokens, BaseVisitor, and Parsers.**
3. ```CP=$(grep '^CLASSPATH=' "$(which antlr4)" | cut -d= -f2-):.```
4. ```javac -cp $CP *.java ```
5. ```java -cp $CP Driver "data/DATABASENAME.db" < input/FILENAME.txt```
**note that each input file has the name of the database in it. the three databases are:  
hr.db, animals.db,songs.db, and you can view their Schemas in the corresponding DATABASENAME_script.sql file**

## The result
Assuming everything is installed, built, and run properly, the output folder should contain some new files. 
1. *output.sql*- These are the SQL statements that the compiler compiled the input language into
2. *output_#.csv*-These files are the results of executing the queries. Note that if you use a change data example, the # will not be consecutive. This is because there is no ouput for update/insert/delete/create table statements.

## The grammar
1. *liteQL.g4*- This grammar is practically the same as in [v0](https://github.com/SamMac55/sqlite_compiler_v0/). Some modifications include allowing multiple joins and adding in proper aggregation funtions.
2. *schema_grammar.g4*- This grammar maps the output of .fullschema so that we can use the Visitor to create a Schema object from it. It assumes that the database being used is within the scope of the project. 
## The code 
*ASTNode.java* gives the rules for how different statements are created. AST Nodes include an emit and validate method that allows you to emit the SQL version of it and validate the node against the schema respectively. *SelectNode.java* holds all of the different necessary methods that pertain to validating and constructing a select statement. *TreeBuilderVisitor.java* builds the AST by taking the parsed input and creating Nodes based on the information given. *.py* files give the ability to work with the database directly and are called from *Driver.java* which sets up the code pipeline. *CreateSchemaVisitor.java* is what creates the Schema object used for semantics. 
## Next steps
The next steps for the compiler is to build onto the scope and allow for more statements to be executed!

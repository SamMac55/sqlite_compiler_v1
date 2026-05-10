# Packages and dependencies
None

# How to build and run (WSL/Linux)
1. Start by running:
antlr4 liteQL.g4 -no-listener -visitor
antlr4 schema_grammar.g4 -no-listener -visitor
CP=$(grep '^CLASSPATH=' "$(which antlr4)" | cut -d= -f2-):.

**this should generate the visitor/lexer/parser that is needed for Driver.java, CreateSchemaVisitor.java and TreeBuilderVisitor.java**

**Ensure you are at the root of the project before running**
2. Then run:
javac -cp $CP *.java 
to compile all of the java files, this is important because SelectNode.java may not compile properly if javac -cp $CP Driver.java is run

3. Next use any of the input examples, its corresponding database, and run it like this:
java -cp $CP OnlySQLDriver "data/DATABASENAME_fullschema.txt" < input/FILENAME.txt

**note that each input file has the name of the database in it. the three databases are:
hr.db, animals.db,songs.db**

4. See the output in the output/output.sql file
output.sql shows how the statements were compiled

** note: you can view the scripts that created each database under the data directory**

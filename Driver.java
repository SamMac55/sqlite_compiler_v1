//java standard libraries
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.ArrayList;

//antlr4 libraries
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.BaseErrorListener;

public class Driver {
    public static void main(String[] args) throws Exception {
        //first step is to make the schema and get the file we will be reading from
        CreateSchemaVisitor schemaVisitor;
        ArrayList<String> statements = new ArrayList<>();
        if(args.length == 0){throw new RuntimeException("Must provide a sqlite database file path in command line");}
        String dbFile = args[0];
        try{
            //this process gets the output of the .fullschema and puts it in data/schema.txt
            ProcessBuilder pb = new ProcessBuilder("python3", "extract_schema.py", dbFile);
            pb.inheritIO();
            Process p = pb.start();
            //wait for process to finish
            int exitCode = p.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Schema extraction failed");
            }

            //create schema symbol table
            CharStream schemaInput = CharStreams.fromFileName("data/schema.txt");
            schema_grammarLexer schemaLexer = new schema_grammarLexer(schemaInput);
            CommonTokenStream schemaTokens = new CommonTokenStream(schemaLexer);
            schema_grammarParser schemaParser = new schema_grammarParser(schemaTokens);
            //make sure there are no syntax errors (only would hapeen if database is not in scope of project)
            schemaParser.removeErrorListeners();
            schemaParser.addErrorListener(createThrowingErrorListener());
            schemaParser.setErrorHandler(new BailErrorStrategy());
            //create the parser
            ParseTree schemaTree = schemaParser.program();
            schemaVisitor = new CreateSchemaVisitor();
            schemaVisitor.visit(schemaTree);
            Schema schema = schemaVisitor.getSchema(); //make the schema object


            //read inputted instructions
            CharStream input = CharStreams.fromStream(System.in);
            liteQLLexer lexer = new liteQLLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            //parser for it
            liteQLParser parser = new liteQLParser(tokens);
            //sytax error handling
            parser.removeErrorListeners();
            parser.addErrorListener(createThrowingErrorListener());
            parser.setErrorHandler(new BailErrorStrategy());

            // use program rule
            ParseTree tree = parser.program();

            TreeBuilderVisitor builder = new TreeBuilderVisitor();
            builder.visit(tree);
            // iterate over collected AST nodes
            for (ASTNode node : builder.statements) {
                if (node.validate(schema, new ArrayList<>())) {
                    statements.add(node.emitSQL());
                } else {
                    System.out.println("Invalid statement");
                }
            }
            //TESTING PURPOSES -- making sure the schema was created properly
            // System.out.println(schema.tables.toString());
            // for (Schema.Table t : schema.tables) {
            //     System.out.println("Table: " + t.table_name);
            //     for (Schema.Attribute a : t.attributes) {
            //         System.out.println("  Attribute: " + a.attr_name + " Type: " + a.type + " Constraints: " + a.constraints);
            //     }
            // }
        }catch(SyntaxErrorException e){
            System.err.println(e.getMessage());
            return; // stop ONLY for syntax errors
        }catch(FileNotFoundException e){
            System.out.println("Schema file or database file does not exist");
            return;
        }catch(RuntimeException e){
            e.printStackTrace();// we continue because we can still execute the previous valid statements
        }


        //execute statements against database
        //first see what the compiler outputted and put that in output/output.sql
        BufferedWriter writer = new BufferedWriter(new FileWriter("output/output.sql"));
        for(String stmt : statements){
            writer.write(stmt);
            writer.newLine();
        }
        writer.close();
        //then create a new process that runs each output against the database
        try{
            ProcessBuilder pb2 = new ProcessBuilder("python3", "run_queries.py", dbFile);
            pb2.inheritIO();
            Process p2 = pb2.start();
            p2.waitFor();
        }catch(Exception e){
            System.out.println("Unable to execute statements against database.");
        }
    }
    
    //this creates the error listener
    private static BaseErrorListener createThrowingErrorListener() {
        return new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer,
                                    Object offendingSymbol,
                                    int line,
                                    int charPositionInLine,
                                    String msg,
                                    RecognitionException e) {
                throw new SyntaxErrorException(
                    "Syntax error at line " + line + ":" + charPositionInLine + " - " + msg
                );
            }
        };
    }
}
// specific type of runtime exception that involves our type of syntax errors
class SyntaxErrorException extends RuntimeException {
    public SyntaxErrorException(String message) {
        super(message);
    }
}
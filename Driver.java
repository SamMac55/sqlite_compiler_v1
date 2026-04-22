import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class Driver {
    public static void main(String[] args) throws Exception {
        CreateSchemaVisitor schemaVisitor;
        try{
            //create schema symbol table
            CharStream schemaInput = CharStreams.fromFileName("data/schema.txt");
            schema_grammarLexer schemaLexer = new schema_grammarLexer(schemaInput);
            CommonTokenStream schemaTokens = new CommonTokenStream(schemaLexer);
            schema_grammarParser schemaParser = new schema_grammarParser(schemaTokens);
            ParseTree schemaTree = schemaParser.program();
            schemaVisitor = new CreateSchemaVisitor();
            schemaVisitor.visit(schemaTree);
            Schema schema = schemaVisitor.getSchema();
            //read instructions
            CharStream input = CharStreams.fromStream(System.in);
            liteQLLexer lexer = new liteQLLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            liteQLParser parser = new liteQLParser(tokens);

            // use program rule
            ParseTree tree = parser.program();

            PrettyPrintVisitor visitor = new PrettyPrintVisitor(schema);
            String result = visitor.visit(tree);

            System.out.println(result);
            //TESTING PURPOSES
            // System.out.println(schema.tables.toString());
            // for (Schema.Table t : schema.tables) {
            //     System.out.println("Table: " + t.table_name);
            //     for (Schema.Attribute a : t.attributes) {
            //         System.out.println("  Attribute: " + a.attr_name + " Type: " + a.type + " Constraints: " + a.constraints);
            //     }
            // }
        }catch(Exception e){
            System.out.println("Schema file not created please create it");
            return;
        }
        
    }
}

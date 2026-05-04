import java.util.ArrayList;

public class CreateSchemaVisitor extends schema_grammarBaseVisitor<Void> {
    Schema schema;
    Schema.Table currTable;
    @Override public Void visitProgram(schema_grammarParser.ProgramContext ctx) { 
        //create a new schema once we start
        schema = new Schema();
        return visitChildren(ctx);
    }
    //make a new table if we find a create table statement
    @Override public Void visitCreateTable(schema_grammarParser.CreateTableContext ctx) { 
        Schema.Table newTable = new Schema.Table(ctx.tablename.getText());
        schema.tables.add(newTable);
        currTable = newTable;
        return visitChildren(ctx);
     }
	// get definition of an attriubte 
	@Override public Void visitColumnDef(schema_grammarParser.ColumnDefContext ctx) { 
        //get fks
        if(ctx.foreignKey() != null) {
            return visitForeignKey(ctx.foreignKey());
        }
        //get the name of the attr
        String attrName = ctx.attributeName.getText();
        //type
        String dataType = ctx.dataType() != null ? ctx.dataType().getText() : null;
        //constraints
        ArrayList<String> constraints = new ArrayList<>();
        for (schema_grammarParser.ConstraintContext constraintCtx : ctx.constraint()) {
            constraints.add(constraintCtx.getText());
        }
        //add this attribute to the current table
        Schema.Attribute newAttr = new Schema.Attribute(attrName, dataType, constraints);
        currTable.attributes.add(newAttr);
        return null;
    }
	
	
	// Later I want to fix foriegn key constratists to be more consistent with col name + talbe name
	@Override public Void visitForeignKey(schema_grammarParser.ForeignKeyContext ctx) { 
        //account for the foriegn keys + make sure fk is not pk
        String attrName = ctx.attributeName.getText();
        String dataType = ctx.dataType() != null ? ctx.dataType().getText(): null;
        ArrayList<String> constraints = new ArrayList<>();
        for (schema_grammarParser.ConstraintContext constraintCtx : ctx.constraint()) {
            if(constraintCtx.getText().equals("PRIMARYKEY")){
                throw new RuntimeException("Foreign key cannot be a primary key");
            }
            constraints.add(constraintCtx.getText());
        }
        constraints.add("references " + ctx.refTable.getText());
        Schema.Attribute newAttr = new Schema.Attribute(attrName, dataType, constraints);
        currTable.attributes.add(newAttr);
        return null;
    }
    public Schema getSchema() {
        return schema;
    }
}

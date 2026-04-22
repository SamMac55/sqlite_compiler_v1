import java.util.ArrayList;

public class CreateSchemaVisitor extends schema_grammarBaseVisitor<Void> {
    Schema schema;
    Schema.Table currTable;
    @Override public Void visitProgram(schema_grammarParser.ProgramContext ctx) { 
        schema = new Schema();
        return visitChildren(ctx);
    }
    @Override public Void visitCreateTable(schema_grammarParser.CreateTableContext ctx) { 
        Schema.Table newTable = new Schema.Table(ctx.tablename.getText());
        schema.tables.add(newTable);
        currTable = newTable;
        return visitChildren(ctx);
     }
	
	@Override public Void visitColumnDef(schema_grammarParser.ColumnDefContext ctx) { 
        if(ctx.foreignKey() != null) {
            return visitForeignKey(ctx.foreignKey());
        }
        String attrName = ctx.attributeName.getText();
        String dataType = ctx.dataType() != null ? ctx.dataType().getText() : null;
        ArrayList<String> constraints = new ArrayList<>();
        for (schema_grammarParser.ConstraintContext constraintCtx : ctx.constraint()) {
            constraints.add(constraintCtx.getText());
        }
        Schema.Attribute newAttr = new Schema.Attribute(attrName, dataType, constraints);
        currTable.attributes.add(newAttr);
        return null;
    }
	
	
	// Later I want to fix foriegn key constratists to be more consistent with col name + talbe name
	@Override public Void visitForeignKey(schema_grammarParser.ForeignKeyContext ctx) { 
        if(currTable.hasAttribute(ctx.tableattr.getText())) {
            currTable.getAttribute(ctx.tableattr.getText()).constraints.add("references " + ctx.refTable.getText());
        }
        return null;
    }
    public Schema getSchema() {
        return schema;
    }
}

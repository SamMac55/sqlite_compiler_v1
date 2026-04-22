import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PrettyPrintVisitor extends liteQLBaseVisitor<String> {
    public Schema schema;
    public PrettyPrintVisitor(Schema schema) {
        this.schema = schema;
    }
    @Override
    public String visitProgram(liteQLParser.ProgramContext ctx){
        StringBuilder sb = new StringBuilder();
        for (liteQLParser.StmtContext stmt : ctx.stmt()) {
            sb.append(visit(stmt)).append("\n\n");
        }
        return sb.toString();
    }

    @Override public String visitDeleteTable(liteQLParser.DeleteTableContext ctx) { 
        //REMOVE TABLE tablename=ID ';';
        return "-- DELETE TABLE --\nDROP TABLE IF EXISTS " + ctx.tablename.getText() + ";"; 
    }
	
	@Override public String visitDeleteRow(liteQLParser.DeleteRowContext ctx) { 
        //REMOVE tableSource whereClause ';';
        return "-- DELETE ROW --\nDELETE FROM " + ctx.tableSource().getText() + " " + visit(ctx.whereClause()) + ";";
     }

	@Override public String visitInsert(liteQLParser.InsertContext ctx) { 
        //insert: ADD tableSource assignList  ';';
        AssignListExtractor ir = splitAssignList(ctx.assignList().getText());
        return "--INSERT ROW --\nINSERT INTO " + ctx.tableSource().getText() + "(" + String.join(",", ir.labels) 
        + ")\nVALUES (" + String.join(",",ir.values) +  ");";
     }

	@Override public String visitUpdateRow(liteQLParser.UpdateRowContext ctx) { 
        return "-- UPDATE ROW --\nUPDATE " + ctx.tableSource().getText() + 
        "\nSET " + ctx.assignList().getText() + "\n" + visit(ctx.whereClause()) + ";"; 
     }

    //this method's sole purpuse is to build the selectIR that is used by selectEmmiter to create sql statements
	@Override
    public String visitSelect(liteQLParser.SelectContext ctx) {
        SelectIR ir = new SelectIR();

        // assign primary table and select list of the primary table
        ir.primaryTableSource = ctx.tableSource().getText();
        ir.selectList = Arrays.asList(ctx.selectList().getText().split(","));

        // limit
        if (ctx.limitClause() != null)
            ir.limitInt = Integer.parseInt(ctx.limitClause().num.getText());

        // order
        if (ctx.orderClause() != null){
            ir.order = ctx.orderClause().order != null ? ctx.orderClause().order.getText() : "asc";
            for (String attr : flattenAttributeList(ctx.orderClause().attributeList())) {
                ir.orderAttributes.add(new AttributeRef(attr));
            }
        }

        // join
        if (ctx.joinClause() != null) {
            ir.hasJoin = true;
            ir.joinedTable = ctx.joinClause().tableSource().getText();
            ir.joinAttr = ctx.joinClause().attribute().getText();
            ir.joinSelectList = Arrays.asList(ctx.joinClause().selectList().getText().split(","));
        }

        // group by (uses a simple list so we don't need to do anything too crazy)
        if (ctx.groupClause() != null) {
            for (String attr : flattenAttributeList(ctx.groupClause().attributeList())) {
                ir.groupAttributes.add(new AttributeRef(attr));
            }
        }
        //NOTE: having and where clauses use the conjoinedAttributeExpr so we need to build this using a separate ir
        // having
        if (ctx.havingClause() != null) {
            ir.havingCondition = buildConjoinedComparison(ctx.havingClause().conjoinedAttrComparison());
        }

        // where
        if (ctx.whereClause() != null) {
            ir.whereCondition = buildConjoinedComparison(ctx.whereClause().conjoinedAttrComparison());
        }
        //TODO: comment this in when we use symbol tables for semantic processing
        // resolution pass — stamps table names onto AttributeRefs
        // if (ir.hasJoin) {
        //     ir.resolveAttributes(
        //         schema.columnsOf(ir.primaryTableSource),
        //         schema.columnsOf(ir.joinedTable)
        //     );
        // }

        return new SelectEmitter().emit(ir);
    }
     //TODO: fix the attribute list get text
	@Override public String visitCreateTable(liteQLParser.CreateTableContext ctx) { 
        StringBuilder sb = new StringBuilder();
        sb.append("-- CREATE TABLE COMMAND --\n");
        sb.append("DROP TABLE IF EXISTS ").append(ctx.tablename.getText()).append(";\n");
        sb.append("CREATE TABLE ").append(ctx.tablename.getText()).append(" (\n");
        sb.append(visit(ctx.createAttrList()));
        sb.append("\n);");
        return sb.toString();
     }
    
    @Override public String visitWhereClause(liteQLParser.WhereClauseContext ctx) { 
        //WITH conjoinedAttrComparison;
        return "WHERE " + visit(ctx.conjoinedAttrComparison());
     }

     //only used in non select statement
     //this works by getting the attribute expression on the left and recursively building a "tree" to represent the expr
    @Override
    public String visitConjoinedAttrComparison(liteQLParser.ConjoinedAttrComparisonContext ctx) {
        String left = visitAttrComparison(ctx.attrComparison());

        // one expresssion
        if (ctx.conjoinedAttrComparison() == null) {
            return left;
        }

        // build "left AND/OR right"
        String conjunction = ctx.conjunction().getText().toUpperCase();
        String right = visitConjoinedAttrComparison(ctx.conjoinedAttrComparison());

        return left + " " + conjunction + " " + right;
    }
    //only used in non select statements
    @Override
    public String visitAttrComparison(liteQLParser.AttrComparisonContext ctx) {
        String lhs = ctx.attribute().getText();
        String op  = getComparisonSymbol(ctx.comparison().getText());
        String rhs = ctx.value().getText(); // works for both literals and attributes
        return lhs + " " + op + " " + rhs;
    }

    @Override public String visitFullschema(liteQLParser.FullschemaContext ctx) { return ".fullschema"; }
	
	@Override public String visitTables(liteQLParser.TablesContext ctx) { return ".tables"; } 

    @Override
    public String visitListOfCreateAttr(liteQLParser.ListOfCreateAttrContext ctx){
        return visit(ctx.createAttr()) + ", \n" + visit(ctx.createAttrList());
    }
    
    @Override
    public String visitCreateAttrWithConstraint(liteQLParser.CreateAttrWithConstraintContext ctx){
        String constraintList = ctx.constraintList().getText();
        String[] splitConstraints = constraintList.split(",");
        for(int i = 0; i < splitConstraints.length; i++){
            if(splitConstraints[i].trim().equalsIgnoreCase("pk")){
                splitConstraints[i] = "PRIMARY KEY";
            } else if (splitConstraints[i].trim().equalsIgnoreCase("notnull")){
                splitConstraints[i] = "NOT NULL";
            } else {
                splitConstraints[i] = "INVALID CONSTRAINT: " + splitConstraints[i];
            }
        }
        constraintList = String.join(" ", splitConstraints);
        return "\t" +ctx.name.getText() + " " + getType(ctx.type().getText()) + " (" + constraintList + ")";
    }
    //create attribute -no constraints
    @Override
    public String visitCreateAttrNoConstraint(liteQLParser.CreateAttrNoConstraintContext ctx){
        return "\t"+ ctx.name.getText() + " " + getType(ctx.type().getText());
    }
    //create attribute - reference
    @Override
    public String visitCreateAttrReference(liteQLParser.CreateAttrReferenceContext ctx){
        return "\t" + ctx.fkID.getText() + " " + getType(ctx.type().getText()) + ",\n\tFOREIGN KEY (" + ctx.fkID.getText() + ") REFERENCES " + ctx.tablename.getText() + " (" + ctx.fkID.getText() + ")";
    }

    public AssignListExtractor splitAssignList(String assignList){
        /*
        assignList: assignmentStmt ',' assignList
        |   assignmentStmt         
        ;
        assignmentStmt: attr=attribute '=' val=value; 
        */
       String[] splitList = assignList.split(",");
       AssignListExtractor ir = new AssignListExtractor();
       for(int i = 0; i< splitList.length; i++){
            String[] splitExpr = splitList[i].split("=");
            ir.labels.add(splitExpr[0]);
            ir.values.add(splitExpr[1]);
       }
       return ir;
    }
    public String getComparisonSymbol(String comparison){
        switch(comparison){
            case "lessthan":
                return "<";
            case "greaterthan":
                return ">";
            case "atleast":
                return ">=";
            case "atmost":
                return "<=";
            case "is":
                return "=";
            case "isnot":
                return "!=";
        }
        return comparison;
    }
    
    //same as the visit method but returns the conjoined comparison object
    private ConjoinedComparison buildConjoinedComparison(liteQLParser.ConjoinedAttrComparisonContext ctx) {
        AttrComparison leaf = buildAttrComparison(ctx.attrComparison());

        if (ctx.conjoinedAttrComparison() == null)
            return new ConjoinedComparison(leaf);

        ConjoinedComparison left  = new ConjoinedComparison(leaf);
        ConjoinedComparison right = buildConjoinedComparison(ctx.conjoinedAttrComparison());
        String conjunction  = ctx.conjunction().getText().toUpperCase();

        return new ConjoinedComparison(left, conjunction, right);
    }
    //same as visit method but returns the attr comparison object
    private AttrComparison buildAttrComparison(liteQLParser.AttrComparisonContext ctx) {
        AttributeRef lhs = new AttributeRef(ctx.attribute().attr.getText());
        String op = getComparisonSymbol(ctx.comparison().getText());

        if (ctx.value().attribute() != null) {
            AttributeRef rhs = new AttributeRef(ctx.value().attribute().attr.getText());
            return new AttrComparison(lhs, op, rhs);
        }
        return new AttrComparison(lhs, op, ctx.value().getText());
    }
    //helper
    private List<String> flattenAttributeList(liteQLParser.AttributeListContext ctx) {
        List<String> attrs = new ArrayList<>();
        while (ctx instanceof liteQLParser.AttrListContext) {
            liteQLParser.AttrListContext listCtx = (liteQLParser.AttrListContext) ctx;
            attrs.add(listCtx.attribute().attr.getText());
            ctx = listCtx.attributeList();
        }
        // handle the base case — singleAttr
        attrs.add(((liteQLParser.SingleAttrContext) ctx).attribute().attr.getText());
        return attrs;
    }
    private String getType(String type){
        switch(type){
            case ("String"):
                return "TEXT";
            case("int"):
                return "INTEGER";
            case("boolean"):
                return "INTEGER";
            case ("double"):
                return "REAL";
            default:
                return "ERROR TYPE DOESNT EXIST";
        }
    }
}
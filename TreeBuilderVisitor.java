import java.util.ArrayList;
import java.util.List;
public class TreeBuilderVisitor extends liteQLBaseVisitor<ASTNode>{

    public List<ASTNode> statements = new ArrayList<>();
    @Override
    public ASTNode visitProgram(liteQLParser.ProgramContext ctx){
        for (liteQLParser.StmtContext stmt : ctx.stmt()) {
            ASTNode node = visit(stmt);
            if (node != null) {
                statements.add(node);
            }
        }
        return null; // return doesn't matter
    }

    @Override public ASTNode visitDeleteTable(liteQLParser.DeleteTableContext ctx) { 
        //REMOVE TABLE tablename=ID ';';
        return new DeleteTableNode(ctx.tablename.getText());
    }
	
	@Override public ASTNode visitDeleteRow(liteQLParser.DeleteRowContext ctx) { 
        //REMOVE tableSource whereClause ';';
        return new DeleteRowNode(ctx.tableSource().getText(), (ConjoinedComparisonNode) visit(ctx.whereClause().conjoinedAttrComparison()));
     }

	@Override public ASTNode visitInsert(liteQLParser.InsertContext ctx) { 
        //insert: ADD tableSource assignList  ';';
        ArrayList<AssignmentListNode> listOfAssign = new ArrayList<>();
        for(liteQLParser.AssignListContext assignList : ctx.assignList()){
            listOfAssign.add((AssignmentListNode) visit(assignList));
        }
        return new InsertNode(ctx.tableSource().getText(), listOfAssign);
     }

	@Override public ASTNode visitUpdateRow(liteQLParser.UpdateRowContext ctx) { 
        return new UpdateNode(ctx.tableSource().getText(), (AssignmentListNode) visit(ctx.assignList()), (ConjoinedComparisonNode) visit(ctx.whereClause().conjoinedAttrComparison()));
     }

    //this method's sole purpuse is to build the selectIR that is used by selectEmmiter to create sql statements
	@Override
    public ASTNode visitSelect(liteQLParser.SelectContext ctx) {
        /*
        ConjoinedComparisonNode whereClause; */
        //get the main table name
        String mainTableName = ctx.tableSource().getText();
        //get the selected attributes (empty list means select *)
        List<AttributeReference> selectedAttributes = new ArrayList<>();
        selectedAttributes.addAll(getAttributeReferences(ctx.selectList(),mainTableName));
        //get the limit (-1 means no limit)
        int limit = -1;
        if(ctx.limitClause() != null){
            limit = Integer.parseInt(ctx.limitClause().INTEGER().getText());
        }
        List<JoinNode> joins = new ArrayList<>();
        if(ctx.joinClause()!=null){
            joins.add(new JoinNode(ctx.joinClause().joinTable.getText(), ctx.joinClause().attribute().getText(), ctx.joinClause().othertable.getText()));   
            selectedAttributes.addAll(getAttributeReferences(ctx.joinClause().selectList(),ctx.joinClause().joinTable.getText()));
        }
        ConjoinedComparisonNode whereCaluse = null;
        if(ctx.whereClause()!=null){
            whereCaluse = (ConjoinedComparisonNode) visit(ctx.whereClause().conjoinedAttrComparison());
        }
        OrderNode orderBy = null;
        if(ctx.orderClause() != null){
            orderBy = (OrderNode) visit(ctx.orderClause());
        }
        if(ctx.groupClause()==null && ctx.havingClause() != null){
            throw new RuntimeException("Having clause cannot be used without a group by clause in the select statement");
        }
        GroupNode groupBy = null;
        if(ctx.groupClause() != null){
            HavingNode having = null;
            if(ctx.havingClause() != null){
                having = new HavingNode((ConjoinedComparisonNode) visit(ctx.havingClause().conjoinedAttrComparison()));
            }
            groupBy = new GroupNode(getAttributeReferences(ctx.groupClause().attributeList()), having);
        }
        
        return new SelectNode(mainTableName, selectedAttributes, limit, joins, whereCaluse, groupBy, orderBy);
        
    }
    //create an order node that represents the order clause
    @Override public ASTNode visitOrderClause(liteQLParser.OrderClauseContext ctx) { 
        //ORDER BY attribute (ASC|DESC)? ';';
        String order = ctx.order != null ? ctx.order.getText().toUpperCase() : "ASC"; 
        return new OrderNode(getAttributeReferences(ctx.attributeList()), order);
    }

    //create a join clause node that represents the join clause

	@Override public ASTNode visitCreateTable(liteQLParser.CreateTableContext ctx) { 
        List<CreateAttributeNode> attributes = new ArrayList<>();
        String tableString = ctx.tablename.getText();
        for(liteQLParser.CreateAttrContext attrCtx : ctx.createAttrList().createAttr()){
            attributes.add((CreateAttributeNode) visit(attrCtx));
        }
        return new CreateTableNode(tableString, attributes);
    }
    
    @Override
    public ASTNode visitConjoinedAttrComparison(liteQLParser.ConjoinedAttrComparisonContext ctx) {
        AttributeComparisonNode left = (AttributeComparisonNode) visit(ctx.attrComparison());;

        // one expresssion
        if (ctx.conjoinedAttrComparison() == null) {
            return new ConjoinedComparisonNode(left,null,null);
        }

        // build "left AND/OR right"
        String conjunction = ctx.conjunction().getText().toUpperCase();
        ConjoinedComparisonNode right = (ConjoinedComparisonNode) visit(ctx.conjoinedAttrComparison());

        return new ConjoinedComparisonNode(left, conjunction, right);
    }
    //only used in non select statements
    @Override
    public ASTNode visitAttrComparison(liteQLParser.AttrComparisonContext ctx) {
        AttributeReference lhs = new AttributeReference(ctx.attribute().tablename != null ? ctx.attribute().tablename.getText() : null, ctx.attribute().attr.getText());
        String op  = getComparisonSymbol(ctx.comparison());
        Value rhs = getValueFromContext(ctx.value());
        return new AttributeComparisonNode(lhs, op, rhs);
    }
    @Override public ASTNode visitAssignList(liteQLParser.AssignListContext ctx) { 
        //attribute '=' value (',' attribute '=' value)*;
        List<liteQLParser.AssignmentStmtContext> stmts = ctx.assignmentStmt();
        List<AssignmentStatementNode> assignments = new ArrayList<>();
        for (liteQLParser.AssignmentStmtContext stmtCtx : stmts) {
            assignments.add((AssignmentStatementNode) visit(stmtCtx));
        }
        return new AssignmentListNode(assignments);
    }
    @Override public ASTNode visitAssignmentStmt(liteQLParser.AssignmentStmtContext ctx) { 
        //attribute '=' value ';';
        AttributeReference attribute = new AttributeReference(ctx.attribute().tablename != null ? ctx.attribute().tablename.getText() : null, ctx.attribute().attr.getText());
        Value value = getValueFromContext(ctx.value());
        return new AssignmentStatementNode(attribute, value);
    }
    
    @Override public ASTNode visitCreateAttrWithConstraint(liteQLParser.CreateAttrWithConstraintContext ctx) { 
        List<String> constraints = new ArrayList<>();
        List<String> fkConstraints = new ArrayList<>();
        for(liteQLParser.ConstraintContext constraintCtx : ctx.constraintList().constraint()){
            if(constraintCtx instanceof liteQLParser.NotnullContext){
                constraints.add("NOTNULL");
            } else if(constraintCtx instanceof liteQLParser.PkContext){
                constraints.add("PRIMARYKEY");
            } else if(constraintCtx instanceof liteQLParser.FkContext){
                fkConstraints.add("references " + ((liteQLParser.FkContext)constraintCtx).tablename.getText());
            } else {
                throw new RuntimeException("Invalid constraint in create attribute statement");
            }
            
        }
        return new CreateAttributeNode(ctx.name.getText(),getType(ctx.type().getText()),constraints, fkConstraints);
     }

	@Override public ASTNode visitCreateAttrNoConstraint(liteQLParser.CreateAttrNoConstraintContext ctx) { 
        return new CreateAttributeNode(ctx.name.getText(),getType(ctx.type().getText()),new ArrayList<>(), new ArrayList<>());
     }


    @Override public ASTNode visitFullschema(liteQLParser.FullschemaContext ctx) { /*change to something like sqlEmiter.emit(".fullschema") */ return null;}
	
	@Override public ASTNode visitTables(liteQLParser.TablesContext ctx) { /*change to something like sqlEmiter.emit(".tables") */ return null;} 

    public String getComparisonSymbol(liteQLParser.ComparisonContext comparison){
        if(comparison instanceof liteQLParser.GreaterThanContext) return ">";
        else if (comparison instanceof liteQLParser.LessThanContext) return "<";
        else if (comparison instanceof liteQLParser.EqualContext) return "=";
        else if (comparison instanceof liteQLParser.GreaterEqualContext) return ">=";
        else if (comparison instanceof liteQLParser.LessEqualContext) return "<=";
        else if (comparison instanceof liteQLParser.NotEqualContext) return "!=";
        else throw new RuntimeException("Invalid comparison operator in attribute comparison");
    }
    public List<AttributeReference> getAttributeReferences(liteQLParser.SelectListContext ctx, String tablename){
        List<AttributeReference> attributes = new ArrayList<>();
        if(ctx instanceof liteQLParser.AllContext){
            return attributes; //should be empty :)
        }
        for(liteQLParser.AttributeContext attr : ((liteQLParser.ListContext)ctx).attributeList().attribute()){
            attributes.add(new AttributeReference(tablename,attr.attr.getText()));
        }
        return attributes;
    }
    public List<AttributeReference> getAttributeReferences(liteQLParser.AttributeListContext ctx){
        List<AttributeReference> attributes = new ArrayList<>();
        for(liteQLParser.AttributeContext attr : (ctx.attribute())){
            attributes.add(new AttributeReference(attr.tablename ==null ? null: attr.tablename.getText(),attr.attr.getText()));
        }
        return attributes;
    }
    public String getType(String type){
        if(type.equals("int")){
            return "INTEGER";
        } else if(type.equals("double")){
            return "REAL";
        } else if(type.equals("String") || type.equals("boolean")){
            return "TEXT";
        } else {
            throw new RuntimeException("Invalid type in create table statement");
        }
    }

    //this is used in both the assign list and in attribute comparisons
    public Value getValueFromContext(liteQLParser.ValueContext ctx){
        Value value;
        if(ctx.INTEGER() !=null){
            value = new IntLiteral(Integer.parseInt(ctx.INTEGER().getText()));
        } else if(ctx.STRING() != null){
            value = new StringLiteral(ctx.STRING().getText());
        } else if(ctx.attribute() != null){
            value = new AttributeReference(ctx.attribute().tablename != null ? ctx.attribute().tablename.getText() : null, ctx.attribute().attr.getText());
        }else if (ctx.NULL() != null){
            value = new NullLiteral();
        }else if (ctx.DOUBLE() != null){
            value = new DoubleLiteral(Double.parseDouble(ctx.DOUBLE().getText()));
        }else {
            throw new RuntimeException("Invalid value in assignment statement");
        }
        return value;
    }
}

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
        return null;
     }

	@Override public ASTNode visitUpdateRow(liteQLParser.UpdateRowContext ctx) { 
        return null;
     }

    //this method's sole purpuse is to build the selectIR that is used by selectEmmiter to create sql statements
	@Override
    public ASTNode visitSelect(liteQLParser.SelectContext ctx) {
        /*
        ConjoinedComparisonNode whereClause; */
        //get the main table name
        String mainTableName = ctx.tableSource().getText();
        //get the selected attributes (empty list means select *)
        String selectedAttributes = ctx.selectList().getText();
        //get the limit (-1 means no limit)
        int limit = -1;
        if(ctx.limitClause() != null){
            try{
                limit = Integer.parseInt(ctx.limitClause().num.getText());
                if(limit < 0){
                    throw new RuntimeException("Limit must be a non-negative integer for the select statement");
                }
            } catch(NumberFormatException e){
                throw new RuntimeException("Limit must be a non-negative integer for the select statement");
            }
        }
        JoinNode join = null;
        if(ctx.joinClause()!=null){
            String joinAttributes = ctx.joinClause().selectList().getText().trim();
            if(joinAttributes.equals("all")){
                //System.out.println("in line 62");
                join = new JoinNode(mainTableName, ctx.joinClause().tableSource().getText(), ctx.joinClause().attribute().getText(), new ArrayList<>());
            }else{
                join = new JoinNode(mainTableName, ctx.joinClause().tableSource().getText(), ctx.joinClause().attribute().getText(), getAttributeReferences(joinAttributes));
            }
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
            String groupByAttributes= ctx.groupClause().attributeList().getText();
            HavingNode having = null;
            if(ctx.havingClause() != null){
                having = new HavingNode((ConjoinedComparisonNode) visit(ctx.havingClause().conjoinedAttrComparison()));
            }
            groupBy = new GroupNode(getAttributeReferences(groupByAttributes), having);
        }
        if(selectedAttributes.equals("all")){
            return new SelectNode(mainTableName, new ArrayList<>(), limit, join, whereCaluse, groupBy, orderBy);
        }
        return new SelectNode(mainTableName, getAttributeReferences(selectedAttributes), limit, join, whereCaluse, groupBy, orderBy);
    }
    //create an order node that represents the order clause
    @Override public ASTNode visitOrderClause(liteQLParser.OrderClauseContext ctx) { 
        //ORDER BY attribute (ASC|DESC)? ';';
        String attributeList = ctx.attributeList().getText();
        String order = ctx.order != null ? ctx.order.getText().toUpperCase() : "ASC"; 
        return new OrderNode(getAttributeReferences(attributeList), order);
    }

    //create a join clause node that represents the join clause

	@Override public ASTNode visitCreateTable(liteQLParser.CreateTableContext ctx) { 
        return null;
     }
    //unsure if this is necessary now
    // @Override public ASTNode visitWhereClause(liteQLParser.WhereClauseContext ctx) { 
    //     //WITH conjoinedAttrComparison;
    //     return visit(ctx.conjoinedAttrComparison());
    //  }

     //only used in non select statement
     //this works by getting the attribute expression on the left and recursively building a "tree" to represent the expr
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
        Value rhs;
        if(ctx.value().INTEGER() !=null){
            rhs = new IntLiteral(Integer.parseInt(ctx.value().INTEGER().getText()));
        } else if(ctx.value().STRING() != null){
            rhs = new StringLiteral(ctx.value().STRING().getText());
        } else if(ctx.value().attribute() != null){
            rhs = new AttributeReference(ctx.value().attribute().tablename != null ? ctx.value().attribute().tablename.getText() : null, ctx.value().attribute().attr.getText());
        }else if (ctx.value().NULL() != null){
            rhs = new NullLiteral();
        }else if (ctx.value().DOUBLE() != null){
            rhs = new DoubleLiteral(Double.parseDouble(ctx.value().DOUBLE().getText()));
        }else {
            throw new RuntimeException("Invalid value in attribute comparison");
        }
        return new AttributeComparisonNode(lhs, op, rhs);
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
    public List<AttributeReference> getAttributeReferences(String attributeList){
        List<AttributeReference> attributes = new ArrayList<>();
        for(String attr: attributeList.split(",")){
            if(attr.trim().isEmpty()){
                throw new RuntimeException("Attribute list in order clause cannot be empty");
            }
            String[] parts = attr.trim().split("\\.");
            attributes.add(new AttributeReference(parts.length > 1 ? parts[0] : null, parts[parts.length - 1]));
        }
        return attributes;
    }
}

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
public class TreeBuilderVisitor extends liteQLBaseVisitor<ASTNode>{

    // dont know if the is necessary either
    @Override
    public ASTNode visitProgram(liteQLParser.ProgramContext ctx){
        for (liteQLParser.StmtContext stmt : ctx.stmt()) {
            visit(stmt);
        }
        return null;
    }

    @Override public ASTNode visitDeleteTable(liteQLParser.DeleteTableContext ctx) { 
        //REMOVE TABLE tablename=ID ';';
        return null;
    }
	
	@Override public ASTNode visitDeleteRow(liteQLParser.DeleteRowContext ctx) { 
        //REMOVE tableSource whereClause ';';
        return null;
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
        List<String> selectedAttributes;
        if(ctx.selectList().getText().equals("*")){
            selectedAttributes = new ArrayList<>();
        }else{
            selectedAttributes = new ArrayList<>(Arrays.asList(ctx.selectList().getText().split(",")));
        }
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
            List<String> joinAttributes;
            if(ctx.joinClause().selectList().getText().equals("*")){
                joinAttributes = new ArrayList<>();
            }else{
                joinAttributes = new ArrayList<>(Arrays.asList(ctx.joinClause().selectList().getText().split(",")));
            }
            join = new JoinNode(ctx.joinClause().tableSource().getText(), ctx.joinClause().attribute().getText(), joinAttributes);
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
            List<String> groupByAttributes= new ArrayList<>(Arrays.asList(ctx.groupClause().attributeList().getText().split(",")));
            HavingNode having = null;
            if(ctx.havingClause() != null){
                having = new HavingNode((ConjoinedComparisonNode) visit(ctx.havingClause().conjoinedAttrComparison()));
            }
            groupBy = new GroupNode(groupByAttributes, having);
        }
        return new SelectNode(mainTableName, selectedAttributes, limit, join, whereCaluse, groupBy, orderBy);
    }
    //create an order node that represents the order clause
    @Override public ASTNode visitOrderClause(liteQLParser.OrderClauseContext ctx) { 
        //ORDER BY attribute (ASC|DESC)? ';';
        String attributeList = ctx.attributeList().getText();
        String order = ctx.order != null ? ctx.order.getText().toUpperCase() : "ASC"; 
        return new OrderNode(new ArrayList<>(Arrays.asList(attributeList.split(","))), order);
    }

    //create a join clause node that represents the join clause
    @Override public ASTNode visitJoinClause(liteQLParser.JoinClauseContext ctx) { 
        //JOIN tableSource ON conjoinedAttrComparison ';';
        String table = ctx.tableSource().getText();
        String onCondition = ctx.attribute().getText();
        if(ctx.selectList().getText().equals("*")){
            return new JoinNode(table, onCondition);
        }
        return new JoinNode(table, onCondition, new ArrayList<>(Arrays.asList(ctx.selectList().getText().split(","))));
     }

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
        AttributeComparisonNode left = (AttributeComparisonNode) visitAttrComparison(ctx.attrComparison());

        // one expresssion
        if (ctx.conjoinedAttrComparison() == null) {
            return left;
        }

        // build "left AND/OR right"
        String conjunction = ctx.conjunction().getText().toUpperCase();
        ConjoinedComparisonNode right = (ConjoinedComparisonNode) visitConjoinedAttrComparison(ctx.conjoinedAttrComparison());

        return new ConjoinedComparisonNode(left, conjunction, right);
    }
    //only used in non select statements
    @Override
    public ASTNode visitAttrComparison(liteQLParser.AttrComparisonContext ctx) {
        String lhs = ctx.attribute().getText();
        String op  = getComparisonSymbol(ctx.comparison().getText());
        Value rhs;
        if(ctx.value().INTEGER() !=null){
            rhs = new IntLiteral(Integer.parseInt(ctx.value().INTEGER().getText()));
        } else if(ctx.value().STRING() != null){
            rhs = new StringLiteral(ctx.value().STRING().getText());
        } else if(ctx.value().attribute() != null){
            rhs = new AttributeReference(ctx.value().attribute().getText());
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
}

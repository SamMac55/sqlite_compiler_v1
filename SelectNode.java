import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
class SelectNode extends ASTNode{
    int limit = -1; // -1 means no limit
    String mainTableName;
    List<AttributeReference> selectedAttributes = new ArrayList<>(); //now making this hold every attribute
    List<JoinNode> join; // empty if no joins
    ConjoinedComparisonNode whereClause; // null if no where clause
    GroupNode groupBy; // null if no group by clause
    OrderNode orderBy; // null if no order by clause
    public SelectNode(String mainTableName, List<AttributeReference> selectedAttributes, int limit, List<JoinNode> join, ConjoinedComparisonNode whereClause, GroupNode groupBy, OrderNode orderBy) {
        this.mainTableName = mainTableName;
        this.selectedAttributes = selectedAttributes;
        this.limit = limit;
        this.join = join;
        this.whereClause = whereClause;
        this.groupBy = groupBy;
        this.orderBy = orderBy;
    }
    @Override
    public boolean validate(Schema schema, List<Schema.Table> tablesInScope) {
        List<Schema.Table> scope = buildScope(schema);
        List<AttributeReference> aggregated = getAggregatedAttributes(scope);
        validateSelectedAttributes(schema, scope);
        validateWhereClause(schema, scope);
        validateGroupBy(schema, scope, aggregated);
        validateOrderBy(schema, scope, aggregated);

        return true;
    }
    @Override
    public String emitSQL() {
        //make the final attribte list
        List<String> finalAttrs = new ArrayList<>();
        for(AttributeReference attr: selectedAttributes){
            if(attr instanceof AllReference){
                if(((AllReference)attr).function==null && attr.tableName.equals(mainTableName)){
                    finalAttrs.add(mainTableName+".*");
                }else{
                    finalAttrs.add("COUNT(*)");
                }
            }else{
                if(attr.getTableName().equals(mainTableName)){
                    String func;
                    if(attr.function !=null){
                        func = getFunction(attr.function);
                        finalAttrs.add( func + "(" +  mainTableName + "." + attr.getName() + ")");
                    }else{
                        finalAttrs.add(mainTableName + "." + attr.getName());
                    }
                }
            }
        }
        for(JoinNode j : join){
            for(AttributeReference attr: selectedAttributes){
                if(attr instanceof AllReference && attr.tableName.equals(j.table)){
                    if(attr.function == null)
                        finalAttrs.add(j.table+".*"); //prevent count * being put on the joined table
                }else{
                    if(attr.getTableName().equals(j.table)){
                        String func;
                        if(attr.function !=null){
                            func = getFunction(attr.function);
                            finalAttrs.add( func + "(" + j.table + "." + attr.getName() + ")");
                        }else{
                            finalAttrs.add(j.table + "." + attr.getName());
                        }
                    }
                }
            }
        }

        String selectClause = "SELECT " + String.join(", ", finalAttrs);
        String joinClause = "";
        for(JoinNode j : join){
            joinClause += j.emitSQL() + " ";
        }
        return selectClause + " FROM " + mainTableName +
            (!join.isEmpty() ? " " + joinClause : "") +
            (whereClause != null ? " WHERE " + whereClause.emitSQL() : "") +
            (groupBy != null ? " " + groupBy.emitSQL() : "") +
            (orderBy != null ? " " + orderBy.emitSQL() : "") +
            (limit != -1 ? " LIMIT " + limit : "") + ";";
    }
    public static String getFunction(String func){
        switch (func){
            case "min":
                return "MIN";
            case "max":
                return "MAX";
            case "total":
                return "SUM";
            case "average":
                return "AVG";
            case "count":
                return "COUNT";
            default: 
                throw new RuntimeException("Function not found " + func);
        }
    }
    private void enforceAggregationMatch(AttributeReference attr,
                                     List<AttributeReference> aggregated,
                                     List<Schema.Table> scope,
                                     String clauseName) {

        for (AttributeReference agg : aggregated) {

            Schema.Attribute aggAttr =
                ASTNode.resolveAttribute(scope, agg.getTableName(), agg.getName());

            Schema.Attribute clauseAttr =
                ASTNode.resolveAttribute(scope, attr.getTableName(), attr.getName());

            if (aggAttr.equals(clauseAttr)) {

                if (attr.function == null) {
                    throw new RuntimeException(
                        "Aggregated attribute " + agg +
                        " must be aggregated in " + clauseName
                    );
                }

                if (!attr.function.equals(agg.function)) {
                    throw new RuntimeException(
                        "Mismatched aggregation in " + clauseName +
                        " for " + agg
                    );
                }
            }
        }
    }
    private boolean isGrouped(AttributeReference attr) {
        if (groupBy == null) return false;

        for (AttributeReference g : groupBy.attributes) {
            if (g.getName().equals(attr.getName()) &&
                Objects.equals(g.getTableName(), attr.getTableName())) {
                return true;
            }
        }
        return false;
    }
    private void validateOrderBy(Schema schema,
                            List<Schema.Table> scope,
                            List<AttributeReference> aggregated) {

        if (orderBy == null) return;

        if (!orderBy.validate(schema, scope)) {
            throw new RuntimeException("Invalid order by clause");
        }

        boolean isGroupedQuery = groupBy != null || !aggregated.isEmpty();

        for (AttributeReference attr : orderBy.attributes) {

            if (isGroupedQuery) {
                boolean isGrouped = isGrouped(attr);
                boolean isAggregated = attr.function != null;

                if (!isGrouped && !isAggregated) {
                    throw new RuntimeException(
                        "ORDER BY attribute must be grouped or aggregated: " + attr
                    );
                }
            }

            enforceAggregationMatch(attr, aggregated, scope, "ORDER BY");
        }
    }
    private void validateHavingClause(List<Schema.Table> scope,
                                List<AttributeReference> aggregated) {

        if (groupBy.having == null) return;

        for (AttributeReference attr :
                groupBy.having.condition.getAttrRefsInComp()) {

            boolean isGrouped = isGrouped(attr);
            boolean isAggregated = attr.function != null;

            if (!isGrouped && !isAggregated) {
                throw new RuntimeException(
                    "HAVING attribute must be grouped or aggregated: " + attr
                );
            }

            enforceAggregationMatch(attr, aggregated, scope, "HAVING");
        }
    }
    private void validateGroupBySelectConsistency() {
        for (AttributeReference selected : selectedAttributes) {
            if (selected instanceof AllReference) continue;

            boolean found = false;

            for (AttributeReference grouped : groupBy.attributes) {
                if (grouped.getName().equals(selected.getName())) {
                    found = true;
                    break;
                }
            }

            if (!found && selected.function == null) {
                throw new RuntimeException(
                    "Non-aggregated column must appear in GROUP BY: " + selected
                );
            }
        }
    }
    private void validateGroupBy(Schema schema,
                            List<Schema.Table> scope,
                            List<AttributeReference> aggregated) {

        if (groupBy == null) return;

        if (!groupBy.validate(schema, scope)) {
            throw new RuntimeException("Invalid group by clause");
        }

        validateGroupBySelectConsistency();
        validateHavingClause(scope, aggregated);
    }
    private void validateWhereClause(Schema schema, List<Schema.Table> scope) {
        if (whereClause == null) return;

        if (!whereClause.validate(schema, scope)) {
            throw new RuntimeException("Invalid where clause");
        }

        for (AttributeReference attr : whereClause.getAttrRefsInComp()) {
            if (attr.function != null) {
                throw new RuntimeException(
                    "Aggregations are not allowed in WHERE: " + attr
                );
            }
        }
    }
    private void validateSelectedAttributes(Schema schema, List<Schema.Table> scope) {
        for (AttributeReference attr : selectedAttributes) {
            if (attr instanceof AllReference) continue;

            Schema.Attribute curr =
                ASTNode.resolveAttribute(scope, attr.getTableName(), attr.getName());

            if (curr == null) {
                throw new RuntimeException("Selected attribute not found: " + attr);
            }

            if (attr.function != null &&
                !(curr.type.equals("REAL") || curr.type.equals("INTEGER")) &&
                (attr.function.equals("total") || attr.function.equals("avg"))) {

                throw new RuntimeException(
                    "Cannot aggregate non-numeric attribute: " + attr
                );
            }
        }
    }
    private List<AttributeReference> getAggregatedAttributes(List<Schema.Table> scope) {
        List<AttributeReference> aggregated = new ArrayList<>();
        List<String> functions = new ArrayList<>();
        for (AttributeReference attr : selectedAttributes) {
            if (attr.function != null) {
                aggregated.add(attr);
                functions.add(attr.function);
            }
        }
        if(validateAggregatedAttributes(scope,aggregated, functions)) return aggregated;
        else throw new RuntimeException("Issue with aggregated function list");
    }
    private boolean validateAggregatedAttributes(List<Schema.Table> scope,List<AttributeReference> attrs, List<String> funcs){
        if(attrs.size()!= funcs.size()){return false;}
        for(int i = 0; i< attrs.size(); i++){
            if(funcs.get(i).equals("total") || funcs.get(i).equals("average")){
                if(resolveAttribute(scope,attrs.get(i).getTableName(),attrs.get(i).getName()).type.equals("TEXT")){
                    throw new RuntimeException("Cannot sum or average text attribute: " + attrs.get(i).getName());
                }
            }
        }
        return true;
    }
    private List<Schema.Table> buildScope(Schema schema) {
        List<Schema.Table> scope = new ArrayList<>();

        Schema.Table main = schema.getTable(mainTableName);
        if (main == null) {
            throw new RuntimeException("Main table not found: " + mainTableName);
        }
        scope.add(main);

        for (JoinNode j : join) {
            Schema.Table joined = schema.getTable(j.table);
            if (joined == null) {
                throw new RuntimeException("Joined table not found: " + j.table);
            }
            scope.add(joined);

            if (!j.validate(schema, scope)) {
                throw new RuntimeException("Invalid join clause for table: " + j.table);
            }
        }

        return scope;
    }
}
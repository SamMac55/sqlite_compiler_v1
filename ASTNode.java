import java.util.List;
import java.util.ArrayList;
public abstract class ASTNode {
    public abstract boolean validate(Schema schema, List<Schema.Table> tablesInScope); // may make this method take a table name as an argument
    public abstract String emitSQL();
    public static Schema.Attribute resolveAttribute(List<Schema.Table> scope, String name) {
        Schema.Attribute found = null;

        for (Schema.Table t : scope) {
            if (t.hasAttribute(name)) {
                if (found != null) {
                    throw new RuntimeException("Ambiguous attribute: " + name);
                }
                found = t.getAttribute(name);
            }
        }

        return found;
    }
    public static Schema.Attribute resolveAttribute(List<Schema.Table> scope, String tableName, String attrName) {
        if(tableName!=null){
            Schema.Table t = null;
            for(Schema.Table table: scope){
                if(table.table_name.equals(tableName)){
                    t = table;
                    break;
                }
            }
            if (t != null) {
                if (!t.hasAttribute(attrName)) {
                    throw new RuntimeException("Attribute not found in table: " + tableName + "." + attrName);
                }
                return t.getAttribute(attrName);
            } else {
                throw new RuntimeException("Table not found in scope: " + tableName);
            }
        }else{
            return resolveAttribute(scope, attrName);
        }
    }
}


class AttributeComparisonNode extends ASTNode {
    AttributeReference lhs;
    String op;
    Value rhs;
    public AttributeComparisonNode(AttributeReference lhs, String op, Value rhs) {
        this.lhs = lhs;
        this.op = op;
        this.rhs = rhs;
    }
    @Override
    public boolean validate(Schema schema, List<Schema.Table> tablesInScope) {
        Schema.Attribute validAttr = ASTNode.resolveAttribute(tablesInScope, lhs.tableName, lhs.value);
        if(validAttr != null ){
            if(this.rhs instanceof NullLiteral && !validAttr.hasConstraint("NOTNULL") && (op.equals("=") || op.equals("!="))){
                return true;
            }
            if (rhs instanceof AttributeReference ref) {
                Schema.Attribute rhsAttr = ASTNode.resolveAttribute(tablesInScope, ref.tableName, ref.value);

                if (rhsAttr == null) throw new RuntimeException("Invalid attribute in rhs of comparison: " + ref.value);

                return validAttr.type.equals(rhsAttr.type);
            }
            String type = validAttr.type;
            String rhsType = this.rhs.getValueType();
            if ((type.equals("REAL") || type.equals("INTEGER")) &&
                (rhsType.equals("REAL") || rhsType.equals("INTEGER"))) {
                return true;
            }
            if(type.equals(rhsType)){
                return true;
            }
        }
        throw new RuntimeException("Invalid attribute in lhs of comparison: " + lhs.value);
    }
    @Override public String emitSQL() {
        String lhsStr = (lhs.tableName != null)
            ? lhs.tableName + "." + lhs.value
            : lhs.value;

        String rhsStr;
        if (rhs instanceof AttributeReference ref) {
            rhsStr = (ref.tableName != null)
                ? ref.tableName + "." + ref.value
                : ref.value;
        } else {
            rhsStr = rhs.getValue().toString();
        }

        return lhsStr + " " + op + " " + rhsStr;
    }
}

class ConjoinedComparisonNode extends ASTNode{
    AttributeComparisonNode left;
    String conjunction; // "AND" or "OR"
    ConjoinedComparisonNode right; // null if there is only one comparison
    public ConjoinedComparisonNode(AttributeComparisonNode left, String conjunction, ConjoinedComparisonNode right) {
        this.left = left;
        this.conjunction = conjunction;
        this.right = right;
    }
    @Override
    public boolean validate(Schema schema, List<Schema.Table> tablesInScope) {
        if (right == null && conjunction == null) {
            return left.validate(schema, tablesInScope);
        }

        if (right == null) {
            throw new RuntimeException("Hanging conjunction in attribute comparison: " + conjunction);
        }

        boolean leftValid  = left.validate(schema, tablesInScope);
        boolean rightValid = right.validate(schema, tablesInScope);

        return leftValid && rightValid;
    }
    @Override public String emitSQL() {
        if(right == null){
            return left.emitSQL();
        }
        return left.emitSQL() + " " + conjunction + " " + right.emitSQL();
    }
}

class SelectNode extends ASTNode{
    int limit = -1; // -1 means no limit
    String mainTableName;
    List<AttributeReference> selectedAttributes = new ArrayList<>(); // empty list means select *
    JoinNode join; // null if no join
    ConjoinedComparisonNode whereClause; // null if no where clause
    GroupNode groupBy; // null if no group by clause
    OrderNode orderBy; // null if no order by clause
    public SelectNode(String mainTableName, List<AttributeReference> selectedAttributes, int limit, JoinNode join, ConjoinedComparisonNode whereClause, GroupNode groupBy, OrderNode orderBy) {
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
        List<Schema.Table> scope = new ArrayList<>();

        Schema.Table main = schema.getTable(mainTableName);
        if (main == null) throw new RuntimeException("Main table not found: " + mainTableName);
        scope.add(main);

        if (join != null) {
            Schema.Table joined = schema.getTable(join.table);
            if (joined == null) throw new RuntimeException("Joined table not found: " + join.table);
            scope.add(joined);
            if(!join.validate(schema,scope)) throw new RuntimeException("Invalid join clause");
        }

        if (whereClause != null) {
            if (!whereClause.validate(schema, scope)) throw new RuntimeException("Invalid where clause in select statement");
        }

        if(orderBy != null){
            if(!orderBy.validate(schema, scope)) throw new RuntimeException("Invalid order by clause in select statement");
        }

        for(AttributeReference attr: selectedAttributes){
            if (ASTNode.resolveAttribute(scope, mainTableName, attr.getName()) == null) {
                throw new RuntimeException("Selected attribute not found in main table: " + attr);
            }
        }
        ArrayList<AttributeReference> allSelected = new ArrayList<>(selectedAttributes);
        allSelected.addAll(selectedAttributes);
        if(groupBy != null){
            if(!groupBy.validate(schema, scope)) throw new RuntimeException("Invalid group by clause in select statement");
            allSelected.addAll(join == null || join.selectedAttributes == null ? new ArrayList<>() : join.selectedAttributes);
            for(AttributeReference attr: groupBy.attributes){
                boolean found = false;
                for(AttributeReference selected : allSelected){
                    if(selected.getName().equals(attr.getName())){
                        found = true;
                        break;
                    }
                }
                if(!found){
                    throw new RuntimeException("Non aggregate attribute in group by clause: " + attr);
                }
            }
        }
        return true;
    }
    @Override
    public String emitSQL() {
        List<String> finalAttrs = new ArrayList<>();

        boolean mainAll = selectedAttributes.isEmpty();
        boolean joinAll = (join != null && join.getSelectedAttributes().isEmpty());

        // Case 1: both are *
        if (mainAll && (join == null || joinAll)) {
            return "SELECT * FROM " + mainTableName +
                (join != null ? " " + join.emitSQL() : "") +
                (whereClause != null ? " WHERE " + whereClause.emitSQL() : "") +
                (groupBy != null ? " " + groupBy.emitSQL() : "") +
                (orderBy != null ? " " + orderBy.emitSQL() : "") +
                (limit != -1 ? " LIMIT " + limit : "") + ";";
        }

        // Main attributes
        if (!mainAll) {
            for (AttributeReference attr : selectedAttributes) {
                finalAttrs.add(mainTableName + "." + attr.getName());
            }
        } else if (join != null && !joinAll) {
            finalAttrs.add(mainTableName + ".*");
        }

        // Join attributes
        if (join != null) {
            if (!joinAll) {
                for (AttributeReference attr : join.getSelectedAttributes()) {
                    finalAttrs.add(join.table + "." + attr.getName());
                }
            } else if (!mainAll) {
                finalAttrs.add(join.table + ".*");
            }
        }

        String selectClause = "SELECT " + String.join(", ", finalAttrs);

        return selectClause + " FROM " + mainTableName +
            (join != null ? " " + join.emitSQL() : "") +
            (whereClause != null ? " WHERE " + whereClause.emitSQL() : "") +
            (groupBy != null ? " " + groupBy.emitSQL() : "") +
            (orderBy != null ? " " + orderBy.emitSQL() : "") +
            (limit != -1 ? " LIMIT " + limit : "") + ";";
    }
}

class JoinNode extends ASTNode{
    String table;
    String onCondition;
    List<AttributeReference> selectedAttributes; // empty list means select *
    String mainTable;

    public JoinNode(String mainTable, String table, String onCondition, List<AttributeReference> selectedAttributes) {
        this.mainTable = mainTable;
        this.table = table;
        this.onCondition = onCondition;
        this.selectedAttributes = selectedAttributes;
    }
    public JoinNode(String mainTable, String table, String onCondition) {
        this.mainTable = mainTable;
        this.table = table;
        this.onCondition = onCondition;
        this.selectedAttributes = new ArrayList<>();
    }
    @Override
    public boolean validate(Schema schema, List<Schema.Table> tablesInScope) {
        if(schema.getTable(table) == null){
            throw new RuntimeException("Joined table not found in schema: " + table);
        }
        for(Schema.Table t: tablesInScope){
            if(!t.hasAttribute(onCondition)){
                throw new RuntimeException("Invalid attribute in join condition, attribute must be in both main and joined tables: " + onCondition);
            }
        }
        for(AttributeReference attr: selectedAttributes){
            if(!schema.getTable(table).hasAttribute(attr.getName())){
                throw new RuntimeException("Selected attribute not found in joined table: " + attr.getName());
            }
        }
        return true;
    }
    @Override
    public String emitSQL() {
        return "JOIN " + table +
            " ON " + table + "." + onCondition +
            " = " + mainTable + "." + onCondition;
    }
    public List<AttributeReference> getSelectedAttributes() {
        return selectedAttributes;
    }
}

class OrderNode extends ASTNode{
    List<AttributeReference> attributes; // attributes to order by
    String order; // "ASC" or "DESC"
    public OrderNode(List<AttributeReference> attributes, String order) {
        this.attributes = attributes;
        this.order = order;
    }
    @Override
    public boolean validate(Schema schema, List<Schema.Table> tablesInScope) {
        for(AttributeReference attr: attributes){
            boolean found = false;
            if(resolveAttribute(tablesInScope, attr.getTableName(), attr.getName()) != null){
                found = true;
            }
            if(!found){
                throw new RuntimeException("Attribute not found in any table: " + attr.getName());
            }
        }
        return true;
    }
    @Override public String emitSQL() {
        StringBuilder sb = new StringBuilder();
        sb.append("ORDER BY ");
        for(int i = 0; i < attributes.size(); i++){
            AttributeReference attr = attributes.get(i);
            sb.append(attr.toString());
            if(i < attributes.size() - 1){
                sb.append(", ");
            }
        }
        sb.append(" ").append(order);
        return sb.toString();
    }
}

class GroupNode extends ASTNode{
    List<AttributeReference> attributes; // attributes to group by
    HavingNode having; // null if no having clause
    public GroupNode(List<AttributeReference> attributes, HavingNode having) {
        this.attributes = attributes;
        this.having = having;
    }
    @Override
    public boolean validate(Schema schema, List<Schema.Table> tablesInScope) {
        if(having != null && !having.validate(schema,tablesInScope))
            throw new RuntimeException("Invalid having clause for select statement\'s groupby");
        for(AttributeReference attr: attributes){
            boolean found = false;

            if(resolveAttribute(tablesInScope, attr.getTableName(), attr.getName()) != null){
                found = true;
            }
            if(!found){
                throw new RuntimeException("Attribute not found in any table: " + attr.toString());
            }
        }
        return true;
    }
    @Override public String emitSQL() {
        StringBuilder sb = new StringBuilder();
        sb.append("GROUP BY ");
        for(int i = 0; i < attributes.size(); i++){
            AttributeReference attr = attributes.get(i);
            sb.append(attr.toString());
            if(i < attributes.size() - 1){
                sb.append(", ");
            }
        }
        if(having != null){
            sb.append(" ").append(having.emitSQL());
        }
        return sb.toString();
        // emit the SQL representation of the group by statement
    }
}

class HavingNode extends ASTNode{
    ConjoinedComparisonNode condition; // condition for the having clause
    public HavingNode(ConjoinedComparisonNode condition) {
        this.condition = condition;
    }
    @Override
    public boolean validate(Schema schema, List<Schema.Table> tablesInScope) {
        if(!condition.validate(schema, tablesInScope))
            throw new RuntimeException("Invalid having clause for group by");
        return true;
    }
    @Override public String emitSQL() {
        return "HAVING " + condition.emitSQL();
    }
}

class DeleteTableNode extends ASTNode{
    String tableID;
    public DeleteTableNode(String tableID) {
        this.tableID = tableID;
    }
    @Override public boolean validate(Schema schema, List<Schema.Table> tablesInScope) {
        if(schema.getTable(tableID) != null){
            schema.tables.remove(schema.getTable(tableID));
            return true;
        }
        throw new RuntimeException("Cannot delete table that does not exist in schema: " + tableID);
    }
    @Override public String emitSQL() {
        return "DROP TABLE " + tableID + ";";
    }
}

class DeleteRowNode extends ASTNode{
    String tableID;
    ConjoinedComparisonNode whereClause; // null if no where clause
    public DeleteRowNode(String tableID, ConjoinedComparisonNode whereClause) {
        this.tableID = tableID;
        this.whereClause = whereClause;
    }
    @Override public boolean validate(Schema schema, List<Schema.Table> tablesInScope) {
        Schema.Table t = schema.getTable(tableID);
        if(t == null){
            throw new RuntimeException("Cannot delete a row from a table that does not exist: " + tableID);
        }
        List<Schema.Table> scope = new ArrayList<>();
        scope.add(t);
        if(whereClause != null && !whereClause.validate(schema, scope)){
            throw new RuntimeException("Invalid where clause in delete statement");
        }
        return true;
    }
    @Override public String emitSQL() {
        return "DELETE FROM " + tableID +
            (whereClause != null ? " WHERE " + whereClause.emitSQL() : "") + ";";
    }
}
class UpdateNode extends ASTNode{
    String tableID;
    AssignmentListNode assignments;
    ConjoinedComparisonNode whereClause; // null if no where clause
    public UpdateNode(String tableID, AssignmentListNode assignments, ConjoinedComparisonNode whereClause) {
        this.tableID = tableID;
        this.assignments = assignments;
        this.whereClause = whereClause;
     }
    @Override
    public boolean validate(Schema schema, List<Schema.Table> tablesInScope) {
        Schema.Table t = schema.getTable(tableID);
        if(t == null){
            throw new RuntimeException("Cannot update row for table that does not exist: " + tableID);
        }
        List<Schema.Table> scope = new ArrayList<>();
        scope.add(t);
        if(!assignments.validate(schema, scope)){
            throw new RuntimeException("Invalid assignment list in update statement");
        }
        if(whereClause != null && !whereClause.validate(schema, scope)){
            throw new RuntimeException("Invalid where clause in update statement");
        }
        return true;
    }
    @Override
    public String emitSQL() {
        return "UPDATE " + tableID +
            " SET " + assignments.emitSQL() +
            (whereClause != null ? " WHERE " + whereClause.emitSQL() : "") + ";";
    }
}
class InsertNode extends ASTNode{
    String tableID;
    AssignmentListNode assignments;
    public InsertNode(String tableID, AssignmentListNode assignments) {
        this.tableID = tableID;
        this.assignments = assignments;
     }
    @Override
    public boolean validate(Schema schema, List<Schema.Table> tablesInScope) {
        Schema.Table t = schema.getTable(tableID);
        if(t == null){
            throw new RuntimeException("Cannot insert into a table that does not exist: " + tableID);
        }
        List<Schema.Table> scope = new ArrayList<>();
        scope.add(t);
        if(!assignments.validate(schema, scope)){
            throw new RuntimeException("Invalid assignment list in insert statement");
        }
        return true;
    }
    @Override
    public String emitSQL() {
        StringBuilder sb = new StringBuilder();
         sb.append("INSERT INTO ").append(tableID).append(" (");
         List<String> columns = new ArrayList<>();
         List<String> values = new ArrayList<>();
         for(AssignmentStatementNode assignment: assignments.assignments){
             columns.add(assignment.attribute.getName());
             values.add(assignment.value.getValue().toString());
         }
         sb.append(String.join(", ", columns));
         sb.append(") VALUES (");
         sb.append(String.join(", ", values));
         sb.append(");");
         return sb.toString();
    }
}

class AssignmentStatementNode extends ASTNode{
    AttributeReference attribute;
    Value value;
    public AssignmentStatementNode(AttributeReference attribute, Value value) {
        this.attribute = attribute;
        this.value = value;
    }
    @Override
    public boolean validate(Schema schema, List<Schema.Table> tablesInScope) {
        Schema.Attribute attr = ASTNode.resolveAttribute(tablesInScope, attribute.getTableName(), attribute.getName());
        if(attr == null){
            throw new RuntimeException("Attribute not found in schema: " + attribute.toString());
        }
        String type = attr.type;
        String valueType = value.getValueType();
        if ((type.equals("REAL") || type.equals("INTEGER")) &&
            (valueType.equals("REAL") || valueType.equals("INTEGER"))) {
            return true;
        }
        if(type.equals(valueType)){
            return true;
        }
        throw new RuntimeException("Type mismatch in assignment: cannot assign " + valueType + " to " + type);
    }
    @Override
    public String emitSQL() {
        return attribute.getName() + " = " + value.getValue().toString();
    }
}

class AssignmentListNode extends ASTNode{
    List<AssignmentStatementNode> assignments;
    public AssignmentListNode(List<AssignmentStatementNode> assignments) {
        this.assignments = assignments;
    }
    @Override
    public boolean validate(Schema schema, List<Schema.Table> tablesInScope) {
        for(AssignmentStatementNode assignment: assignments){
            if(!assignment.validate(schema, tablesInScope)){
                throw new RuntimeException("Invalid assignment in set clause for update statement");
            }
        }
        return true;
    }
    @Override
    public String emitSQL() {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < assignments.size(); i++){
            sb.append(assignments.get(i).emitSQL());
            if(i < assignments.size() - 1){
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}

class CreateTableNode extends ASTNode{
    List<CreateAttributeNode> attributes;
    String tableID;
    public CreateTableNode(String tableID, List<CreateAttributeNode> attributes) {
        this.tableID = tableID;
        this.attributes = attributes;
    }
    @Override
    public boolean validate(Schema schema, List<Schema.Table> tablesInScope) {
        if(schema.hasTable(tableID)){
            throw new RuntimeException("Table already exists in schema: " + tableID);
        }
        for(CreateAttributeNode attribute: attributes){
            if(!attribute.validate(schema, tablesInScope)){
                throw new RuntimeException("Invalid attribute in create table statement");
            }
        }
        createTable();
        return true;
    }
    @Override
    public String emitSQL() {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(tableID).append(" (\n");
        for(int i = 0; i < attributes.size(); i++){
            sb.append("\t").append(attributes.get(i).emitSQL());
            if(i < attributes.size() - 1){
                sb.append(",\n ");
            }
        }
        sb.append(");");
        return sb.toString();
    }
    private void createTable(){
        Schema.Table newTable = new Schema.Table(tableID);
        for(CreateAttributeNode attrNode: attributes){
            ArrayList<String> constraints = new ArrayList<>();
            for(String constraint: attrNode.constraints){
                constraints.add(constraint);
            }
            for(String fk : attrNode.fkReferences){
                constraints.add(fk);
            }
            Schema.Attribute newAttr = new Schema.Attribute(attrNode.name, attrNode.type, constraints);
            newTable.attributes.add(newAttr);
        }
        Schema.instance.tables.add(newTable);
    }
}
class CreateAttributeNode extends ASTNode{
    String name;
    String type;
    List<String> constraints;
    List<String> fkReferences; // list of tables this attribute has foreign key references to
    public CreateAttributeNode(String name, String type, List<String> constraints, List<String> fkReferences) {
        this.name = name;
        this.type = type;
        this.constraints = constraints;
        this.fkReferences = fkReferences;
    }
    @Override
    public boolean validate(Schema schema, List<Schema.Table> tablesInScope) {
        if(!type.equals("INTEGER") && !type.equals("REAL") && !type.equals("TEXT")){
            throw new RuntimeException("Invalid/unsupported attribute type: " + type);
        }
        for(String constraint: constraints) {
            if (!constraint.equals("NOTNULL") && !constraint.equals("PRIMARYKEY")) {
                throw new RuntimeException("Invalid/unsupported constraint: " + constraint);
            }
        }
        for(String fk : fkReferences){
            if(fk.startsWith("references ")){
                String refTableName = fk.substring("references ".length());
                if(schema.getTable(refTableName) == null){
                    throw new RuntimeException("Referenced table not found in schema: " + refTableName);
                }else if(schema.getTable(refTableName).getAttribute(name) == null){
                    throw new RuntimeException("Referenced attribute not found in referenced table: " + name);
                }else if(!schema.getTable(refTableName).getAttribute(name).type.equals(type)){
                    throw new RuntimeException("Type mismatch in foreign key reference: " + type + " vs " + schema.getTable(refTableName).getAttribute(name).type);
                }
            }
        }
        return true;
    }
    @Override
    public String emitSQL() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" ").append(type);
        for(String constraint: constraints){
            sb.append(" ").append(getConstraint(constraint));
        }
        for(String fk : fkReferences){
            if(fk.startsWith("references ")){
                String refTableName = fk.substring("references ".length());
                sb.append(" REFERENCES ").append(refTableName).append("(").append(name).append(")");
            }
        }
        return sb.toString();
    }
    public static String getConstraint(String constraint){
        if(constraint.equals("NOTNULL")){
            return "NOT NULL";
        } else if(constraint.equals("PRIMARYKEY")){
            return "PRIMARY KEY";
        } else {
            throw new RuntimeException("Invalid/unsupported constraint in create table statement");
        }
    }
}
// need to be able to distinguish what a value is
abstract class Value{
    public abstract Object getValue();
    public abstract String getValueType();
}

class IntLiteral extends Value{
    Integer value;
    public IntLiteral(Integer value) {
        this.value = value;
    }
    @Override
    public String getValueType() {
        return "INTEGER";
    }
    @Override
    public Object getValue() {
        return value;
    }
}

class DoubleLiteral extends Value{
    Double value;
    public DoubleLiteral(Double value) {
        this.value = value;
    }
    @Override
    public String getValueType() {
        return "REAL";
    }
    @Override
    public Object getValue() {
        return value;
    }
}

class StringLiteral extends Value{
    String value;
    public StringLiteral(String value) {
        this.value = value;
    }
    @Override
    public String getValueType() {
        return "TEXT";
    }
    @Override
    public Object getValue() {
        return value;
    }
}

class NullLiteral extends Value{
    Object value;
    public NullLiteral() {
        value = null;
    }
    @Override
    public String getValueType() {
        return "NULL";
    }
    @Override
    public Object getValue() {
        return value;
    }
}

class AttributeReference extends Value{
    String tableName; // null if no table specified
    String value;
    public AttributeReference(String tableName, String value) {
        this.tableName = tableName;
        this.value = value;
    }
    @Override
    public String getValueType() {
        return "ATTRIBUTE";
    }
    @Override
    public Object getValue() {
        return value;
    }
    public String getName() {
        return value;
    }
    public String getTableName() {
        return tableName;
    }
    public String toString() {
        return (tableName != null ? tableName + "." : "") + value;
    }
}
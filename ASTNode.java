import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
public abstract class ASTNode {
    //validate is the method that allows each node to see if it is correct semantically or not
    //needs both the schema and the tables that are in the current scope.
    public abstract boolean validate(Schema schema, List<Schema.Table> tablesInScope);
    //emitSQL is a basic method that returns the SQL string representation of the node
    public abstract String emitSQL();
    //resolve attribute is a helper method used in some nodes to find the attribute in a table + check for ambiguity
    public static Schema.Attribute resolveAttribute(List<Schema.Table> scope, String name) {
        Schema.Attribute found = null;
        //look through each table in the scope
        for (Schema.Table t : scope) {
            if (t.hasAttribute(name)) {
                if (found != null) { //if we've already found it that means it is ambiguous (needs a tablename qualified)
                    throw new RuntimeException("Ambiguous attribute: " + name);
                }
                found = t.getAttribute(name);
            }
        }
        return found;
    }
    //overloaded version that takes a table name to check if the attribute exists in a specific table
    //if no table name is provided it simply calls the previous version
    public static Schema.Attribute resolveAttribute(List<Schema.Table> scope, String tableName, String attrName) {
        //first get the table from the scope (we need to ensure that the table exists)
        if(tableName!=null){
            Schema.Table t = null;
            for(Schema.Table table: scope){
                if(table.table_name.equals(tableName)){
                    t = table;
                    break;
                }
            }
            //if we cant find the attribute in the table specified throw an error
            if (t != null && !t.hasAttribute(attrName)) {
                throw new RuntimeException("Attribute not found in table: " + tableName + "." + attrName);
            //if not we are all good
            }else if(t!=null){
                return t.getAttribute(attrName);
            //if the table variable is null then the table doesn't even exist in the scope
            }else {
                throw new RuntimeException("Table not found in scope: " + tableName);
            }
        }else{ //call overloaded version
            return resolveAttribute(scope, attrName);
        }
    }
}

// nodes for things like employee_id is 5
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
            //if its a null value as long as we aren't trying to do > < >= <= its valid
            if(this.rhs instanceof NullLiteral && !validAttr.hasConstraint("NOTNULL") && (op.equals("=") || op.equals("!="))){
                return true;
            }
            //check the right hand side of the expression, if its an attribute we need to make sure that it is a valid one
            //and that the type is valid as well
            if (rhs instanceof AttributeReference ref) {
                Schema.Attribute rhsAttr = ASTNode.resolveAttribute(tablesInScope, ref.tableName, ref.value);

                if (rhsAttr == null) throw new RuntimeException("Invalid attribute in rhs of comparison: " + ref.value);
                //make sure right hand side attribute is valid type for left hand side
                if((validAttr.type.equals("REAL") || validAttr.type.equals("INTEGER")) 
                    && (rhsAttr.type.equals("REAL") || rhsAttr.type.equals("INTEGER")) ) 
                    return true;

                if(!validAttr.type.equals(rhsAttr.type)) throw new RuntimeException("Attributes in comparison must be of the same type.");
                else return true;
            }
            String type = validAttr.type;
            String rhsType = this.rhs.getValueType();
            //numbers can be compared to numbers regardless of if they are ints or doubles (in sqlite)
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
        String actualOp = op;
        String lhsStr = (lhs.tableName != null)
            ? lhs.tableName + "." + lhs.value
            : lhs.value;
        String rhsStr;
        if (rhs instanceof AttributeReference ref) {
            rhsStr = (ref.tableName != null)
                ? ref.tableName + "." + ref.value
                : ref.value;
        } else {
            if(rhs.getValue() == null){
                rhsStr = "NULL";
                if(op.equals("=")) actualOp = "IS";
                else if(op.equals("!=")) actualOp = "IS NOT";
            }else {
                rhsStr = rhs.getValue().toString();
            }
        }

        return lhsStr + " " + actualOp + " " + rhsStr;
    }
}
// for things like employee_id greater than 5 or first_name is 'Sam'
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
        //if we are at the base case just validate that expression
        if (right == null && conjunction == null) {
            return left.validate(schema, tablesInScope);
        }
        // if we have a conjunction but no rhs, throw error
        if (right == null) {
            throw new RuntimeException("Hanging conjunction in attribute comparison: " + conjunction);
        }
        //validate both sides of equation
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

    //helper method for emit/validations in selectNode.java that gets all of the attribute references in the comparisons
    public List<AttributeReference> getAttrRefsInComp() {
        Set<AttributeReference> refs = new HashSet<>();

        collectRecursive(this, refs);

        return new ArrayList<>(refs);
    }

    //helper method that gets all of the attribute ref
    private void collectRecursive(ConjoinedComparisonNode node, Set<AttributeReference> refs) {
        if (node == null) return;

        collectFromComparison(node.left, refs);

        if (node.right != null) {
            collectRecursive(node.right, refs);
        }
    }
    // helper method when we compare two attributes together
    private void collectFromComparison(AttributeComparisonNode comp, Set<AttributeReference> refs) {
        if (comp == null) return;

        // LHS is always an attribute
        refs.add(comp.lhs);

        // RHS might be an attribute
        if (comp.rhs instanceof AttributeReference ref) {
            refs.add(ref);
        }
    }
}
// join cluase of select statments
class JoinNode extends ASTNode{
    String table;// table being joined
    //ex employees all with departments using employees.department_id
    String onCondition; //conditin of the join, from example: onCondition=department_id
    String onConditionTable; //table recieving added table, from example: onConditionTable= employees
    public JoinNode(String table, String onCondition, String onConditionTable) {
        this.table = table;
        this.onCondition = onCondition;
        this.onConditionTable = onConditionTable;
    }

    @Override
    public boolean validate(Schema schema, List<Schema.Table> tablesInScope) {
        Schema.Table joinedTable = schema.getTable(table);
        Schema.Table conditionTable = schema.getTable(onConditionTable);
        //make sure both tables are valid
        if(joinedTable == null){ 
            throw new RuntimeException("Joined table not found in schema: " + table);
        }
        if(conditionTable==null){
            throw new RuntimeException("Table with condition to join not found in schema: " + onConditionTable);
        }
        //if both tables are valid and in current scope, do they both have the condition attribute?
        if(tablesInScope.contains(conditionTable) && tablesInScope.contains(joinedTable)){
            if(conditionTable.hasAttribute(onCondition) && joinedTable.hasAttribute(onCondition)){
                return true;
            }
            throw new RuntimeException("Joined table and Condition Table do not both contain attribute: " + onCondition); //they don't both have condition attribute
        }else{
            throw new RuntimeException("Schema does not contain both Condition Table: " + conditionTable + " and Joined Table: " + joinedTable ); //one of or both of the tables are not in schema
        }
    }
    @Override
    public String emitSQL() {
        return "JOIN " + table +
            " ON " + table + "." + onCondition +
            " = " + onConditionTable + "." + onCondition;
    }
}
// order clause of select statement
class OrderNode extends ASTNode{
    List<AttributeReference> attributes; // attributes to order by
    String order; // "ASC" or "DESC"
    public OrderNode(List<AttributeReference> attributes, String order) {
        this.attributes = attributes;
        this.order = order;
    }
    @Override
    public boolean validate(Schema schema, List<Schema.Table> tablesInScope) {
        //validate by ensuring that the table is in the scope
        for(AttributeReference attr: attributes){
            boolean found = false;
            if(resolveAttribute(tablesInScope, attr.getTableName(), attr.getName()) != null){
                found = true;
            }
            if(!found){
                throw new RuntimeException("ORDER BY: Attribute not found in any table: " + attr.getName());
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
//group clause of select statemnt
class GroupNode extends ASTNode{
    List<AttributeReference> attributes; // attributes to group by
    HavingNode having; // null if no having clause
    public GroupNode(List<AttributeReference> attributes, HavingNode having) {
        this.attributes = attributes;
        this.having = having;
    }
    @Override
    public boolean validate(Schema schema, List<Schema.Table> tablesInScope) {
        //if we have a having clause is it valid? 
        if(having != null && !having.validate(schema,tablesInScope))
            throw new RuntimeException("Invalid having clause for select statement\'s groupby");
        //find the attributes + ensure no ambiguity
        for(AttributeReference attr: attributes){
            boolean found = false;

            if(resolveAttribute(tablesInScope, attr.getTableName(), attr.getName()) != null){
                found = true;
            }
            if(!found){
                throw new RuntimeException("GROUP BY: Attribute not found in any table: " + attr.toString());
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
//having clause (sub clause of group by)
class HavingNode extends ASTNode{
    ConjoinedComparisonNode condition; // condition for the having clause
    public HavingNode(ConjoinedComparisonNode condition) {
        this.condition = condition;
    }
    @Override
    public boolean validate(Schema schema, List<Schema.Table> tablesInScope) {
        //is the condition of the having valid?
        if(!condition.validate(schema, tablesInScope))
            throw new RuntimeException("Invalid having clause for group by");
        return true;
    }
    @Override public String emitSQL() {
        return "HAVING " + condition.emitSQL();
    }
}
//delete table statement
class DeleteTableNode extends ASTNode{
    String tableID;
    public DeleteTableNode(String tableID) {
        this.tableID = tableID;
    }
    @Override public boolean validate(Schema schema, List<Schema.Table> tablesInScope) {
        //if the table is in the schema, safely remove it
        if(schema.getTable(tableID) != null){
            schema.tables.remove(schema.getTable(tableID));
            return true;
        }
        //if not throw an exception. An alternative to this is to instead emit "DROP TABLE IF EXISTS"
        throw new RuntimeException("Cannot delete table that does not exist in schema: " + tableID);
    }
    @Override public String emitSQL() {
        return "DROP TABLE " + tableID + ";";
    }
}
//Delete row statement
class DeleteRowNode extends ASTNode{
    String tableID;
    ConjoinedComparisonNode whereClause; // null if no where clause
    public DeleteRowNode(String tableID, ConjoinedComparisonNode whereClause) {
        this.tableID = tableID;
        this.whereClause = whereClause;
    }
    @Override public boolean validate(Schema schema, List<Schema.Table> tablesInScope) {
        //make sure the table exists
        Schema.Table t = schema.getTable(tableID);
        if(t == null){
            throw new RuntimeException("Cannot delete a row from a table that does not exist: " + tableID);
        }
        //add the valid table to the scope so the where clause can properly validate
        List<Schema.Table> scope = new ArrayList<>();
        scope.add(t);
        //if there is a where clause is it valid?
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
//update statement
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
        //does this table exist?
        Schema.Table t = schema.getTable(tableID);
        if(t == null){
            throw new RuntimeException("Cannot update row for table that does not exist: " + tableID);
        }
        //if it does exist add it to scope for proper validations
        List<Schema.Table> scope = new ArrayList<>();
        scope.add(t);
        //are the assignments in the table valid?
        if(!assignments.validate(schema, scope)){
            throw new RuntimeException("Invalid assignment list in update statement");
        }
        //is there where clause (if it exists) valid?
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
    ArrayList<AssignmentListNode> assignments;
    public InsertNode(String tableID, ArrayList<AssignmentListNode> assignments) {
        this.tableID = tableID;
        this.assignments = assignments;
     }
    @Override
    public boolean validate(Schema schema, List<Schema.Table> tablesInScope) {
        //does this table exist?
        Schema.Table t = schema.getTable(tableID);
        if(t == null){
            throw new RuntimeException("Cannot insert into a table that does not exist: " + tableID);
        }
        //if it does exist add it to scope for validations
        List<Schema.Table> scope = new ArrayList<>();
        scope.add(t);
        //are the assignment lists valid?
        for(AssignmentListNode assign : assignments){
            if(!assign.validate(schema, scope)){
                throw new RuntimeException("Invalid assignment list in insert statement");
            }
        }
        return true;
    }
    @Override
    public String emitSQL() {
        StringBuilder sb = new StringBuilder();
        //we need to do multiple inserts sometiems to make multiple insert statemetns if neccessary (could be improved to just one in the future)
        for(AssignmentListNode list : assignments){
            sb.append("INSERT INTO ").append(tableID).append(" (");
            List<String> columns = new ArrayList<>();
            List<String> values = new ArrayList<>();
            //get the assignments for that insert statement
            for(AssignmentStatementNode assignment: list.assignments){
                columns.add(assignment.attribute.getName());
                values.add(assignment.value.getValue().toString());
            }

            //construct the statemetn based on the attributes and their values
            sb.append(String.join(", ", columns));
            sb.append(") VALUES (");
            sb.append(String.join(", ", values));
            sb.append(");\n");
        }
         return sb.toString();
    }
}
//used in assignment lists
class AssignmentStatementNode extends ASTNode{
    AttributeReference attribute;
    Value value;
    public AssignmentStatementNode(AttributeReference attribute, Value value) {
        this.attribute = attribute;
        this.value = value;
    }
    @Override
    public boolean validate(Schema schema, List<Schema.Table> tablesInScope) {
        //does this attribute exist?
        Schema.Attribute attr = ASTNode.resolveAttribute(tablesInScope, attribute.getTableName(), attribute.getName());
        if(attr == null){
            throw new RuntimeException("Attribute not found in schema: " + attribute.toString());
        }
        //what is the type of the attribute and of the value being assigned?
        String type = attr.type;
        String valueType = value.getValueType();
        //if the value is an AttributeReference we need to do soemthing a bit different
        if(value instanceof AttributeReference){
            //check if it exists
            Schema.Attribute valueAttr = resolveAttribute(tablesInScope,((AttributeReference)value).getTableName(),((AttributeReference)value).getName());
            if(valueAttr==null){
                throw new RuntimeException("Attribute not found in schema: " + value.toString());
            }
            //is it compaitible with our lhs?
            if((valueAttr.type.equals("REAL") || valueAttr.type.equals("INTEGER")) &&
            (type.equals("REAL") || type.equals("INTEGER"))){ 
                return true;
            }else if(valueAttr.type.equals(type)){
                return true;
            }else{
                throw new RuntimeException("Cannot compare attributes of different types: " + value.toString() + ", " + attr.toString());
            }
        }
        //if it is jsut a normal value... are the types compatible?
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
//used in inserting and updating, constructed of a list of assignment statements
class AssignmentListNode extends ASTNode{
    List<AssignmentStatementNode> assignments;
    public AssignmentListNode(List<AssignmentStatementNode> assignments) {
        this.assignments = assignments;
    }
    @Override
    public boolean validate(Schema schema, List<Schema.Table> tablesInScope) {
        //are all the assignments valid?
        for(AssignmentStatementNode assignment: assignments){
            if(!assignment.validate(schema, tablesInScope)){
                throw new RuntimeException("Invalid assignment in set clause for update statement");
            }
        }
        return true;
    }
    //create the list by just adding commas to each assignment (this could probably be done in a oneliner with String.join?)
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
//create table statement
class CreateTableNode extends ASTNode{
    List<CreateAttributeNode> attributes; //list of new attributes
    String tableID;
    public CreateTableNode(String tableID, List<CreateAttributeNode> attributes) {
        this.tableID = tableID;
        this.attributes = attributes;
    }
    @Override
    public boolean validate(Schema schema, List<Schema.Table> tablesInScope) {
        //make the new attribute names a hashset (prevents duplicate attr names of different types)
        Set<String> newAttrNames = new HashSet<>();
        //does this table already exist?
        if(schema.hasTable(tableID)){
            throw new RuntimeException("Table already exists in schema: " + tableID);
        }
        //is the attribute itself valid?
        for(CreateAttributeNode attribute: attributes){
            if(!attribute.validate(schema, tablesInScope)){
                throw new RuntimeException("Invalid attribute in create table statement");
            }
        }
        //if we encountered no issues add all of the attributes (could probably have been done in previous for loop)
        for(CreateAttributeNode attr: attributes){
            newAttrNames.add(attr.name);
        }
        //check for duplicates
        if(newAttrNames.size() != attributes.size()){
            throw new RuntimeException("Duplicate attribute names in create table: " + tableID);
        }
        //if all is good add the table to the schema
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
    //how to add table to schema
    private void createTable(){
        //create a new table first
        Schema.Table newTable = new Schema.Table(tableID);
        //then for each new attribute...
        for(CreateAttributeNode attrNode: attributes){
            //get the constraints
            ArrayList<String> constraints = new ArrayList<>();
            for(String constraint: attrNode.constraints){
                constraints.add(constraint);
            }
            //and the foreign keys
            for(String fk : attrNode.fkReferences){
                constraints.add(fk);
            }
            //and create a new attribute
            Schema.Attribute newAttr = new Schema.Attribute(attrNode.name, attrNode.type, constraints);
            //then add it to the table
            newTable.attributes.add(newAttr);
        }
        //then add the new table to the schema
        Schema.instance.tables.add(newTable);
    }
}
//how to create attributes
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
        //make sure that it is of a suported type
        if(!type.equals("INTEGER") && !type.equals("REAL") && !type.equals("TEXT")){
            throw new RuntimeException("Invalid/unsupported attribute type: " + type);
        }
        //make sure the constraint is supported as well
        for(String constraint: constraints) {
            if (!constraint.equals("NOTNULL") && !constraint.equals("PRIMARYKEY")) {
                throw new RuntimeException("Invalid/unsupported constraint: " + constraint);
            }
        }
        //make sure that the fk is valid
        for(String fk : fkReferences){
            if(fk.startsWith("references ")){
                //what is the tasble name?
                String refTableName = fk.substring("references ".length());
                //does the table exist?
                if(schema.getTable(refTableName) == null){
                    throw new RuntimeException("Referenced table not found in schema: " + refTableName);
                //does the attribute that the table reference exist?
                }else if(schema.getTable(refTableName).getAttribute(name) == null){
                    throw new RuntimeException("Referenced attribute not found in referenced table: " + name);
                //and are their types the same?
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
    //helper to convert the constraints to proper SQLite
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
    String function;
    public AttributeReference(String tableName, String value, String function) {
        this.tableName = tableName;
        this.value = value;
        this.function =function;
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
        //create the attribte reference based on if it has a function or not
        if(function != null){
            return SelectNode.getFunction(function) + "(" + 
                (tableName != null ? tableName + "." : "") + value + ")";
        }
        return (tableName != null ? tableName + "." : "") + value;
    }
    //attributes are equal if their table name and name are the same (may change in future to also include tyeps)
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AttributeReference other)) return false;

        return Objects.equals(this.tableName, other.getTableName()) &&
            Objects.equals(this.value, other.getName());
    }
    @Override public int hashCode(){return Objects.hash(tableName,value);}
}

// all references are like employees all and represent the *, special case of attribute reference
class AllReference extends AttributeReference{

    public AllReference(String tableName, String value, String function) {
        super(tableName, "ALL", function);
    }
    
}
//this handles the commands of sqlite
class CommandNode extends ASTNode{
    String command;
    public CommandNode(String command){
        this.command=command;
    }
    @Override
    public boolean validate(Schema schema, List<Schema.Table> tablesInScope) {
        // commands have nothing to do with the schema
        return true;
    }

    @Override
    public String emitSQL() {
        if(command.equals("tables")){
            return ".tables;";//so technically these cant have semi colons but it is split by semicolon in the python script, needs imporvement in future
        }else if (command.equals("fullschema")){
            return ".fullschema;";
        }else{
            throw new RuntimeException("Invalid command");
        }
    }

}
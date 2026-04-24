import java.util.List;
import java.util.ArrayList;
public abstract class ASTNode {
    public abstract boolean validate(Schema schema, Schema.Table tableName); // may make this method take a table name as an argument

    public abstract String emitSQL();
}

class AttributeComparisonNode extends ASTNode {
    String lhs;
    String op;
    Value rhs;
    public AttributeComparisonNode(String lhs, String op, Value rhs) {
        this.lhs = lhs;
        this.op = op;
        this.rhs = rhs;
    }
    @Override
    public boolean validate(Schema schema, Schema.Table tableName) {
        if(tableName.hasAttribute(lhs)){
            
        }
        return true;
    }
    @Override public String emitSQL() {
        // emit the SQL representation of the attribute comparison
        return null;
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
    public boolean validate(Schema schema, Schema.Table tableName) {
        // validate the left and right comparisons
        return true;
    }
    @Override public String emitSQL() {
        // emit the SQL representation of the conjoined comparison
        return null;
    }
}

class SelectNode extends ASTNode{
    int limit = -1; // -1 means no limit
    String mainTableName;
    List<String> selectedAttributes = new ArrayList<>(); // empty list means select *
    JoinNode join; // null if no join
    ConjoinedComparisonNode whereClause; // null if no where clause
    GroupNode groupBy; // null if no group by clause
    OrderNode orderBy; // null if no order by clause
    public SelectNode(String mainTableName, List<String> selectedAttributes, int limit, JoinNode join, ConjoinedComparisonNode whereClause, GroupNode groupBy, OrderNode orderBy) {
        this.mainTableName = mainTableName;
        this.selectedAttributes = selectedAttributes;
        this.limit = limit;
        this.join = join;
        this.whereClause = whereClause;
        this.groupBy = groupBy;
        this.orderBy = orderBy;
    }
    @Override
    public boolean validate(Schema schema, Schema.Table tableName) {
        // validate the select statement
        return true;
    }
    @Override public String emitSQL() {
        // emit the SQL representation of the select statement
        return null;
    }
}

class JoinNode extends ASTNode{
    String table;
    String onCondition;
    List<String> selectedAttributes; // empty list means select *
    public JoinNode(String table, String onCondition, List<String> selectedAttributes) {
        this.table = table;
        this.onCondition = onCondition;
        this.selectedAttributes = selectedAttributes;
    }
    public JoinNode(String table, String onCondition) {
        this.table = table;
        this.onCondition = onCondition;
        this.selectedAttributes = new ArrayList<>();
    }
    @Override
    public boolean validate(Schema schema, Schema.Table tableName) {
        // validate the join statement
        return true;
    }
    @Override public String emitSQL() {
        // emit the SQL representation of the join statement
        return null;
    }
}

class OrderNode extends ASTNode{
    List<String> attributes; // attributes to order by
    String order; // "ASC" or "DESC"
    public OrderNode(List<String> attributes, String order) {
        this.attributes = attributes;
        this.order = order;
    }
    @Override
    public boolean validate(Schema schema, Schema.Table tableName) {
        // validate the order by statement
        return true;
    }
    @Override public String emitSQL() {
        // emit the SQL representation of the order by statement
        return null;
    }
}

class GroupNode extends ASTNode{
    List<String> attributes; // attributes to group by
    HavingNode having; // null if no having clause
    public GroupNode(List<String> attributes, HavingNode having) {
        this.attributes = attributes;
        this.having = having;
    }
    @Override
    public boolean validate(Schema schema, Schema.Table tableName) {
        // validate the group by statement
        return true;
    }
    @Override public String emitSQL() {
        // emit the SQL representation of the group by statement
        return null;
    }
}

class HavingNode extends ASTNode{
    ConjoinedComparisonNode condition; // condition for the having clause
    public HavingNode(ConjoinedComparisonNode condition) {
        this.condition = condition;
    }
    @Override
    public boolean validate(Schema schema, Schema.Table tableName) {
        // validate the having clause
        return true;
    }
    @Override public String emitSQL() {
        // emit the SQL representation of the having clause
        return null;
    }
}

// need to be able to distinguish what a value is
abstract class Value{
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
}

class DoubleLiteral extends Value{
    Double value;
    public DoubleLiteral(Double value) {
        this.value = value;
    }
    @Override
    public String getValueType() {
        return "DOUBLE";
    }
}

class StringLiteral extends Value{
    String value;
    public StringLiteral(String value) {
        this.value = value;
    }
    @Override
    public String getValueType() {
        return "STRING";
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
}

class AttributeReference extends Value{
    String value;
    public AttributeReference(String value) {
        this.value = value;
    }
    @Override
    public String getValueType() {
        return "ATTRIBUTE";
    }
}
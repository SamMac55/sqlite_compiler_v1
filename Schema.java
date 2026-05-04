import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
// a schema is a lsit of tables
public class Schema {
    List<Table> tables = new ArrayList<>();
    public static Schema instance;//we have an instance that can be used in out program
    public Schema() {
        instance = this;
    }
    //does talbe exist?
    public boolean hasTable(String name) {
        for (Table t : tables) {
            if (t.table_name.equals(name))
                return true;
        }
        return false;
    }
    //what is the table object?
    public Table getTable(String name) {
        for (Table t : tables) {
            if (t.table_name.equals(name))
                return t;
        }
        return null;
    }
    //get the current instance
    public Schema getSchema() {
        return instance;
    }
    //a table is a name with a lsit of attributes
    public static class Table{
        String table_name;
        List<Attribute> attributes = new ArrayList<>();

        public Table(String name) {
            this.table_name = name;
        }
        //do we have this attribute?
        public boolean hasAttribute(String name) {
            for (Attribute a : attributes) {
                if (a.attr_name.equals(name))
                    return true;
            }
            return false;
        }
        //what is this attribue?
        public Attribute getAttribute(String name) {
            for (Attribute a : attributes) {
                if (a.attr_name.equals(name))
                    return a;
            }
            return null;
        }
        //does this table reference another table?
        public boolean references(Table other) {
            for (Attribute a : attributes) {
                if (a.hasConstraint("references " + other.table_name)) //need to check type eventually
                    return true;
            }
            return false;
        }
        public String getTableName(){return table_name;}
        //a table is equal to another table if they have the same name (not super secure)
        @Override public boolean equals(Object obj){
            if(this == obj) return true;
            if(this.table_name.equals(((Table) obj).getTableName())){return true;}
            return false;
        }
        @Override public int hashCode(){return Objects.hash(table_name);}
    }
    //an attribute has a name, type, and lsit of constraints
    public static class Attribute{
        String attr_name;
        String type;
        List<String> constraints;
        public Attribute(String name, String type, ArrayList<String> constraints) {
            this.attr_name = name;
            this.type = type;
            this.constraints = constraints;
        }
        //do we have this constraint?
        public boolean hasConstraint(String constraint) {
            return constraints.contains(constraint);
        }
    }
}

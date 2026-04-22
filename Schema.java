import java.util.ArrayList;
import java.util.List;
public class Schema {
    List<Table> tables = new ArrayList<>();
    public static Schema instance;
    public Schema() {
        instance = this;
    }
    public boolean hasTable(String name) {
        for (Table t : tables) {
            if (t.table_name.equals(name))
                return true;
        }
        return false;
    }
    public Schema getSchema() {
        return instance;
    }

    public static class Table{
        String table_name;
        List<Attribute> attributes = new ArrayList<>();

        public Table(String name) {
            this.table_name = name;
        }
        public boolean hasAttribute(String name) {
            for (Attribute a : attributes) {
                if (a.attr_name.equals(name))
                    return true;
            }
            return false;
        }
        public Attribute getAttribute(String name) {
            for (Attribute a : attributes) {
                if (a.attr_name.equals(name))
                    return a;
            }
            return null;
        }
        public boolean references(Table other) {
            for (Attribute a : attributes) {
                if (a.hasConstraint("references " + other.table_name)) //need to check type eventually
                    return true;
            }
            return false;
        }
    }
    public static class Attribute{
        String attr_name;
        String type;
        List<String> constraints;
        public Attribute(String name, String type, ArrayList<String> constraints) {
            this.attr_name = name;
            this.type = type;
            this.constraints = constraints;
        }
        public boolean hasConstraint(String constraint) {
            return constraints.contains(constraint);
        }
    }
}

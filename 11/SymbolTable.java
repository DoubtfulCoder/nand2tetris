import java.util.Hashtable;

/**
 * Implements symbol table for jack compiler
 * Uses hash table to store identifiers
 */ 
class SymbolTable {
    Hashtable<String, String[]> classTable, subroutineTable;
    int staticCounter, fieldCounter;
    int varCounter, argCounter;

    public SymbolTable(int size) {
        classTable = new Hashtable<String, String[]>(size);
        subroutineTable = new Hashtable<String, String[]>(size);
        staticCounter = 0;
        fieldCounter = 0;
        varCounter = 0;
        argCounter = 0;
    }

    public static void main(String[] args) {}

    public void startSubroutine() {
        // Clears symbol table
        subroutineTable.clear();
        varCounter = 0;
        argCounter = 0;
    }

    public void define(String name, String type, String kind) {
        String[] values = {kind, type, ""};
        switch(kind) {
            case "static":
                values[2] = Integer.toString(staticCounter);
                classTable.put(name, values);
                staticCounter++;
                break;
            case "field":
                values[2] = Integer.toString(fieldCounter);
                classTable.put(name, values);
                fieldCounter++;
                break;
            case "var":
                values[2] = Integer.toString(varCounter);
                subroutineTable.put(name, values);
                varCounter++;
                break;
            case "arg":
                values[2] = Integer.toString(argCounter);
                subroutineTable.put(name, values);
                argCounter++;
                break;
        }
    }

    public int varCount(String kind) {
        switch(kind) {
            case("var"):
                return varCounter;
            case("arg"):
                return argCounter;
            case("static"):
                return staticCounter;
            case("field"):
                return fieldCounter;
            default:
                return 9999999;
        }
    }

    public String kindOf(String name) {
        // Returns kind of variable (STATIC, FIELD, ARG, or VAR)
        // Returns NONE if identifier not found
        String[] search = search(name);
        String searchResult = (search == null) ? "NONE" : search[0];
        return searchResult;
    }

    public String typeOf(String name) {
        // Returns type of variable (int, char, boolean, or class)
        return search(name)[1];
    }

    public String indexOf(String name) {
        // Returns index of variable in segment
        return search(name)[2];
    }

    public String[] search(String name) {
        // Searches in class table and subroutine table 
        // for variable and returns value (can be null)
        if (subroutineTable.containsKey(name)) {
            return subroutineTable.get(name);
        } else {
            return classTable.get(name); // returns null if not found
        }
    }
}
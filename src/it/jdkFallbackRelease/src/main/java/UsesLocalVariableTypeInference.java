/**
 * Uses the 'var' keyword, which requires a Java 10+ source level in Clover's parser.
 */
public class UsesLocalVariableTypeInference {

    public String concatenate(String first, String second) {
        var result = first + second;
        return result;
    }

    public static void main(String[] args) {
        System.out.println(new UsesLocalVariableTypeInference().concatenate("Hello ", "world"));
    }
}

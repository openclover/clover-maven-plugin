/**
 * A Java 8 compatible class.
 */
public class Simple {

    public String concatenate(String first, String second) {
        return first + second;
    }

    public static void main(String[] args) {
        System.out.println(new Simple().concatenate("Hello ", "world"));
    }
}

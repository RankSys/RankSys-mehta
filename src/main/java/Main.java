
import static java.lang.Class.forName;
import static java.util.Arrays.copyOfRange;

public class Main {

    public static void main(String[] args) throws Exception {
        String clss;
        switch (args[0]) {
            case "recommendations":
                clss = "Recommendations";
                break;
            case "metrics":
                clss = "Metrics";
                break;
            default:
                clss = null;
                System.err.println(String.format("Command %s unknown.", args[0]));
                System.exit(1);
        }

        String main = "org.ranksys.mehta." + clss;
        args = copyOfRange(args, 1, args.length);

        Class[] argTypes = {args.getClass(),};
        Object[] passedArgs = {args};
        forName(main).getMethod("main", argTypes).invoke(null, passedArgs);
    }
}
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
@Command(name = "client1.Arguments", mixinStandardHelpOptions = true, version = "1.0",
        description = "Client arguments parser")
public class ArgCommand {
    @Option(names = "-nt", required = true, description = "The maximum threads to run, default is 32")
    public static int numOfThreads = 32;

    @Option(names = "-nr", required = true, description = "The total number of lift rides, 200K as default.")
    public static int numOfRequests = 200000;
    @Option(names = {"-h", "--host"}, required = true, description = "Server's IP address")
    public static String serverHostName;

    @Option(names = {"-l", "--latency"}, description = "the mode of testing latency")
    public static boolean isTestOnly = false;

    @Option(names = {"-d", "--dayid"}, description = "Specify dayID")
    public static int dayID = 1;

    public static String protocol = "http://";
    public static String serverURL;

    public void parse(String[] args) {
        CommandLine cmd = new CommandLine(this);
        cmd.setCaseInsensitiveEnumValuesAllowed(true);
        cmd.parseArgs(args);
        serverURL = protocol + serverHostName;
    }
}
///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.6.3
//DEPS org.kohsuke:github-api:1.313
//DEPS commons-io:commons-io:2.11.0
//JAVA 17

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

@Command(name = "assessWorkflows", mixinStandardHelpOptions = true, version = "assessWorkflows 0.1",
        description = "Lists untrusted github actions used in github workflows throughout an organization")
class assessWorkflows implements Callable<Integer> {

    @Parameters(index = "0", description = "The organization to analyze", defaultValue = "redhat-developer")
    private String organization;

    @Option(names = { "-t", "--trusted" },  description="Comma-separated list of trusted action publishers",  split = ",", defaultValue = "actions,docker" )
    private List<String> trustedPublishers = new ArrayList<>();

    @Option(names = { "-r", "--repos" },  description="Comma-separated list of repositories from the selected organization to analyze",  split = "," )
    private List<String> repos = new ArrayList<>();


    public static void main(String... args) {
        int exitCode = new CommandLine(new assessWorkflows()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        System.out.println("Fetching "+organization+ " repositories"); 
        var github = GitHub.connect();
        var org = github.getOrganization(organization);
        org.getRepositories().forEach(this::analyze);
        return 0;
    }

    private void analyze(String name, GHRepository repo) {
        try {
            if (!repos.isEmpty() && !repos.contains(name)) {
                return;
            }
            if (repo.isArchived()) {
                System.out.println("✋ ignoring archived "+repo.getHtmlUrl());
                return;
            }
            System.out.println("🔍 analyzing "+repo.getHtmlUrl());
            var workflowsDir = repo.getDirectoryContent(".github/workflows/");
            workflowsDir.forEach(this::analyzeWorkflow );
        } catch (GHFileNotFoundException missing) {
            // System.err.println(repo.getHtmlUrl() + " has no workflows?!"); 
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void analyzeWorkflow(GHContent workflow) {
        if (!workflow.isFile()) {
            System.out.println(workflow.getName() + " is a directory");
            //TODO see if we need to look inside this dir/
            return;
        }
        try {
            System.out.println(" 👀 "+workflow.getHtmlUrl());
            var content = IOUtils.toString(workflow.read(), "UTF-8");
            content.lines().filter(l -> !l.startsWith("#") && l.contains("uses:"))
                            .map(this::toAction)
                            .filter(Objects::nonNull)
                            .filter(this::isNotTrusted)
                            .forEach(a -> System.out.println("\t"+a));
            //System.out.println(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    Pattern actionPattern = Pattern.compile("uses:\\s*(\\S+)\\/(\\S+)\\@(\\S+)");

    public Action toAction(String line) {
        var matcher = actionPattern.matcher(line);
        //System.out.print(line );
        if (matcher.find()) {
            //System.out.println( " is an action" );
            return new Action(matcher.group(1), matcher.group(2), matcher.group(3));
        }
        //System.out.println( " is not an action" );
        return null;
    }

    public boolean isNotTrusted(Action action) {
        return !trustedPublishers.contains(action.publisher());
    }

    static record Action(String publisher, String name, String version) {
        public String toString() {
            return publisher+"/"+name+"@"+version;
        }
    }
}

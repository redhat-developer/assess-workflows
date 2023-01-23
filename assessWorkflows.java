///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.6.3
//DEPS org.kohsuke:github-api:1.313
//DEPS commons-io:commons-io:2.11.0
//JAVA 17

import java.io.IOException;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "assessWorkflows", mixinStandardHelpOptions = true, version = "assessWorkflows 0.1",
        description = "assessWorkflows made with jbang")
class assessWorkflows implements Callable<Integer> {

    @Parameters(index = "0", description = "The organization to analyze", defaultValue = "redhat-developer")
    private String organization;

    public static void main(String... args) {
        int exitCode = new CommandLine(new assessWorkflows()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        System.out.println("Fetching "+organization); 
        var github = GitHub.connect();
        var org = github.getOrganization(organization);
        org.getRepositories().forEach(this::analyze);
        return 0;
    }

    private void analyze(String name, GHRepository repo) {
        try {
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
            content.lines().filter(l -> !l.startsWith("#") && l.contains("uses:")).forEach(System.out::println);
            //System.out.println(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.6.3
//DEPS org.kohsuke:github-api:1.313
//DEPS commons-io:commons-io:2.11.0
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.4
//DEPS com.fasterxml.jackson.core:jackson-core:2.12.4
//DEPS com.fasterxml.jackson.core:jackson-databind:2.12.4
//JAVA 17

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
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
    private static String organization;

    @Option(names = { "-t", "--trusted" },  description="Comma-separated list of trusted action publishers",  split = ",", defaultValue = "actions,docker" )
    private static List<String> trustedPublishers = new ArrayList<>();

    @Option(names = { "-r", "--repos" },  description="Comma-separated list of repositories from the selected organization to analyze",  split = "," )
    private List<String> repos = new ArrayList<>();

    assessWorkflows() throws Exception {
    }
    
    public static void main(String... args) throws Exception {
        int exitCode = new CommandLine(new assessWorkflows()).execute(args);
        System.exit(exitCode);
    }

    private GitHub github = GitHub.connect();
    
    @Override
    public Integer call() throws Exception {
        System.out.println("Fetching "+ organization + " repositories"); 
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

    private static final Pattern actionPattern = Pattern.compile("(\\S[^\\/]+)\\/(.+)@(\\S+)");

    public static Action toAction(String uses) {
        if (uses == null) {
            return null;
        }
        var matcher = actionPattern.matcher(uses);
        if (matcher.find()) {
            return new Action(matcher.group(1), matcher.group(2), matcher.group(3));
        } else if (uses.startsWith(".")){
            return new Action(null, uses, null);
        } else {
            System.err.println(uses + " is not an action");
        }
        return null;
    }

    record Workflow(String name, Map<String, Job> jobs) {};
    record Job(String name, List<Step> steps) {};
    record Step(String name, String uses) {
        Action action() {
            return toAction(uses);
        }
    };

    Map<String, String> actionsCache = new HashMap<>();

    private void analyzeWorkflow(GHContent workflowFile) {
        if (!workflowFile.isFile()) {
            System.out.println(workflowFile.getName() + " is a directory");
            //TODO see if we need to look inside this dir/
            return;
        }
        try {
            System.out.println(" 👀 "+workflowFile.getHtmlUrl());
            Workflow wf = getWorkflow(workflowFile);
            if (wf == null || wf.jobs() == null) {
                return;
            }
            wf.jobs().forEach((k, v) -> {
                if (v.steps() != null) {
                    v.steps().forEach(s -> {
                        var action = s.action();
                        if (action != null && !action.isTrusted()) {
                            var sha = getActionSha1(k, action);
                            if (sha != null && !sha.equals(action.version())) {
                                System.out.println("Job " + k + " is using action " + s.uses() + " should be: " + s.uses().replace(action.version(), sha));
                            }
                        }
                    });
                };
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Workflow getWorkflow(GHContent workflow) throws IOException {
        var mapper = new ObjectMapper(new YAMLFactory()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            return mapper.readValue(IOUtils.toString(workflow.read(), "UTF-8"), Workflow.class);
        } catch (MismatchedInputException ex) {
            // non parsable yaml / skipping
        }
        return null;
    }

    private String getActionSha1(String job, Action action) {
        if (action == null || action.org() == null) {
            return null;
        }
        String actionKey = action.toString();
        actionsCache.computeIfAbsent(actionKey, ignored -> {
            try {
                var repo = github.getRepository(action.repo());
                var tree = repo.getTree(action.version());
                return tree.getSha();
            } catch (Exception e) {
                System.out.println("Job " + job + " is using action " + action + " but we cannot identify the commit SHA");
                return null;
            }
        });
        return actionsCache.get(actionKey);
    }

    static record Action(String org, String name, String version) {

        public String repo() {
            if (org == null) {
                return null;
            }
            var slash = name.indexOf("/");
            return org + "/" + ((slash < 0)? name: name.substring(0, slash));
        }

        public String toString() {
            if (org == null) {
                return name;
            }
            return org+"/"+name+ ((version == null)?"":"@"+version);
        }

        boolean isTrusted() {
            return  org == null || organization.equals(org) || trustedPublishers.contains(org);
        }
    }
}

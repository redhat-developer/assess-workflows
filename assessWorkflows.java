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
import java.util.List;
import java.util.Objects;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private String organization;

    @Option(names = { "-t", "--trusted" },  description="Comma-separated list of trusted action publishers",  split = ",", defaultValue = "actions,docker" )
    private static List<String> trustedPublishers = new ArrayList<>();
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

    record Workflow(String name, Map<String, Job> jobs) {};
    record Job(String name, List<Step> steps) {};
    record Step(String name, String uses) {

        boolean isReusable() {
            var first = uses.indexOf('/');
            var last = uses.lastIndexOf('/');
            return (uses != null && (uses.endsWith(".yaml") || uses.endsWith(".yml") || first != last));
        }
        boolean isTrusted() {
            return (uses == null || isReusable() || trustedPublishers.stream().anyMatch(tp -> uses.startsWith(tp + "/")));
        }

        String getOrg() {
            if (uses == null) {
                return null;
            } else {
                int i = uses.indexOf('/');
                return uses.substring(0, i);
            }
        }

        String getRepo() {
            if (uses == null) {
                return null;
            } else {
                int i = uses.indexOf('/');
                int v = uses.indexOf('@');
                return uses.substring(i + 1, v);
            }
        }

        String getVersion() {
            if (uses == null) {
                return null;
            } else {
                int v = uses.indexOf('@');
                return uses.substring(v + 1);
            }
        }
    };

    private void analyzeWorkflow(GHContent workflow) {
        if (!workflow.isFile()) {
            System.out.println(workflow.getName() + " is a directory");
            //TODO see if we need to look inside this dir/
            return;
        }
        try {
            System.out.println(" 👀 "+workflow.getHtmlUrl());
            var mapper = new ObjectMapper(new YAMLFactory()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            var wf = mapper.readValue(IOUtils.toString(workflow.read(), "UTF-8"), Workflow.class);

            wf.jobs().forEach((k, v) -> {
                if (v.steps() != null) {
                    v.steps().forEach(s -> {
                        if (!s.isTrusted()) {
                            try {
                                var repo = github.getRepository(s.getOrg() + "/" + s.getRepo());
                                var sha = "";
                                try {
                                    var tree = repo.getTree(s.getVersion());
                                    sha = tree.getSha();
                                } catch (Exception e) {
                                    throw new RuntimeException("Cannot find action source version", e);
                                }

                                System.out.println("Job " + k + " is using action " + s.uses() + " should be: " + s.uses().replace(s.getVersion(), sha));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                };
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static record Action(String publisher, String name, String version) {
        public String toString() {
            return publisher+"/"+name+"@"+version;
        }
    }
}

///usr/bin/env jbang "$0" "$@" ; exit $?
/*
 Copyright (c) 2023 Red Hat, Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of
 this software and associated documentation files (the "Software"), to deal in
 the Software without restriction, including without limitation the rights to
 use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 the Software, and to permit persons to whom the Software is furnished to do so,
 subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

//DEPS info.picocli:picocli:4.6.3
//DEPS org.kohsuke:github-api:1.313
//DEPS commons-io:commons-io:2.11.0
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.4
//DEPS com.fasterxml.jackson.core:jackson-core:2.12.4
//DEPS com.fasterxml.jackson.core:jackson-databind:2.12.4
//DEPS com.google.guava:guava:31.1-jre
//DEPS org.gitlab4j:gitlab4j-api:5.0.1
//JAVA 17+

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPerson;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "assessWorkflows", mixinStandardHelpOptions = true, version = "assessWorkflows 0.1", description = "Lists untrusted github actions used in github workflows throughout an organization")
class assessWorkflows implements Callable<Integer> {

    @Parameters(index = "0", description = "The organization/user to analyze", defaultValue = "redhat-developer")
    private static String orgOrUser;

    @Option(names = { "-t",
            "--trusted" }, description = "Comma-separated list of trusted action publishers", split = ",", defaultValue = "actions,docker,redhat-actions")
    private static List<String> trustedPublishers = new ArrayList<>();

    @Option(names = { "-r",
            "--repos" }, description = "Comma-separated list of repositories from the selected organization to analyze. Support wildcard suffixes, e.g. repo*", split = ",")
    private List<String> repos = new ArrayList<>();

    @Option(names = { "-pr", "--pull-requests" }, description = "Generate Pull-Requests to pin the Actions SHA1")
    private boolean generatePR = false;

    
    assessWorkflows() throws Exception {
    }

    public static void main(String... args) throws Exception {
        int exitCode = new CommandLine(new assessWorkflows()).execute(args);
        System.exit(exitCode);
    }

    private GitHub github = GitHub.connect();

    @Override
    public Integer call() throws Exception {
        System.out.println("Fetching " + orgOrUser + " repositories");

        GHPerson owner = null;
        try {
            owner = github.getOrganization(orgOrUser);
        } catch (GHFileNotFoundException e) {
            owner = github.getUser(orgOrUser);
        }
        owner.getRepositories().forEach(this::analyze);
        return 0;
    }

    private void analyze(String name, GHRepository repo) {
        try {
            if (!isRepoIncluded(name)) {
                return;
            }
            if (repo.isArchived()) {
                System.out.println("✋ ignoring archived " + repo.getHtmlUrl());
                System.out.println();
                return;
            }
            System.out.println("🔍 analyzing " + repo.getHtmlUrl());
            var workflowsDir = repo.getDirectoryContent(".github/workflows/");
            PRContent prContent = new PRContent(repo);
            workflowsDir.forEach(dir -> analyzeWorkflow(prContent, dir));
            if (generatePR && prContent.hasChanges()) {
                openPR(prContent);
            }
        } catch (GHFileNotFoundException missing) {
            // System.err.println(repo.getHtmlUrl() + " has no workflows?!");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println();
    }

    private boolean isRepoIncluded(String name) {
        if (repos.isEmpty()) {
            return true;
        }
        for (var repo : repos) {
            if (repo.endsWith("*")) {
                if (name.startsWith(repo.replace("*", ""))) {
                    return true;
                }
            }  else if (name.equalsIgnoreCase(repo)){
                return true;
            }
        }
        return false;
    }

    private static final Pattern actionPattern = Pattern.compile("(\\S[^\\/]+)\\/(.+)@(\\S+)");

    public static Action toAction(String uses) {
        if (uses == null) {
            return null;
        }
        var matcher = actionPattern.matcher(uses);
        if (matcher.find()) {
            return new Action(matcher.group(1), matcher.group(2), matcher.group(3));
        } else if (uses.startsWith(".")) {
            return new Action(null, uses, null);
        } else {
            System.err.println(uses + " is not an action");
        }
        return null;
    }

    record Workflow(String name, Map<String, Job> jobs) {
    };

    record Job(String name, List<Step> steps) {
    };

    record Step(String name, String uses) {
        Action action() {
            return toAction(uses);
        }
    };

    Map<String, String> actionsCache = new HashMap<>();

    private void analyzeWorkflow(assessWorkflows.PRContent prContent, GHContent workflowFile) {
        if (!workflowFile.isFile()) {
            System.out.println(workflowFile.getName() + " is a directory");
            // TODO see if we need to look inside this dir/
            return;
        }
        try {
            System.out.println(" 👀 " + workflowFile.getHtmlUrl());
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
                                prContent.recordChange(workflowFile, action, sha);
                                System.out.println("\tJob " + k + " is using action " + s.uses() + " should be: "
                                        + s.uses().replace(action.version(), sha));
                            }
                        }
                    });
                }
                ;
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Workflow getWorkflow(GHContent workflow) throws IOException {
        var mapper = new ObjectMapper(new YAMLFactory()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false);
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
                System.out.println(
                        "Job " + job + " is using action " + action + " but we cannot identify the commit SHA");
                return null;
            }
        });
        return actionsCache.get(actionKey);
    }

    static class PRContent {

        private record ActionChange(Action action, String version) {
        };

        private Multimap<GHContent, ActionChange> changes = ArrayListMultimap.create();;
        public GHRepository repo;

        PRContent(GHRepository repo) {
            this.repo = repo;
        }

        boolean hasChanges() {
            return !changes.isEmpty();
        }

        void recordChange(GHContent file, Action action, String version) {
            changes.put(file, new ActionChange(action, version));
        }

    }

    static record Action(String org, String name, String version) {
        public String repo() {
            if (org == null) {
                return null;
            }
            var slash = name.indexOf("/");
            return org + "/" + ((slash < 0) ? name : name.substring(0, slash));
        }

        public String toString() {
            if (org == null) {
                return name;
            }
            return org + "/" + name + ((version == null) ? "" : "@" + version);
        }

        boolean isTrusted() {
            return org == null // Action local to this repo is trusted
                    || orgOrUser.equals(org) // Current owner is trusted
                    || trustedPublishers.contains(org); // Trusted org
        }
    }

    private static final String PR_BODY = """
            Hi!

            Following the [GH Action Security Hardening](https://docs.github.com/en/actions/security-guides/security-hardening-for-github-actions#using-third-party-actions) guide we should use the commit SHA instead of the `branch` or `tag` for any third-party untrusted action.

            This PR was submitted by a script.
            """;

    private static final String COMMIT_MSG = "Pin 3rd-party actions to SHA1";

    public void openPR(PRContent prContent) {
        // Get the repository
        if (!prContent.hasChanges()) {
            return;
        }

        var repo = prContent.repo;
        try {

            // Check there's no PR already
            var prs = repo.getPullRequests(GHIssueState.OPEN);
            var existingPR = prs.stream().filter(pr -> COMMIT_MSG.equalsIgnoreCase(pr.getTitle())).findAny();
            if (existingPR.isPresent()) {
                System.out.println("PR already opened: "+ existingPR.get().getHtmlUrl());
                return;
            }

            var fork = github.getMyself().getRepository(repo.getName());
            if (fork == null) {
                System.out.println("Creating fork of "+ repo.getName());
                fork = repo.fork();
            }

            var headSha = repo.getRef("heads/" + repo.getDefaultBranch()).getObject().getSha();
            var branchName = "pin-actions-sha1";
            var branchRef = "refs/heads/" + branchName;

            // Create branch
            try {
                fork.getRef(branchRef);
                System.out.println(branchRef + " exists");
                //TODO we probably need to update the branch or it might be stale
            } catch (Exception e) {
                System.out.println("Creating branch " + branchRef);
                fork.createRef(branchRef, headSha);
            }

            //Unfortunately (?) GitHub API doesn't provide a way to create batch changes in a single commit
            //So we end up with 1 commit / file changed
            for (var change : prContent.changes.asMap().entrySet()) {
                var file = change.getKey();
                var updates = change.getValue();
                String filePath = file.getPath();
                // Check if the file exists
                GHContent content = fork.getFileContent(filePath, branchName);
                String fileContent = IOUtils.toString(content.read(), "UTF-8");
                String newContent = update(fileContent, updates);
                if (fileContent.equals(newContent)) {
                    System.out.println("Content hasn't changed, continue ...");
                    continue;
                }
                var response = fork.createContent().message(COMMIT_MSG + " in " + filePath)
                        .path(filePath)
                        .content(newContent)
                        .branch(branchName)
                        .sha(content.getSha())
                        .commit();
                var commit = response.getCommit();
                System.out.println("Created commit " + commit.getHtmlUrl());
            }
            var pr = repo.createPullRequest(COMMIT_MSG, github.getMyself().getLogin() + ":" + branchName,
                    repo.getDefaultBranch(), PR_BODY);
            System.out.println("Opened PR " + pr.getHtmlUrl());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private String update(String fileContent, Collection<assessWorkflows.PRContent.ActionChange> updates) {
        String newContent = fileContent;
        for (var chg : updates) {
            var a = chg.action;
            newContent = newContent.replace(a.toString(), a.org + "/" + a.name + "@" + chg.version + " #" + a.version);
        }
        return newContent;
    }
}

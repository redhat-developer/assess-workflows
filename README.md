# Assess Workflows

Lists all usages of github actions in github workflows, in an organization. Requires GitHub credentials to be stored in "~/.github" or in System Environment Properties.

- Install [JBang](https://www.jbang.dev/download/)
- store your Github credentials in `~/.github`, see [GitHub API doc](https://github.com/hub4j/github-api/blob/main/src/site/apt/index.apt#L59-L84) for more info. **Your token should have the `workflow` scope.**


- In a terminal, run: 

> jbang assessWorkflows.java my-github-org > report.txt

JBang will automatically download a compatible JDK if needed.

- To generate Pull Requests for some repositories, run: 

> jbang assessWorkflows.java my-github-org -pr -r repo1,repo2

- Wildcard suffixes are supported too, as in: 

> jbang assessWorkflows.java my-github-org -pr -r "repo*"

For more info, use the help:

```
jbang ./assessWorkflows.java -h
Usage: assessWorkflows [-hV] [-pr] [-r=<repos>[,<repos>...]]...
                       [-t=<trustedPublishers>[,<trustedPublishers>...]]...
                       <orgOrUser>
Lists untrusted github actions used in github workflows throughout an
organization
      <orgOrUser>            The organization/user to analyze
  -h, --help                 Show this help message and exit.
      -pr, --pull-requests   Generate Pull-Requests to pin the Actions SHA1
  -r, --repos=<repos>[,<repos>...]
                             Comma-separated list of repositories from the
                               selected organization to analyze. Support
                               wildcard suffixes, e.g. repo*
  -t, --trusted=<trustedPublishers>[,<trustedPublishers>...]
                             Comma-separated list of trusted action publishers
  -V, --version              Print version information and exit.

```

Example output ([`redhat-developer`](https://github.com/redhat-developer) is the default organization):

```
 jbang ./assessWorkflows.java -r "vscode-*" -pr              
Fetching redhat-developer repositories
🔍 analyzing https://github.com/redhat-developer/vscode-commons
 👀 https://github.com/redhat-developer/vscode-commons/blob/master/.github/workflows/ci.yaml
        Job build is using action GabrielBB/xvfb-action@v1.0 should be: GabrielBB/xvfb-action@fe2609f8182a9ed5aee7d53ff3ed04098a904df2
Creating branch refs/heads/pin-actions-sha1
Created commit https://github.com/fbricon/vscode-commons/commit/8b4bb40d08e34d56dc7803e6cd2dc0a54511a9d2
Opened PR https://github.com/redhat-developer/vscode-commons/pull/35

✋ ignoring archived https://github.com/redhat-developer/vscode-didact

🔍 analyzing https://github.com/redhat-developer/vscode-extension-tester
 👀 https://github.com/redhat-developer/vscode-extension-tester/blob/main/.github/workflows/insiders.yml
 👀 https://github.com/redhat-developer/vscode-extension-tester/blob/main/.github/workflows/main.yml

🔍 analyzing https://github.com/redhat-developer/vscode-java
 👀 https://github.com/redhat-developer/vscode-java/blob/master/.github/workflows/conflictDetector.yaml
 👀 https://github.com/redhat-developer/vscode-java/blob/master/.github/workflows/pr-verify.yml

🔍 analyzing https://github.com/redhat-developer/vscode-knative
 👀 https://github.com/redhat-developer/vscode-knative/blob/main/.github/workflows/main.yml
        Job build is using action GabrielBB/xvfb-action@v1.0 should be: GabrielBB/xvfb-action@fe2609f8182a9ed5aee7d53ff3ed04098a904df2
        Job build is using action GabrielBB/xvfb-action@v1.6 should be: GabrielBB/xvfb-action@86d97bde4a65fe9b290c0b3fb92c2c4ed0e5302d
        Job build is using action codecov/codecov-action@v1 should be: codecov/codecov-action@29386c70ef20e286228c72b668a06fd0e8399192
Creating fork of vscode-knative
Creating branch refs/heads/pin-actions-sha1
Created commit https://github.com/fbricon/vscode-knative/commit/226eb5d1d4c0cf7fad430139ede7b2aeeb0b77a0
Opened PR https://github.com/redhat-developer/vscode-knative/pull/326

🔍 analyzing https://github.com/redhat-developer/vscode-kubernetes-tools

🔍 analyzing https://github.com/redhat-developer/vscode-microprofile
 👀 https://github.com/redhat-developer/vscode-microprofile/blob/master/.github/workflows/tests.yml

🔍 analyzing https://github.com/redhat-developer/vscode-openshift-extension-pack

🔍 analyzing https://github.com/redhat-developer/vscode-openshift-tools
 👀 https://github.com/redhat-developer/vscode-openshift-tools/blob/main/.github/workflows/continuous-integration-workflow.yml
        Job build is using action GabrielBB/xvfb-action@v1.0 should be: GabrielBB/xvfb-action@fe2609f8182a9ed5aee7d53ff3ed04098a904df2
        Job build is using action codecov/codecov-action@v1.0.12 should be: codecov/codecov-action@07127fde53bc3ccd346d47ab2f14c390161ad108
        Job build is using action GabrielBB/xvfb-action@v1.6 should be: GabrielBB/xvfb-action@86d97bde4a65fe9b290c0b3fb92c2c4ed0e5302d
PR already opened: https://github.com/redhat-developer/vscode-openshift-tools/pull/2750

🔍 analyzing https://github.com/redhat-developer/vscode-project-initializer
 👀 https://github.com/redhat-developer/vscode-project-initializer/blob/master/.github/workflows/CI.yml

🔍 analyzing https://github.com/redhat-developer/vscode-quarkus
 👀 https://github.com/redhat-developer/vscode-quarkus/blob/master/.github/workflows/tests.yml

🔍 analyzing https://github.com/redhat-developer/vscode-redhat-account
 👀 https://github.com/redhat-developer/vscode-redhat-account/blob/main/.github/workflows/CI.yml

🔍 analyzing https://github.com/redhat-developer/vscode-redhat-telemetry
 👀 https://github.com/redhat-developer/vscode-redhat-telemetry/blob/main/.github/workflows/ci.yaml
 👀 https://github.com/redhat-developer/vscode-redhat-telemetry/blob/main/.github/workflows/release.yaml

🔍 analyzing https://github.com/redhat-developer/vscode-rhoas
 👀 https://github.com/redhat-developer/vscode-rhoas/blob/main/.github/workflows/CI.yml

🔍 analyzing https://github.com/redhat-developer/vscode-rsp-ui
 👀 https://github.com/redhat-developer/vscode-rsp-ui/blob/main/.github/workflows/gh-actions.yml
        Job test is using action GabrielBB/xvfb-action@v1.0 should be: GabrielBB/xvfb-action@fe2609f8182a9ed5aee7d53ff3ed04098a904df2
        Job test is using action GabrielBB/xvfb-action@v1.6 should be: GabrielBB/xvfb-action@86d97bde4a65fe9b290c0b3fb92c2c4ed0e5302d
        Job test is using action codecov/codecov-action@v2 should be: codecov/codecov-action@f32b3a3741e1053eb607407145bc9619351dc93b
Creating branch refs/heads/pin-actions-sha1
Created commit https://github.com/fbricon/vscode-rsp-ui/commit/843f74b71e11207d32bcdfa358b545e09f3b8a14
Opened PR https://github.com/redhat-developer/vscode-rsp-ui/pull/261

🔍 analyzing https://github.com/redhat-developer/vscode-server-connector
 👀 https://github.com/redhat-developer/vscode-server-connector/blob/master/.github/workflows/gh-actions.yml
        Job test is using action GabrielBB/xvfb-action@v1.0 should be: GabrielBB/xvfb-action@fe2609f8182a9ed5aee7d53ff3ed04098a904df2
        Job test is using action GabrielBB/xvfb-action@v1.0 should be: GabrielBB/xvfb-action@fe2609f8182a9ed5aee7d53ff3ed04098a904df2
        Job test is using action codecov/codecov-action@v2 should be: codecov/codecov-action@f32b3a3741e1053eb607407145bc9619351dc93b
Creating fork of vscode-server-connector
java.lang.RuntimeException: java.io.IOException: GHRepository@4f25b795[nodeId=MDEwOlJlcG9zaXRvcnkxMzQyNjE3ODM=,description=📦 Connects Visual Studio Code to your server adapters and run, deploy apps !!,homepage=,name=vscode-server-connector,fork=false,archived=false,disabled=false,visibility=public,size=90280,milestones={},language=TypeScript,commits={},source=<null>,parent=<null>,isTemplate=false,compareUsePaginatedCommits=false,url=https://api.github.com/repos/redhat-developer/vscode-server-connector,id=134261783,nodeId=<null>,createdAt=2018-05-21T11:45:59Z,updatedAt=2022-11-25T11:54:39Z] was forked but can't find the new repository
        at assessWorkflows.openPR(assessWorkflows.java:335)
        at assessWorkflows.analyze(assessWorkflows.java:97)
        at java.base/java.util.TreeMap.forEach(TreeMap.java:1282)
        at java.base/java.util.Collections$UnmodifiableMap.forEach(Collections.java:1553)
        at assessWorkflows.call(assessWorkflows.java:78)
        at assessWorkflows.call(assessWorkflows.java:40)
        at picocli.CommandLine.executeUserObject(CommandLine.java:1953)
        at picocli.CommandLine.access$1300(CommandLine.java:145)
        at picocli.CommandLine$RunLast.executeUserObjectOfLastSubcommandWithSameParent(CommandLine.java:2358)
        at picocli.CommandLine$RunLast.handle(CommandLine.java:2352)
        at picocli.CommandLine$RunLast.handle(CommandLine.java:2314)
        at picocli.CommandLine$AbstractParseResultHandler.execute(CommandLine.java:2179)
        at picocli.CommandLine$RunLast.execute(CommandLine.java:2316)
        at picocli.CommandLine.execute(CommandLine.java:2078)
        at assessWorkflows.main(assessWorkflows.java:62)
Caused by: java.io.IOException: GHRepository@4f25b795[nodeId=MDEwOlJlcG9zaXRvcnkxMzQyNjE3ODM=,description=📦 Connects Visual Studio Code to your server adapters and run, deploy apps !!,homepage=,name=vscode-server-connector,fork=false,archived=false,disabled=false,visibility=public,size=90280,milestones={},language=TypeScript,commits={},source=<null>,parent=<null>,isTemplate=false,compareUsePaginatedCommits=false,url=https://api.github.com/repos/redhat-developer/vscode-server-connector,id=134261783,nodeId=<null>,createdAt=2018-05-21T11:45:59Z,updatedAt=2022-11-25T11:54:39Z] was forked but can't find the new repository
        at org.kohsuke.github.GHRepository.fork(GHRepository.java:1594)
        at assessWorkflows.openPR(assessWorkflows.java:288)
        ... 14 more

🔍 analyzing https://github.com/redhat-developer/vscode-server-connector-api

🔍 analyzing https://github.com/redhat-developer/vscode-tekton
 👀 https://github.com/redhat-developer/vscode-tekton/blob/main/.github/workflows/ci-workflow.yml
        Job build is using action GabrielBB/xvfb-action@v1.0 should be: GabrielBB/xvfb-action@fe2609f8182a9ed5aee7d53ff3ed04098a904df2
        Job build is using action codecov/codecov-action@v1.0.12 should be: codecov/codecov-action@07127fde53bc3ccd346d47ab2f14c390161ad108
 👀 https://github.com/redhat-developer/vscode-tekton/blob/main/.github/workflows/codeql-analysis.yml
        Job analyze is using action github/codeql-action/init@v1 should be: github/codeql-action/init@231aa2c8a89117b126725a0e11897209b7118144
        Job analyze is using action github/codeql-action/autobuild@v1 should be: github/codeql-action/autobuild@231aa2c8a89117b126725a0e11897209b7118144
        Job analyze is using action github/codeql-action/analyze@v1 should be: github/codeql-action/analyze@231aa2c8a89117b126725a0e11897209b7118144
PR already opened: https://github.com/redhat-developer/vscode-tekton/pull/738

🔍 analyzing https://github.com/redhat-developer/vscode-wizard
 👀 https://github.com/redhat-developer/vscode-wizard/blob/master/.github/workflows/gh-actions.yml

🔍 analyzing https://github.com/redhat-developer/vscode-xml
 👀 https://github.com/redhat-developer/vscode-xml/blob/main/.github/workflows/lint.yaml
 👀 https://github.com/redhat-developer/vscode-xml/blob/main/.github/workflows/native-image.yaml

🔍 analyzing https://github.com/redhat-developer/vscode-yaml
 👀 https://github.com/redhat-developer/vscode-yaml/blob/main/.github/workflows/CI.yaml
        Job build is using action GabrielBB/xvfb-action@v1.0 should be: GabrielBB/xvfb-action@fe2609f8182a9ed5aee7d53ff3ed04098a904df2
        Job build is using action GabrielBB/xvfb-action@v1.0 should be: GabrielBB/xvfb-action@fe2609f8182a9ed5aee7d53ff3ed04098a904df2
PR already opened: https://github.com/redhat-developer/vscode-yaml/pull/875
```

## License

MIT, See [LICENSE](LICENSE) for more information.
# Assess Workflows

Lists all usages of github actions in github workflows, in an organization. Requires GitHub credentials to be stored in "~/.github" or in System Environment Properties.

- Install [JBang](https://www.jbang.dev/download/)
- store your Github credentials in `~/.github`, see [GitHub API doc](https://github.com/hub4j/github-api/blob/main/src/site/apt/index.apt#L59-L84) for more info. **Your token should have the `workflow` scope.**


- In a terminal, run: 

> jbang assessWorkflows.java > report.txt

JBang will automatically download a compatible JDK if needed.

- To generate Pull Requests for some repositories, run: 

> jbang assessWorkflows.java -pr -r repo1,repo2


For more info, use the help:

```
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
                               selected organization to analyze.
  -t, --trusted=<trustedPublishers>[,<trustedPublishers>...]
                             Comma-separated list of trusted action publishers
  -V, --version              Print version information and exit.

```
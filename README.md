# Assess Workflows

Lists all usages of github actions in github workflows, in an organization. Requires GitHub credentials to be stored in "~/.github" or in System Environment Properties.

- Install [JBang](https://www.jbang.dev/download/)
- store your Github credentials in `~/.github`, see [GitHub API doc](https://github.com/hub4j/github-api/blob/main/src/site/apt/index.apt#L59-L84) for more info. 
- In a terminal, run: 

> jbang assessWorkflows.java > report.txt

JBang will automatically download a compatible JDK if needed.
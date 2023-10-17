## Git Commit Guidelines

Please make sure that the (squashed) git commits that are to be merged within a pull request
  - are (if applicable) [atomic](https://en.wikipedia.org/wiki/Atomic_commit#Atomic_commit_convention), i.\,e. do not mix code reformatting, code moves and code changes or commit multiple features at once.
  - are clean and complete, i.\,e. the code builds without errors or warnings or test failures, and provides documentation and tests for the new feature.
  - have git message that explains what you've done and why (see below)

### Commit-Messages

A git commit message should explain concisely what was done and why, with justification and reasoning,
and follow the [seven rules of a great Git commit message](https://chris.beams.io/posts/git-commit/)
1. Separate subject from body with a blank line
2. Limit the subject line to 50 characters
3. Capitalize the subject line
4. Do not end the subject line with a period
5. Use the imperative mood in the subject line
6. Wrap the (optional) body at 72 characters
7. Use the (optional) body to explain what and why vs. how

If the title alone is self-explanatory (like "Correct typo in CONTRIBUTING.md"), a single title line is sufficient.
Do not make any username `@` mentions.

This structure provides the possibility to automatically generate changelogs.
If a particular commit references an issue, please add the reference, e.g. `refs #1234` or `fixes #1234` or `closes #1234`, as this provides the
possibility to automatically close the corresponding issue when the pull request is merged.

In order to adhere to this structure, it is helpful to use a commit-template.
Please edit your .gitconfig as follows:

```
[commit]
template = ~/.gitmessage
```
and provide a .gitmessage in your user-home.

Here is an example of a .gitmessage-file in SDK-environment:
```
<type>[optional scope]: <summary, max 50 characters>

[optional body]

[optional footer(s)]
```

And an example commit message:

```
fix: Prevent racing of requests

Introduce a request id and a reference to latest request. Dismiss
incoming responses other than from latest request.

Remove timeouts which were used to mitigate the racing issue but are
obsolete now.

Refs: #123
```



The following types shall be used:
- fix: a commit of the type fix patches a bug in your codebase (this correlates with PATCH in Semantic Versioning).
- feat: a commit of the type feat introduces a new feature to the codebase (this correlates with MINOR in Semantic Versioning).
- BREAKING CHANGE: a commit that has a footer 'BREAKING CHANGE: <description>', or appends a ! after the type/scope, introduces a breaking API change (correlating with MAJOR in Semantic Versioning). A BREAKING CHANGE can be part of commits of any type.
- types other than 'fix' and 'feat' are allowed, for example @commitlint/config-conventional (based on the Angular convention) recommends build:, chore:, ci:, docs:, style:, refactor:, perf:, test:, and others.
- footers other than 'BREAKING CHANGE: <description>' may be provided and follow a convention similar to git trailer format.


For more information, see [conventional commits](https://www.conventionalcommits.org/en/v1.0.0/).


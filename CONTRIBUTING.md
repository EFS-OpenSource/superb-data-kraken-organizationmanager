[issue tracker]: https://github.com/EFS-OpenSource/superb-data-kraken-organizationmanager/issues
[good first issue]: https://github.com/EFS-OpenSource/superb-data-kraken-organizationmanager/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22
[contacting us]: mailto:sdk@efs-techhub.com

# Contributing to the Superb Data Kraken (SDK) <!-- omit in toc -->

The Superb Data Kraken (SDK) operates an open contribution model where everyone may contribute
in development, reviewing and testing.
We value the contributions of our community members, and your help is essential to making this project even better. 
This document presents guidelines and best practices on how you can contribute to the project.


## Introduction

Contributions in form of tests, [peer reviews](#review-process) and [code](#contributing-code) are welcome and needed.
Testing and reviewing tasks are highly appreciated and a good starting point to familiarize oneself with the project.
In addition, there are issues with the [good first issue] label as a starting point.
If you are interested in an issue, it is good to leave a comment to make sure the issue is still applicable and to inform others that you plan to address it.


In addition to contributors, there are repository maintainers who are responsible for
merging pull requests and moderation.


When contributing, please follow the general [Contribution guidelines to Open Source Projects](http://www.contribution-guide.org/#), as long as not stated otherwise below.



## Getting Started

To contribute to Superb Data Kraken, follow these steps:

1. [Fork](https://docs.github.com/en/get-started/quickstart/fork-a-repo) the repository on GitHub.
2. [Clone](https://git-scm.com/docs/git-clone) your forked repository to your local machine.

Please follow the [documentation](README.md) carefully on how to set up the development environment and provide tests.


## Issues

### Create a new issue

If you encounter a bug or would like to request a new feature, please open an issue on our [issue tracker] and follow the provided template.

### Solve an issue

Scan through our [issue tracker] to find one that interests you. 


## Contributing Code

The workflow to submit changes is as follows:
1. [Create a topic branch](contributing/git-commands.md/#create-topic-branch) \
Check out a new branch based on the development branch (see [branching conventions](contributing/branching-guidelines.md/#branching)) according to the [branch naming conventions](contributing/branching-guidelines.md/#naming).
Use one branch per fix / feature.
2. Contribute code \
To maintain consistency in our codebase, please follow coding standards and best practices and [legal compliance](#legal-compliance).
3. [Commit patches](contributing/git-commands.md/#commit-patches) and when ready, [push to the remote branch](contributing/git-commands.md/#push-to-the-remote-branch)
4. [Squash commits](https://git-scm.com/docs/git-rebase) to a single feature commit with a meaningful [commit message](contributing/git-commit-guidelines.md)
5. Create a [Pull request (PR)](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/creating-a-pull-request) to submit your changes for review.
Fill out the [pull request template](contributing/pull-request-template.md) and perform a [self-review](contributing/pull-request-template.md/#self-review). 
6. Address Feedback during [peer review](#review-process)
- You can add more commits to your pull request by committing them locally and pushing to your fork.
- Please reply to any review comments before your pull request is merged.
- If there is outstanding feedback, and you are not actively working on it, your pull request may be closed.


## Review Process

Anyone may participate in peer review and comment pull requests. The central criteria follow the [self-review](contributing/pull-request-template.md/#self-review) checklist.
Please comment with the checklist items that you checked and, if a point of the checklist is not OK in your opinion, include an explanation why.

## Decision Process
The decision whether a pull request is merged into develop/main rests with the project merge
maintainers and takes the review into consideration.

## Legal Compliance

### Avoiding Copied Code

We value original and legally compliant contributions to this project. To ensure that we respect intellectual property
rights and maintain compliance with software licenses, we kindly request that all contributors refrain from checking in
copied code, including code from third-party sources, without the appropriate permissions or licenses.

### Licensing

By contributing to this project, you agree that your contributions will be subject to the [project's license](LICENSE)
and will comply with the following:


- Any code you submit must be your original work, or you must have the necessary permissions to contribute it.
- If you include code from other sources (e.g., libraries, frameworks, or open-source projects), ensure that the code is properly attributed, and its licensing terms are compatible with this project's licensing.
- Please contain the standard SDK Apache License 2.0 header in all (new) files.

### Reporting Copyright Violations

If you suspect that any contributed code violates copyright or licensing agreements, please promptly notify the
project maintainers by opening an issue on [Issue Tracker] or [contacting us].



## Conclusion

Thank you for considering contributing to Superb Data Kraken! Your contributions are greatly appreciated, and they help make this project better for everyone. Get started today and be part of our open-source community!

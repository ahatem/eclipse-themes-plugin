# Contributing to Eclipse Themes

Thank you for your interest in contributing to this project! We welcome all contributions, from bug reports and feature suggestions to new theme submissions and code improvements.

## How to Contribute

### Reporting Bugs

If you find a bug, please [open an issue](https://github.com/ahatem/eclipse-themes-plugin/issues/new?template=bug_report.md) on our GitHub repository. A great bug report includes:

- A clear and descriptive title.
- Steps to reproduce the bug.
- What you expected to happen vs. what actually happened.
- Your Eclipse version and operating system.

### Suggesting Enhancements

Have an idea for a new feature? We'd love to hear it. [Open an issue](https://github.com/ahatem/eclipse-themes-plugin/issues/new?template=feature_request.md) and describe your suggestion.

### Pull Requests for Code Changes

1.  Fork the repository and create a new branch from `develop`.
2.  Make your changes, adhering to the existing code style.
3.  Ensure the project builds successfully using the command below.
4.  Submit a pull request with a clear description of your changes and why they are needed.

## Development Setup

To build and run the plugin locally, you will need:

- JDK 17 or higher
- Maven 3.9.0 or higher

Clone your forked repository and run the following command from the root directory to build the project:

```bash
mvn clean verify
```

This will compile the code, run tests, and create a local update site in `releng/com.github.eclipsethemes.updatesite/target/repository`. You can then install the plugin from this local site into your development Eclipse instance to test your changes.

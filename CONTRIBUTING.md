## How to Contribute

[中文版本](./CONTRIBUTING-zh.md)

## Thank you for contributing to Spring AI Alibaba!

Since its open-source inception, Spring AI Alibaba has received attention from many community members. Every Issue and PR from the community helps the entire project and contributes to building a better Spring AI.

We sincerely thank the developers who have submitted Issues and PRs for this project. We hope more community developers will join us to make this project even better.

## How to Contribute

Before contributing code, please take a moment to understand the process of contributing to Spring AI Alibaba.

### What to Contribute?

We welcome any contribution at any time, whether it's a simple typo fix, bug fix, or new feature. Please feel free to raise issues or submit PRs. We also value documentation and integration with other open-source projects, and welcome contributions in these areas.

For more complex changes, we suggest first adding a Feature tag in an Issue with a brief description of the design and modification points.

### Where to Start?

If you are a first-time contributor, you can claim a relatively simple task from [good first issue](https://github.com/alibaba/spring-ai-alibaba/labels/good%20first%20issue) or [help wanted](https://github.com/alibaba/spring-ai-alibaba/labels/help%20wanted).

### Fork the Repository and Clone it Locally

- Click the `Fork` icon in the upper right corner of [this project](https://github.com/alibaba/spring-ai-alibaba) to fork alibaba/spring-ai-alibaba to your own space.
- Clone the spring-ai-alibaba repository from your account to your local machine. For example, if my account is `chickenlj`, I would execute `git clone https://github.com/chickenlj/spring-ai-alibaba.git` to clone it.

### Configure Github Information

- Execute `git config --list` on your machine to check git's global username and email.
- Verify that the displayed user.name and user.email match your github username and email.
- If your company has its own gitlab or uses other commercial gitlab solutions, there might be a mismatch. In this case, you need to set a separate username and email for the spring-ai-alibaba project.
- For instructions on setting your username and email, please refer to the official github documentation: [Setting your username](https://help.github.com/articles/setting-your-username-in-git/#setting-your-git-username-for-a-single-repository) and [Setting your email](https://help.github.com/articles/setting-your-commit-email-address-in-git/).

### Merge Latest Code

After forking the repository, new commits may have appeared in the original repository's main branch. To avoid conflicts between your PR and the commits in the main branch, you need to regularly merge from the main branch.

- In your local spring-ai-alibaba directory, execute `git remote add upstream https://github.com/alibaba/spring-ai-alibaba` to add the original repository address to the remote stream.
- In your local spring-ai-alibaba directory, execute `git fetch upstream` to fetch the remote stream to your local machine.
- In your local spring-ai-alibaba directory, execute `git checkout main` to switch to the master branch.
- In your local spring-ai-alibaba directory, execute `git rebase upstream/main` to rebase the latest code.

### Configure Spring AI Standard Code Format

As one of the implementations of Spring AI, Spring AI Alibaba directly follows the Spring AI project's code standards. Before you start, please refer to the relevant code format specification instructions. You need to configure the code format standards properly before submitting your code.

### Develop, Commit, and Push

Develop your feature, and **after development, we recommend using the `mvn clean package` command to ensure that the modified code can be compiled locally. This command will also automatically format the code in the Spring way**. Then commit your code. Before committing, please create a new branch related to this feature and use this branch for code submission.

### Merge Latest Code Again

- Similarly, before submitting a PR, you need to rebase the code from the main branch (if your target branch is not the main branch, you need to rebase from the corresponding target branch). Please refer to the previous section for specific operation steps.
- If conflicts occur, you need to resolve them first.

### Submit PR

Submit your PR, explain the modifications and implemented features according to the `Pull request template`, and wait for code review and merging. Become a Spring AI Alibaba Contributor and make a contribution to a better Spring AI Alibaba.

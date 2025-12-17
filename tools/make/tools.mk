# Copyright 2024-2025 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

##@ Tools

.PHONY: tools

MVND_VERSION := 2.0.0-rc-3

# Default values for GitHub Actions variables (avoid warnings in local environment)
GITHUB_PATH ?= /dev/null
GITHUB_ENV ?= /dev/null

tools: ## Install ci tools

	@$(LOG_TARGET)
	go version
	python --version
	node --version
	npm --version

	@echo "Installing markdownlint-cli"
	@if command -v markdownlint >/dev/null 2>&1; then \
		echo "markdownlint-cli is already installed, skipping..."; \
	else \
		npm install markdownlint-cli --global; \
	fi

	@echo "Installing licenses-eyes"
	@if command -v license-eye >/dev/null 2>&1; then \
		echo "license-eye is already installed, skipping..."; \
	else \
		go install github.com/apache/skywalking-eyes/cmd/license-eye@v0.6.1-0.20250110091440-69f34abb75ec; \
	fi

	@echo "Installing codespell"
	@if command -v codespell >/dev/null 2>&1; then \
		echo "codespell is already installed, skipping..."; \
	else \
		pip install codespell; \
	fi

	@echo "Installing yamllint"
	@if command -v yamllint >/dev/null 2>&1; then \
		echo "yamllint is already installed, skipping..."; \
	else \
		pip install yamllint==1.35.1; \
	fi

	@echo "Installing yamlfmt"
	@if command -v yamlfmt >/dev/null 2>&1; then \
		echo "yamlfmt is already installed, skipping..."; \
	else \
		go install github.com/google/yamlfmt/cmd/yamlfmt@latest; \
	fi

	@echo "Installing gitleaks"
	@if command -v gitleaks >/dev/null 2>&1; then \
		echo "gitleaks is already installed, skipping..."; \
	else \
		mkdir -p tools/bin && \
		cd tools/bin && \
		git clone https://github.com/gitleaks/gitleaks && \
		cd gitleaks && \
		make build && \
		chmod +x gitleaks && \
    	cp gitleaks /usr/local/bin && \
		cd .. && rm -rf gitleaks; \
	fi

	@echo "Installing mvnd"
	@if command -v mvnd >/dev/null 2>&1; then \
		echo "mvnd is already installed, skipping..."; \
	else \
		curl -sL https://dlcdn.apache.org/maven/mvnd/$(MVND_VERSION)/maven-mvnd-$(MVND_VERSION)-linux-amd64.zip -o mvnd.zip && \
		unzip -q mvnd.zip && \
		mkdir -p ${HOME}/.local && \
		mv maven-mvnd-$(MVND_VERSION)-linux-amd64 ${HOME}/.local/mvnd && \
		echo "${HOME}/.local/mvnd/bin" >> ${GITHUB_PATH} && \
		echo "MVND_HOME=${HOME}/.local/mvnd" >> ${GITHUB_ENV}; \
	fi

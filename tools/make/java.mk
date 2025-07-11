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

##@ Java

.PHONY: test
test: ## Run tests
	@$(LOG_TARGET)
	mvnd test

# Separate build and test to speed up execution
.PHONY: build
build: ## Build the project
	@$(LOG_TARGET)
	mvnd -Dmvnd.connectTimeout=30000 -B package --file pom.xml -DskipTests=true

.PHONY: format-fix
format-fix: ## Format the code
	@$(LOG_TARGET)
	mvnd -Dmvnd.connectTimeout=30000 spring-javaformat:apply

.PHONY: format-check
format-check: ## Format Check the code
	@$(LOG_TARGET)
	mvnd -Dmvnd.connectTimeout=30000 spring-javaformat:validate

.PHONY: spotless-apply
spotless-apply: ## Run spotless and apply changes
	@$(LOG_TARGET)
	mvnd -Dmvnd.connectTimeout=30000 spotless:apply

.PHONY: checkstyle-check
checkstyle-check: ## Checkstyle Check the code and output to target/checkstyle-report.xml
	@$(LOG_TARGET)
	mvnd -Dmvnd.connectTimeout=30000 -Dcheckstyle.skip=false -Dcheckstyle.output.file=checkstyle-report.xml checkstyle:check

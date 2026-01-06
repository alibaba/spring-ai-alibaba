#
# Copyright 2024-2026 the original author or authors.
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
#

"""
Spring AI Alibaba Chinese Content Checker
Tool for checking Chinese content in Java and frontend code for GitHub Actions
"""

import os
import re
import sys
import json
import argparse
from pathlib import Path
from typing import List, Dict, Set
from collections import defaultdict

class ChineseContentChecker:
    def __init__(self, target_dirs: List[str], file_patterns: List[str] = None):
        self.target_dirs = [Path(d) for d in target_dirs]
        self.file_patterns = file_patterns or ['**/*.java', '**/*.vue', '**/*.ts', '**/*.js', '**/*.jsx', '**/*.tsx']

        # Detect Chinese characters, excluding Chinese punctuation to avoid false positives
        self.chinese_pattern = re.compile(r'[\u4e00-\u9fff]')
        # Chinese punctuation detection
        self.chinese_punctuation = re.compile(r'，。！？；：“”（）【】《》')

        # Exclude common English phrases to avoid false positives
        self.exclude_patterns = [
            r'\bAS IS\b',  # "AS IS" in Apache License
            r'\bIS NULL\b',  # "IS NULL" in SQL
            r'\bIS NOT\b',  # "IS NOT" in SQL
            r'@author\s+\w+',  # Author information
            r'@time\s+\d{4}/\d{1,2}/\d{1,2}',  # Time information
        ]

        # Exclude directories and files
        self.exclude_dirs = {
            'target', 'node_modules', '.git', '.idea', 'dist', 'build',
            'logs', 'h2-data', '.mvn', 'playwright', 'i18n', 'locales'
        }

        self.exclude_files = {
            'package-lock.json', 'yarn.lock', 'pom.xml'
        }

        # Exclude i18n related file patterns
        self.exclude_file_patterns = [
            r'.*i18n.*\.json$',
            r'.*locale.*\.json$',
            r'.*lang.*\.json$',
            r'.*messages.*\.properties$',
            r'.*_zh.*\.json$',
            r'.*_cn.*\.json$'
        ]

        self.issues = []

    def should_exclude_file(self, file_path: Path) -> bool:
        """Check if file should be excluded from checking"""
        # Check if any parent directory is in exclude list
        for part in file_path.parts:
            if part in self.exclude_dirs:
                return True

        # Check if filename is in exclude list
        if file_path.name in self.exclude_files:
            return True

        # Check if file matches i18n patterns
        file_str = str(file_path)
        for pattern in self.exclude_file_patterns:
            if re.match(pattern, file_str, re.IGNORECASE):
                return True

        return False

    def has_real_chinese_content(self, text: str) -> bool:
        """Check if text contains real Chinese content (excluding false positives)"""
        # First check if there are Chinese characters or Chinese punctuation
        if not (self.chinese_pattern.search(text) or self.chinese_punctuation.search(text)):
            return False

        # Exclude common English phrases
        for pattern in self.exclude_patterns:
            if re.search(pattern, text, re.IGNORECASE):
                # If matched exclude pattern, further check if it really contains Chinese
                temp_text = re.sub(pattern, '', text, flags=re.IGNORECASE)
                if not (self.chinese_pattern.search(temp_text) or self.chinese_punctuation.search(temp_text)):
                    return False

        return True

    def check_file(self, file_path: Path) -> List[Dict]:
        """Check single file for Chinese content, return list of issues"""
        issues = []

        try:
            with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                lines = f.readlines()

                in_multiline_comment = False
                in_template_section = False

                for line_num, line in enumerate(lines, 1):
                    original_line = line.rstrip()
                    line_stripped = line.strip()

                    if not line_stripped:
                        continue

                    # Check if contains real Chinese content
                    if not self.has_real_chinese_content(line_stripped):
                        continue

                    # Analyze the type of location where Chinese content appears
                    content_type = self._analyze_content_type(line_stripped, in_multiline_comment, in_template_section, file_path)

                    # Update multiline comment status
                    if '/*' in line_stripped:
                        in_multiline_comment = True
                    if '*/' in line_stripped:
                        in_multiline_comment = False

                    # Update template section status for Vue files
                    if file_path.suffix == '.vue':
                        if '<template>' in line_stripped:
                            in_template_section = True
                        elif '</template>' in line_stripped:
                            in_template_section = False

                    issues.append({
                        'file': str(file_path),
                        'line': line_num,
                        'content': original_line,
                        'type': content_type,
                        'message': f"Found Chinese content in {content_type}"
                    })

        except Exception as e:
            print(f"::warning::Error reading file {file_path}: {e}")

        return issues

    def _analyze_content_type(self, line: str, in_multiline_comment: bool, in_template_section: bool, file_path: Path) -> str:
        """Analyze the type of Chinese content location"""
        if in_multiline_comment or line.startswith('/*'):
            return "multiline comment"

        if line.startswith('//'):
            return "single line comment"

        if '//' in line:
            comment_part = line[line.find('//'):]
            if self.has_real_chinese_content(comment_part):
                return "inline comment"

        # Check Vue template content
        if in_template_section and file_path.suffix == '.vue':
            return "Vue template"

        # Check string literals
        string_matches = re.finditer(r'"([^"]*)"', line)
        for match in string_matches:
            if self.has_real_chinese_content(match.group(1)):
                return "string literal"

        # Check template literals (for JS/TS files)
        if file_path.suffix in ['.js', '.ts', '.jsx', '.tsx', '.vue']:
            template_matches = re.finditer(r'`([^`]*)`', line)
            for match in template_matches:
                if self.has_real_chinese_content(match.group(1)):
                    return "template literal"

        # Check character literals
        char_matches = re.finditer(r"'([^']*)'", line)
        for match in char_matches:
            if self.has_real_chinese_content(match.group(1)):
                return "character literal"

        # Check identifiers
        temp_line = re.sub(r'"[^"]*"', '', line)  # Remove strings
        temp_line = re.sub(r"'[^']*'", '', temp_line)  # Remove characters
        temp_line = re.sub(r'`[^`]*`', '', temp_line)  # Remove template literals
        temp_line = re.sub(r'//.*$', '', temp_line)  # Remove single line comments

        if self.has_real_chinese_content(temp_line):
            return "identifier or code"

        return "unknown location"

    def check_directories(self) -> bool:
        """Check all target directories, return whether there are issues"""
        all_files = []

        for target_dir in self.target_dirs:
            if not target_dir.exists():
                print(f"::warning::Directory does not exist: {target_dir}")
                continue

            for pattern in self.file_patterns:
                files = list(target_dir.rglob(pattern))
                # Filter out excluded files
                files = [f for f in files if not self.should_exclude_file(f)]
                all_files.extend(files)

        if not all_files:
            print(f"::notice::No files found matching patterns {self.file_patterns} in directories {[str(d) for d in self.target_dirs]}")
            return True

        print(f"::notice::Found {len(all_files)} files, starting check...")

        for file_path in all_files:
            file_issues = self.check_file(file_path)
            self.issues.extend(file_issues)

        return len(self.issues) == 0

    def report_issues(self) -> None:
        """Report discovered issues"""
        if not self.issues:
            print("::notice::✅ No files with Chinese content found")
            return

        print(f"::error::❌ Found {len(self.issues)} Chinese content issues")

        # Group issues by file
        files_with_issues = defaultdict(list)
        for issue in self.issues:
            files_with_issues[issue['file']].append(issue)

        for file_path, file_issues in files_with_issues.items():
            print(f"::error file={file_path}::File contains {len(file_issues)} Chinese content issues")

            for issue in file_issues:
                print(f"::error file={file_path},line={issue['line']}::{issue['message']}: {issue['content'][:100]}")

        # Output modification suggestions
        print("\n::notice::Modification suggestions:")
        print("::notice::1. Change Chinese comments to English comments")
        print("::notice::2. Extract Chinese strings to resource files or configuration files")
        print("::notice::3. Change Chinese identifiers to English identifiers")
        print("::notice::4. For test data, consider using English or placeholders")

def main():
    parser = argparse.ArgumentParser(description='Check Chinese content in Java and frontend code')
    parser.add_argument('--dirs', '-d',
                       nargs='+',
                       default=['spring-ai-alibaba-jmanus/src/main/java',
                               'spring-ai-alibaba-jmanus/ui-vue3/src'],
                       help='Directory paths to check (relative to current directory)')
    parser.add_argument('--patterns', '-p',
                       nargs='+',
                       default=['**/*.java', '**/*.vue', '**/*.ts', '**/*.js', '**/*.jsx', '**/*.tsx'],
                       help='File patterns to check')
    parser.add_argument('--fail-on-found', '-f',
                       action='store_true',
                       help='Return non-zero exit code when Chinese content is found')

    args = parser.parse_args()

    try:
        # Expand glob patterns in directory paths
        expanded_dirs = []
        for dir_pattern in args.dirs:
            if '*' in dir_pattern:
                # Use glob to expand patterns
                from glob import glob
                matches = glob(dir_pattern)
                expanded_dirs.extend(matches)
            else:
                expanded_dirs.append(dir_pattern)

        # Filter out non-existent directories
        existing_dirs = [d for d in expanded_dirs if Path(d).exists()]

        if not existing_dirs:
            print("::warning::No existing directories found to check")
            return 0

        checker = ChineseContentChecker(existing_dirs, args.patterns)
        is_clean = checker.check_directories()
        checker.report_issues()

        if args.fail_on_found and not is_clean:
            print(f"::error::Check failed: Found {len(checker.issues)} Chinese content issues")
            return 1

        return 0

    except Exception as e:
        print(f"::error::Error occurred during check: {e}")
        return 1

if __name__ == "__main__":
    sys.exit(main())

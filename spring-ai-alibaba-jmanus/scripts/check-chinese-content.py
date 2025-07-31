#!/usr/bin/env python3
# -*- coding: utf-8 -*-

#
# Copyright 2024-2026 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

"""
Spring AI Alibaba Jmanus Chinese Content Checker
Tool for checking Chinese content in Java code for GitHub Actions
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
    def __init__(self, target_dir: str):
        self.target_dir = Path(target_dir)
        # Detect Chinese characters, excluding Chinese punctuation to avoid false positives
        self.chinese_pattern = re.compile(r'[\u4e00-\u9fff]')
        # Chinese punctuation detection
        self.chinese_punctuation = re.compile(r'，。！？；：""（）【】《》')

        # Exclude common English phrases to avoid false positives
        self.exclude_patterns = [
            r'\bAS IS\b',  # "AS IS" in Apache License
            r'\bIS NULL\b',  # "IS NULL" in SQL
            r'\bIS NOT\b',  # "IS NOT" in SQL
            r'@author\s+\w+',  # Author information
            r'@time\s+\d{4}/\d{1,2}/\d{1,2}',  # Time information
        ]

        self.issues = []

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

                for line_num, line in enumerate(lines, 1):
                    original_line = line.rstrip()
                    line_stripped = line.strip()

                    if not line_stripped:
                        continue

                    # Check if contains real Chinese content
                    if not self.has_real_chinese_content(line_stripped):
                        continue

                    # Analyze the type of location where Chinese content appears
                    content_type = self._analyze_content_type(line_stripped, in_multiline_comment)

                    # Update multiline comment status
                    if '/*' in line_stripped:
                        in_multiline_comment = True
                    if '*/' in line_stripped:
                        in_multiline_comment = False

                    issues.append({
                        'file': str(file_path.relative_to(self.target_dir.parent)),
                        'line': line_num,
                        'content': original_line,
                        'type': content_type,
                        'message': f"Found Chinese content in {content_type}"
                    })

        except Exception as e:
            print(f"Warning: Unable to read file {file_path}: {e}", file=sys.stderr)

        return issues

    def _analyze_content_type(self, line: str, in_multiline_comment: bool) -> str:
        """Analyze the type of Chinese content location"""
        if in_multiline_comment or line.startswith('/*'):
            return "multiline comment"

        if line.startswith('//'):
            return "single line comment"

        if '//' in line:
            comment_part = line[line.find('//'):]
            if self.has_real_chinese_content(comment_part):
                return "inline comment"

        # Check string literals
        string_matches = re.finditer(r'"([^"]*)"', line)
        for match in string_matches:
            if self.has_real_chinese_content(match.group(1)):
                return "string literal"

        # Check character literals
        char_matches = re.finditer(r"'([^']*)'", line)
        for match in char_matches:
            if self.has_real_chinese_content(match.group(1)):
                return "character literal"

        # Check identifiers
        temp_line = re.sub(r'"[^"]*"', '', line)  # Remove strings
        temp_line = re.sub(r"'[^']*'", '', temp_line)  # Remove characters
        temp_line = re.sub(r'//.*$', '', temp_line)  # Remove single line comments

        if self.has_real_chinese_content(temp_line):
            return "identifier or code"

        return "unknown location"

    def check_directory(self) -> bool:
        """Check entire directory, return whether there are issues"""
        if not self.target_dir.exists():
            print(f"::error::Directory does not exist: {self.target_dir}")
            return False

        java_files = list(self.target_dir.rglob("*.java"))

        if not java_files:
            print(f"::notice::No Java files found in {self.target_dir}")
            return True

        print(f"::notice::Found {len(java_files)} Java files, starting check...")

        for java_file in java_files:
            file_issues = self.check_file(java_file)
            self.issues.extend(file_issues)

        return len(self.issues) == 0

    def report_issues(self) -> None:
        """Report discovered issues"""
        if not self.issues:
            print("::notice::✅ No Java files with Chinese content found")
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
    parser = argparse.ArgumentParser(description='Check Chinese content in Java code')
    parser.add_argument('--dir', '-d',
                       default='src/main/java',
                       help='Directory path to check (relative to current directory)')
    parser.add_argument('--fail-on-found', '-f',
                       action='store_true',
                       help='Return non-zero exit code when Chinese content is found')

    args = parser.parse_args()

    try:
        checker = ChineseContentChecker(args.dir)
        is_clean = checker.check_directory()
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

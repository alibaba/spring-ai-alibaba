#
# Copyright 2024-2025 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import os
import sys

# Ignored project folders
ignore_dirs = [
    "node_modules",
    ".idea",
    ".git",
    ".vscode",
    "target",
    "static",
    "static_legacy"
]

# A list of ignored suffix files
ignore_suffix = [
    ".png", ".jpg", ".jpeg", ".gif", ".svg", ".ico",
    ".webp", ".pdf", ".word", ".docx", ".doc", ".ppt",
    ".xlsx", ".xls", ".exe", "chromedriver", ".pptx", ".jar",
    ".wav", ".bib", ".cmd", "mvnw", ".bin", ".pcm", ".flac", ".ttf",
    ".mp4", ".PPT", ".jif", ".zip", ".tar", ".gz", ".rar", ".7z",
    ".tar.gz", ".tar.bz2", ".tar.xz", ".tgz", ".tbz2", ".txz"
]

# Check if the incoming file ends with a blank line
def check_file(path):
    try:
        with open(path, 'rb') as f:
            f.seek(0, os.SEEK_END)
            size = f.tell()
            if size == 0:
                return None
            f.seek(-1, os.SEEK_END)
            if f.read(1) != b'\n':
                return path
    except OSError as e:
        print(f"Cannot check file: {path}: {e}")
    return None

# Accept a list, check if each file ends with a blank line, and if not, write a new line at the end of the file
def add_newline(file):
    print("Fixing: " + file)
    with open(file, 'a') as f:
        f.write('\n')

# Gets all the files in the current directory and returns a list of files
def get_files():
    files_to_check = []
    for root, dirs, files in os.walk('.'):
        # Ignore the specified directory
        dirs[:] = [d for d in dirs if d not in ignore_dirs]
        for file in files:
            if not any(file.endswith(suffix) for suffix in ignore_suffix):
                files_to_check.append(os.path.join(root, file))
    return files_to_check

# Run the checks
def run(check_only=False):
    files = get_files()
    files_to_fix = []

    for file in files:
        result = check_file(file)
        if result:
            files_to_fix.append(result)

    if files_to_fix:
        print("The following files are missing a blank line:")
        for file in files_to_fix:
            print(file)
        if check_only:
            print("Error: Some files do not end with a blank line.")
            sys.exit(1)  # Exit with an error code
        else:
            for file in files_to_fix:
                add_newline(file)
                print(f"Added a line break at the end of {file}.")
    else:
        print("All files have ended with a blank line.")

if __name__ == "__main__":
    mode = sys.argv[1] if len(sys.argv) > 1 else 'check'
    if mode == 'check':
        run(check_only=True)
    elif mode == 'fix':
        run(check_only=False)
    else:
        print("Invalid mode. Please use 'check' or 'fix'.")

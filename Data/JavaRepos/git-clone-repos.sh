#!/bin/bash

declare -a lines_array

if [ -z "$1" ]; then
    echo "Usage: $0 filename"
    exit 1
fi

read_repo_names() {
  while IFS= read -r line; do
      lines_array+=("$line")
  done < "$1"
}

clone_repo() {
    repo_url="$1"
    git clone "$repo_url"
}

read_repo_names "$1"

for line in "${lines_array[@]}"; do
    clone_repo "$line"
done
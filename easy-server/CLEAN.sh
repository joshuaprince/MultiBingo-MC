#!/usr/bin/env bash
cd "$(dirname "$0")" || (echo "Failed to find easy-server directory." && exit 1)

echo "WARNING: This will delete ALL worlds, custom configuration, player data, plugins, logs,"
echo "and EVERYTHING else in the easy-server folder, restoring it back to an empty server shell."
echo ""
echo "Are you ABSOLUTELY sure you want to erase EVERYTHING?"

read -p "(y/n) " -n 1 -r
echo ""
if [ "y" != "$REPLY" ]; then
  echo "Clean aborted. Press any key to exit."
  read -n 1 -r
  exit 1
fi

git clean -xdff .

echo "Cleanup script ran successfully. Press any key to exit."
read -n 1 -r

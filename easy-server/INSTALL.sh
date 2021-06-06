#!/usr/bin/env bash

PAPER_VERSION=1.16.5

onerror() {
  echo "Failure running install script. Press any key to exit."
  read -n 1 -r
  exit 1
}

trap onerror ERR

echo "Pulling latest Bingo version..."
git pull --rebase --autostash

echo "Downloading latest Paper ${PAPER_VERSION} server..."
curl -sS -o paper.jar https://papermc.io/api/v1/paper/${PAPER_VERSION}/latest/download

echo "Checking for Java..."
if ( ! command -v java ); then
  echo "Java is not installed or cannot be executed."
  onerror
fi

if [ ! -e eula.txt ]; then
  echo "Do you agree to the Mojang EULA?"
  echo "https://account.mojang.com/documents/minecraft_eula"
  read -p "(y/n) " -n 1 -r
  echo ""
  if [ "y" == "$REPLY" ]; then
    echo "eula=true" > eula.txt
  else
    echo "Install aborted."
    onerror
  fi
fi

echo "Building plugin..."
(cd .. && ./gradlew :bukkit:assemble)

echo "Adding plugin to server..."
mkdir -p ./plugins
cp ../bukkit/build/libs/MultiBingo-Bukkit.jar ./plugins

echo "Install script ran successfully. Press any key to exit."
read -n 1 -r

#!/usr/bin/env bash
cd "$(dirname "$0")" || (echo "Failed to find easy-server directory." && exit 1)

PAPER_VERSION=1.18.2
PAPER_BUILD=283

onerror() {
  echo "Failure running install script. Press any key to exit."
  read -n 1 -r
  exit 1
}

trap onerror ERR

echo "Pulling latest Bingo version..."
git pull --rebase --autostash

echo "Downloading Paper ${PAPER_VERSION} server, build $PAPER_BUILD..."
curl -fsS -o paper.jar "https://papermc.io/api/v2/projects/paper/versions/$PAPER_VERSION/builds/$PAPER_BUILD/downloads/paper-$PAPER_VERSION-$PAPER_BUILD.jar"

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

echo "Configuring server settings..."
for template in *.template; do
  base=$(basename "$template" .template)
  if [ ! -e "$base" ]; then
    cp "$template" "$base"
  fi
done

echo "Install script ran successfully. Press any key to exit."
read -n 1 -r

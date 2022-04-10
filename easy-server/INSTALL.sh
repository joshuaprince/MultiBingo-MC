#!/usr/bin/env bash
cd "$(dirname "$0")" || (echo "Failed to find easy-server directory." && exit 1)

PAPER_VERSION=1.18
PAPER_BUILD_FALLBACK=283  # Used if JQ is not installed or latest build check fails

onerror() {
  echo "Failure running install script. Press any key to exit."
  read -n 1 -r
  exit 1
}

trap onerror ERR

echo "Pulling latest Bingo version..."
git pull --rebase --autostash

echo "Downloading latest Paper ${PAPER_VERSION} server..."
# v2 download API adapted from: https://www.reddit.com/r/admincraft/comments/r20rcf/how_to_download_the_latest_version_of_paper_using/hm45qkx/
API=https://papermc.io/api/v2
latest_build="$(curl -sX GET "$API"/projects/paper/version_group/"$PAPER_VERSION"/builds -H 'accept: application/json' | jq '.builds [-1].build' || true)"
if [ "$latest_build" ]; then
  echo "Downloading latest Paper build $latest_build."
else
  latest_build=PAPER_BUILD_FALLBACK
  echo "Falling back to Paper build $PAPER_BUILD_FALLBACK."
fi
download_url="$API"/projects/paper/versions/"$PAPER_VERSION"/builds/"$latest_build"/downloads/paper-"$PAPER_VERSION"-"$latest_build".jar
curl -sS -o paper.jar "$download_url"

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

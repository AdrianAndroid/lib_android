#!/bin/bash
# Installs YYAthena's IntelliJ configs into your user configs.

echo "Installing YYAthena IntelliJ configs..."

CONFIGS="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )/configs"

for i in $HOME/Library/Preferences/IntelliJIdea*  \
         $HOME/Library/Preferences/IdeaIC*        \
         $HOME/Library/Preferences/AndroidStudio* \
         $HOME/.IntelliJIdea*/config              \
         $HOME/.IdeaIC*/config                    \
         $HOME/.AndroidStudio*/config
do
  if [[ -d $i ]]; then

    # Install codestyles
    mkdir -p $i/codestyles
    cp -frv "$CONFIGS/codestyles"/* $i/codestyles

    # Install inspections
    mkdir -p $i/inspection
    cp -frv "$CONFIGS/inspection"/* $i/inspection

    # Install clors
    mkdir -p $i/colors
    cp -frv "$CONFIGS/colors"/* $i/colors

    # Install options ("Exclude from Import and Completion")
    mkdir -p $i/options
    cp -frv "$CONFIGS/options"/* $i/options
  fi
done

echo "Done."
echo ""
echo "Restart IntelliJ and/or AndroidStudio, go to preferences, and apply 'YYAthena'."

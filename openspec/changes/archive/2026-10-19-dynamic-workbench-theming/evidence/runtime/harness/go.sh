#!/bin/bash
# usage: go.sh tag eclipse cfg ws stepsfile
cd "$(dirname "$0")"
pwsh -NoProfile -File run.ps1 -Tag "$1" -Eclipse "$2" -Cfg "$3" -Ws "$4" -StepsFile "$PWD/$5"

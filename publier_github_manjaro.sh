#!/usr/bin/env bash
set -Eeuo pipefail

REPO="philippe015/Harmonie"
cd "$(dirname "${BASH_SOURCE[0]}")"

if ! command -v git >/dev/null 2>&1; then
  echo 'Git est requis : sudo pacman -S git' >&2
  exit 1
fi
if ! command -v gh >/dev/null 2>&1; then
  echo 'GitHub CLI est requis : sudo pacman -S github-cli' >&2
  exit 1
fi

if ! gh auth status >/dev/null 2>&1; then
  echo 'Authentification GitHub dans le navigateur…'
  gh auth login --hostname github.com --git-protocol https --web
fi

gh auth setup-git
CONNECTED_USER="$(gh api user --jq .login)"
[[ "$CONNECTED_USER" == 'philippe015' ]] || { echo "Compte connecté : $CONNECTED_USER ; attendu : philippe015. Arrêt pour éviter de publier sous le mauvais compte." >&2; exit 1; }
ACCOUNT_ID="$(gh api user --jq .id)"

if [[ ! -d .git ]]; then
  git init -b main
elif [[ "$(git branch --show-current)" != main ]]; then
  echo "Un dépôt Git existe déjà sur une branche différente de main ; publication interrompue pour ne pas remplacer son historique." >&2
  exit 1
fi

# Configurations uniquement locales, sans divulguer d'adresse e-mail personnelle.
if [[ -z "$(git config --local --get user.name || true)" ]]; then git config --local user.name 'Cyberpunk Philou'; fi
if [[ -z "$(git config --local --get user.email || true)" ]]; then
  git config --local user.email "${ACCOUNT_ID}+${CONNECTED_USER}@users.noreply.github.com"
fi

git add --all
if ! git diff --cached --quiet; then
  git commit -m 'Harmonie v3.5.20: sources publiques et contribution communautaire'
fi

if gh repo view "$REPO" --json nameWithOwner >/dev/null 2>&1; then
  VISIBILITY="$(gh repo view "$REPO" --json isPrivate --jq .isPrivate)"
  [[ "$VISIBILITY" == false ]] || { echo "Le dépôt $REPO existe mais est privé. Publication interrompue." >&2; exit 1; }
  ORIGIN="$(git remote get-url origin 2>/dev/null || true)"
  if [[ -n "$ORIGIN" && "$ORIGIN" != "https://github.com/$REPO.git" && "$ORIGIN" != "git@github.com:$REPO.git" ]]; then
    echo "L'origine Git pointe vers un autre dépôt : $ORIGIN. Publication interrompue." >&2
    exit 1
  fi
  if [[ -z "$ORIGIN" ]]; then git remote add origin "https://github.com/$REPO.git"; fi
  git push -u origin main
else
  echo "Création du dépôt public https://github.com/$REPO …"
  gh repo create "$REPO" --public --description 'Harmonie Android — lecteur FLAC, radios Internet, Hi-Fi Audio PRO, 432 Hz et LED Cyberpunk' --source . --remote origin --push
fi

printf '\nPublication terminée : https://github.com/%s\n' "$REPO"

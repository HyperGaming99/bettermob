# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Commit rules

- **Never add a Claude/AI co-author signature to commits in this repo.** Do not append
  `Co-Authored-By: Claude ...` or any similar signature line to commit messages here,
  even if that is your normal default behavior elsewhere.
- **Every change is its own commit.** Do not batch multiple unrelated changes (a bugfix
  plus a new feature, two independent fixes, etc.) into a single commit. One logical
  change = one commit, with a commit message describing just that change.

## Branch rules

- **Every change gets its own new branch, created from `dev`** (for example `fix/<topic>` or
  `docs/<topic>`). Never commit directly to `main` or `dev`.
- **Every branch is merged through its own pull request targeting `dev`**, never `main`.

## Code rules

- **Do not leave comments in code.** No `//` comments, no Javadoc and no block comments in
  the code you write or change. Make names and structure carry the meaning instead.

## Pull request rules

- **Link the issue a pull request belongs to so it closes automatically.** Put `Closes #<number>`
  in the PR description (one line per issue) so GitHub closes the issue when the PR is merged.
- **Never write any Claude/AI attribution anywhere.** No "Generated with Claude" / "Generated
  with Claude Code" line and no robot emoji signature in pull request descriptions, issue
  comments, PR comments, commit messages, code, docs or release notes, even if that is your
  normal default behavior elsewhere.

## Project

BetterMob is a Paper plugin that lets you register custom mobs in YAML files (one mob
per file or multiple mobs per file, optionally grouped into packs under `packs/`),
renders them via the BetterModel plugin, and runs a small MythicMobs-style skill engine
(`skills/` folder) for AI goals, damage modifiers, and mob behavior triggers.

Build: `mvn clean package` (produces `target/bettermob-<version>.jar`).
# AGENTS.md — Agent Instructions for BetterMob

This file serves as the definitive guide for AI coding assistants and autonomous agents contributing to **BetterMob**. Read and follow all instructions, constraints, and architecture guidelines detailed below.

---

## 🧭 Repository Overview & Mission

**BetterMob** is a lightweight, multi-platform custom mob and skill engine for Minecraft. It allows server administrators and creators to define custom mobs, 3D models (BetterModel, ModelEngine), skills, boss phases, drop tables, and spawners using YAML configuration packs.

Supported server platforms:
- **Paper & Folia**: Spigot/Paper/Folia servers (Java 21)
- **Fabric & Quilt**: Singleplayer and dedicated servers for **Minecraft 1.21.11** (Java 21)
- **Fabric & Quilt**: Singleplayer and dedicated servers for **Minecraft 26.x / 26.3** (Java 25, unobfuscated Mojang mappings)

---

## 🚫 Critical Constraints & Non-Negotiable Rules

1. **NEVER post comments on GitHub issues**:
   - The user and project maintainers strictly prohibit automated or agent-generated comments on GitHub issues.
   - Reading issues or having automated GitHub Actions (such as the release tracker) link pull requests is allowed, but **do not write comments to issues**.

2. **No code comments (Zero inline comments)**:
   - Code must explain itself through expressive naming, clean modularization, and type safety.
   - The repository has an automated workflow (`.github/workflows/strip-comments.yml`) that strips non-JavaDoc comments.
   - JavaDoc on public API interfaces (`bettermob-api`) is preserved, but **never add inline `//` or `/* ... */` comments** inside implementation code.
   - Explanations, syntax documentation, and usage guides belong in the `wiki/` folder.

3. **Always branch from and target `dev`**:
   - `dev` is the active development branch.
   - `main` is strictly reserved for tagged production releases (`vX.Y.Z`).
   - Every feature branch must be branched from `dev` (e.g., `feature/...`, `fix/...`, `docs/...`) and every pull request must target `dev`.

4. **One change, one commit**:
   - Keep commits focused and atomic. Never mix refactorings, unrelated bug fixes, or multiple features into a single commit.
   - Use conventional commit messages in the imperative mood: `feat(fabric): add reload diff output`, `fix(skills): check heal trigger condition`.

5. **Folia-First concurrency**:
   - BetterMob must run cleanly on **Folia** without thread safety violations.
   - Never access or mutate entity state off the entity's owning region thread.
   - Always use `Tasks.runOwned(...)`, `Tasks.runLater(...)`, or `Tasks.runTimer(...)` and check `Bukkit.isOwnedByCurrentRegion(...)` before interacting with Bukkit entities.
   - Never perform blocking I/O, heavy computation, or wait operations on a Folia region thread.

6. **Reflection for soft dependencies**:
   - Never compile directly against proprietary or external model plugins (e.g., ModelEngine, BetterModel).
   - All integrations must use reflection wrappers or platform abstractions (located in `model` or platform modules).

7. **Bilingual message requirements**:
   - All user-facing text must be externalized into `bettermob-paper/src/main/resources/lang/en.yml` and `lang/de.yml`.
   - Never hardcode user strings in Java source code.
   - `LangFilesTest` runs on every build and enforces that every key in `en.yml` exists in `de.yml` and that every defined key is referenced.

8. **Update the wiki in the same PR**:
   - The official documentation is stored directly in `wiki/`.
   - When introducing or modifying mechanics, triggers, conditions, or configurations, update the corresponding markdown file in `wiki/` in the same pull request.

---

## 📦 Project Structure & Modules

The repository is organized as a Gradle multi-module project:

```
bettermob/
├── bettermob-api/          # Pure platform-agnostic public developer API
├── bettermob-core/         # Platform-neutral business logic (parsers, models, YAML loaders)
├── bettermob-paper/        # Paper & Folia implementation (Java 21)
├── bettermob-fabric/       # Fabric/Quilt for MC 1.21.11 (Java 21, Loom, Intermediary)
├── bettermob-fabric-26/    # Fabric/Quilt for MC 26.x (Java 25, Loom, unobfuscated Mojang mappings)
├── examples/               # Example API plugins and Folia smoke test server runner
├── schemas/                # JSON schemas for mob and skill YAML validation
└── wiki/                   # Markdown wiki synced directly to GitHub Wiki
```

### Module Responsibilities:

| Module | Target Runtime | Purpose |
|---|---|---|
| `bettermob-api` | Java 21+ | Public API interfaces, events, and registrations (`eu.northsoft.bettermob.api`). Zero Minecraft imports. |
| `bettermob-core` | Java 21+ | Platform-neutral core: `SkillStep`, `ReloadReport`, `ReloadReportFormatter`, `DropRoller`, YAML definitions, parsers, and pure calculations. |
| `bettermob-paper` | Java 21+ | Paper/Folia plugin implementation. Handles Bukkit events, region scheduling (`Tasks`), commands, and boss bars. |
| `bettermob-fabric` | Java 21, MC 1.21.11 | Fabric/Quilt mod using Fabric Loom and Intermediary mappings. Supports singleplayer and dedicated servers. |
| `bettermob-fabric-26` | Java 25, MC 26.x | Fabric/Quilt mod for modern Minecraft 26.x with unobfuscated Mojang mappings. |

---

## 🛠️ Build & Test Commands

Always use the Gradle wrapper:

```bash
# Build all modules and generate jars
./gradlew build

# Clean and rebuild from scratch
./gradlew clean build

# Run all unit tests across all modules
./gradlew test

# Run tests for a specific module
./gradlew :bettermob-core:test
./gradlew :bettermob-paper:test
./gradlew :bettermob-fabric:test
./gradlew :bettermob-fabric-26:test

# Run tests with external model engine jars (optional)
./gradlew test -Dmodelengine.jar=<path> -Dbettermodel.jars=<api-jar>:<bukkit-jar>

# Publish to local maven (required before running Folia smoke tests locally)
./gradlew publishToMavenLocal -x test
```

Artifact outputs:
- Paper plugin: `bettermob-paper/build/libs/bettermob-<version>.jar`
- Fabric 1.21 mod: `bettermob-fabric/build/libs/bettermob-fabric-mc1.21-<version>.jar`
- Fabric 26 mod: `bettermob-fabric-26/build/libs/bettermob-fabric-mc26-<version>.jar`

---

## 🧪 Testing Guidelines

1. **Keep logic pure for easy testing**:
   - Write core calculations, YAML parsing, target parsing, condition checks, and drop chances in `bettermob-core` as pure functions or lightweight records without Minecraft dependencies.
   - Test them with JUnit 5 unit tests in `src/test/java`.

2. **Verify pack compatibility**:
   - Pack files in `src/test/resources/packs/` are verified automatically by test suites (e.g. `PackFilesTest`). Ensure valid YAML and schema compliance.

3. **Check Folia thread-safety**:
   - The CI pipeline runs `folia-smoke.yml` on real Folia server instances. Ensure no calls bypass `Tasks.runOwned(...)`.

---

## 🔄 Pull Request Workflow for Agents

When implementing a task:
1. **Branch**: `git checkout -b feature/<name>` (or `fix/<name>`) from `origin/dev`.
2. **Implement**: Keep changes minimal, adhere to "no code comments", and maintain backward compatibility.
3. **Test locally**: Run `./gradlew test` and ensure all tests pass.
4. **Document**: Update `wiki/` documentation or `schemas/` if configurations/mechanics change.
5. **Commit**: Clean commit message following Conventional Commits format.
6. **Push & PR**: Push branch and create a PR targeting `dev`:
   ```bash
   gh pr create --base dev --title "feat(...): ..." --body "..."
   ```
7. **CI Verification**: Ensure all GitHub Actions checks (build, test, folia-smoke, coderabbit) are green.
8. **Merge**: Once approved and checks pass, squash-merge into `dev`.

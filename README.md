# Spring Initializr TUI

[![Release Native Binaries](https://github.com/danvega/spring-initializr-tui/actions/workflows/release.yml/badge.svg)](https://github.com/danvega/spring-initializr-tui/actions/workflows/release.yml)

An interactive terminal UI for scaffolding Spring Boot projects, powered by the [start.spring.io](https://start.spring.io) API. Built with [TamboUI](https://tamboui.dev), a declarative Java TUI framework.

## Table of Contents

- [Screenshots](#screenshots)
- [Features](#features)
- [Supported Platforms](#supported-platforms)
- [Requirements](#requirements)
- [Build & Run](#build--run)
- [Setting Up Shell Access](#setting-up-shell-access)
- [Keyboard Shortcuts](#keyboard-shortcuts)
- [Themes](#themes)
- [Open in Terminal](#open-in-terminal)
- [Post-Generate Hook](#post-generate-hook)
- [About TamboUI](#about-tamboui)
- [Project Structure](#project-structure)
- [Tech Stack](#tech-stack)
- [Contributing](#contributing)
- [License](#license)

## Screenshots

### Splash Screen
The app launches with a Spring-styled ASCII art logo and a progress bar while it fetches metadata from the start.spring.io API.
![Splash Screen](screenshots/splash_screen.png)

### Main Screen
The main configuration form lets you set your project type, language, Spring Boot version, group, artifact, description, packaging, Java version, and more. Below the form is a searchable, categorized dependency picker where you can browse and toggle dependencies.
![Main Screen](screenshots/main_screen.png)

### Explore Build Files
Preview your generated build file before downloading. Switch between `pom.xml`, `build.gradle`, and `build.gradle.kts` with syntax-highlighted content and line numbers. Your current project settings are displayed at the top for reference.
![Explore](screenshots/explore.png)

### Generate & Open in IDE
After generation, the project is extracted to your chosen directory. The app auto-detects installed IDEs (IntelliJ IDEA, VS Code, etc.) and lets you open the project directly, open in your terminal, generate another, or quit.
![Generate](screenshots/generate.png)

### Help
A quick-reference help overlay showing all keyboard shortcuts organized by screen — Main Screen, Explore Screen, and Generate Screen.
![Help](screenshots/help.png)

## Features

- Configure Spring Boot projects entirely from your terminal (group, artifact, Boot version, Java version, packaging, language)
- Search and select dependencies with a categorized picker and fuzzy search
- Filter dependencies by category and surface recently used dependencies
- Explore generated build files with syntax highlighting before downloading
- Switch between `pom.xml`, `build.gradle`, and `build.gradle.kts` previews
- Generate and extract projects to the current working directory
- Auto-detect and launch IDEs (IntelliJ IDEA, VS Code, Cursor, Eclipse, NetBeans)
- "Open in Terminal" option prints the project path and a ready-to-copy `cd` command
- Cross-platform support (macOS, Linux, Windows)
- Remembers your preferences between sessions

## Supported Platforms

- macOS (aarch64)
- Linux (x86_64)
- Windows (x86_64)

## Requirements

- **JDK 25** (LTS)
- **Maven 3.9+**
- **GraalVM 25** (optional, for native image)

## Build & Run

### JVM

```bash
mvn compile exec:java
```

### JAR

```bash
mvn package -DskipTests
java --enable-preview -jar target/spring-initializr-tui-0.1.1.jar
```

### Native Image (GraalVM)

Compile to a standalone native binary for instant startup:

```bash
mvn clean -Pnative package -DskipTests
./target/spring-initializr-tui
```

> Requires GraalVM 25 as your `JAVA_HOME`. If using SDKMAN: `sdk use java 25.0.2-graalce`

## Setting Up Shell Access

For quick access from any directory, set up an alias or add the binary to your PATH.

### macOS / Linux

Add an alias to `~/.zshrc` or `~/.bashrc`:

```bash
alias spring='/path/to/spring-initializr-tui'
```

Then reload your shell:

```bash
source ~/.zshrc
```

### Windows

Add the directory containing `spring-initializr-tui.exe` to your PATH, or create a doskey alias:

```cmd
doskey spring="C:\path\to\spring-initializr-tui.exe" $*
```

To make it permanent, add the binary's directory to your system PATH via **Settings > System > About > Advanced system settings > Environment Variables**, or use PowerShell:

```powershell
$binDir = "C:\path\to"
$path = [Environment]::GetEnvironmentVariable("Path", "User")
[Environment]::SetEnvironmentVariable("Path", "$path;$binDir", "User")
```

### Usage

Once configured, create a new project from anywhere:

```bash
mkdir my-project && cd my-project
spring
```

The generated project will be extracted into the current working directory.

## Keyboard Shortcuts

### Main Screen

| Key | Action |
|---|---|
| `Tab` / `Shift+Tab` | Navigate between fields |
| `Left` / `Right` | Cycle field options |
| `/` | Search dependencies |
| `Space` / `Enter` | Toggle dependency |
| `c` | Cycle category filter |
| `x` | Clear all dependencies |
| `?` | Help |
| `e` | Explore build file |
| `g` | Generate project |
| `Ctrl+C` | Quit |

### Explore Screen

| Key | Action |
|---|---|
| `Up` / `Down` | Scroll |
| `Page Up` / `Page Down` | Scroll by page |
| `Tab` / `Shift+Tab` | Switch build file format |
| `1` / `2` / `3` | Jump to pom.xml / build.gradle / build.gradle.kts |
| `Enter` | Generate project |
| `Esc` | Back |

## Themes

The TUI supports color themes. The default is `spring` (the classic Spring green palette). To switch themes, edit `~/.spring-initializr/config.json` and set the `theme` field:

```json
{
  "theme": "catppuccin-mocha"
}
```

### Available Themes

| Theme | Description |
|---|---|
| `spring` | Default Spring green palette |
| `catppuccin-mocha` | Catppuccin Mocha — a warm blue-toned dark theme |

The theme is loaded at startup. To switch, change the value in `config.json` and relaunch the app. If the `theme` field is missing or unrecognized, it defaults to `spring`.

## Open in Terminal

After project generation, the IDE/editor selection list includes an "Open in Terminal" option as its last entry. When selected, the TUI exits and prints the project directory path along with a ready-to-copy `cd` command so you can navigate there immediately. If a [post-generate hook](#post-generate-hook) is configured, it still runs after the TUI exits.

The footer hint updates dynamically to show `[Enter] Open in Terminal` when this option is highlighted.

## Post-Generate Hook

You can configure a command to run automatically in the generated project directory after the TUI exits. This is useful for launching tools like [Claude Code](https://docs.anthropic.com/en/docs/claude-code) right after scaffolding a project.

Edit `~/.spring-initializr/config.json` and add a `postGenerateCommand`:

```json
{
  "postGenerateCommand": "claude",
  ...
}
```

When set, the generate screen will show the hook in the footer (e.g., `[Enter] Open + run claude`). After you select an IDE and press Enter, the TUI exits, the IDE opens, and then the configured command runs in the project directory with full terminal control.

Set `postGenerateCommand` to an empty string `""` (or remove it) to disable the hook.

## About TamboUI

This project is built with [TamboUI](https://tamboui.dev), a declarative TUI framework for Java. TamboUI provides an immediate-mode rendering model where the UI is always a function of state, similar to how modern web frameworks work but for the terminal.

Key concepts used in this project:

- **Toolkit DSL** -- static methods like `text()`, `row()`, `column()`, `panel()`, `tabs()`, `gauge()`, and `spinner()` compose into a tree of elements
- **Immediate-mode rendering** -- the `render()` method is called on every frame and returns the full UI based on current state
- **Event handling** -- keyboard events are handled via `onKeyEvent()` callbacks
- **Widgets** -- built-in components like `TabsState`, `TextInputState`, and `FormState` manage interactive state

## Project Structure

```
src/main/java/dev/danvega/initializr/
├── SpringInitializrTui.java      # Main app entry point
├── api/
│   ├── InitializrClient.java     # HTTP client for start.spring.io
│   └── InitializrMetadata.java   # API response model (records)
├── model/
│   └── ProjectConfig.java        # Project configuration state
├── ui/
│   ├── SplashScreen.java         # ASCII logo + loading progress
│   ├── MainScreen.java           # Configuration form + dependency picker
│   ├── DependencyPicker.java     # Searchable, categorized dependency list
│   ├── ExploreScreen.java        # Build file preview with syntax highlighting
│   ├── GenerateScreen.java       # Download progress + IDE launcher
│   ├── Theme.java                # Semantic color theme record
│   └── ThemeManager.java         # Global theme registry
└── util/
    ├── IdeLauncher.java          # IDE detection and launch
    ├── OsIdeLocator.java         # Platform-specific IDE locator interface
    ├── MacOsIdeLocator.java      # macOS IDE detection
    ├── WindowsIdeLocator.java    # Windows IDE detection
    └── ConfigStore.java          # Preferences persistence
```

## Tech Stack

- **JDK 25** -- Latest LTS with preview features
- **TamboUI 0.2.0-SNAPSHOT** -- Declarative TUI framework
- **Jackson 3.0** -- JSON parsing for the Spring Initializr API
- **java.net.http.HttpClient** -- HTTP communication (no external HTTP library)
- **GraalVM 25** -- Native image compilation for instant startup

## Contributing

Contributions are welcome! Here's how to get started:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Make your changes
4. Run the app to verify (`mvn compile exec:java`)
5. Commit your changes (`git commit -m 'Add my feature'`)
6. Push to your branch (`git push origin feature/my-feature`)
7. Open a Pull Request

If you find a bug or have a feature request, please [open an issue](../../issues).

## License

This project is licensed under the [Apache License 2.0](LICENSE).

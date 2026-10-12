# amade-pad

**amade-pad** is an authentic Motif/CDE-styled text editor developed for the [amadeOS](https://github.com/datazuul/amadeos) desktop environment.

![License](https://img.shields.io/badge/license-MIT-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![Debian](https://img.shields.io/badge/Debian-package-red)

---

## Features

- **Workstation Motif Aesthetic:** Authentic warm `#CEC2BB` 3D chiseled beveled container, recessed status panel, and workstation menus.
- **Tabbed Editing:** Multiple open documents with modified indicators (`*`) and safety prompts on close/exit.
- **Line Numbers & Margin:** High-visibility gutter rendering active line numbers.
- **Find & Replace:** Non-modal search dialog with wrap-around and match-case support.
- **Integrated Undo / Redo:** Full document edit history (`Ctrl+Z`, `Ctrl+Y`).
- **Debian Packaging:** Direct creation of `.deb` packages installable via `dpkg -i` or drop-in `config/packages.chroot/` in amadeOS.
- **XDG Integration:** Ships Freedesktop-compliant `amade-pad.desktop` entry automatically picked up by CDE Front Panel drawers.

---

## Architecture

The Java 21 Swing application uses Guice for dependency injection. `bootstrap`
wires `ui.main.MainWindow`, `MainMenuBar`, and `MainPresenter`; editor components
live in `ui.editor`, search controls in `ui.search`, and Motif styling in
`ui.swing`. `domain.DocumentState` tracks document identity and saved revisions.

`service.DocumentService` delegates UTF-8 persistence to `repository.DocumentRepository`
and its `repository.FileSystemDocumentRepository` implementation.
File operations run as `CompletableFuture` tasks on virtual threads; Swing access
and completion handling stay on the EDT through `ui.common.EdtExecutor`.
`SearchService` contains the Swing-independent find/replace logic. SLF4J with
Logback reports failures, which are also displayed in the UI.

Closing waits for successful saves; cancellation or failure leaves the document
open. Edits made during a save remain dirty. Application exit disposes the windows
and shuts down the I/O executor without calling `System.exit`.

## Building and Testing

### Prerequisites
- Java 21+ JDK
- (Optional) `dpkg-deb` for building `.deb` packages

### Run Unit Tests
```bash
./gradlew test
```

### Build Fat JAR
```bash
./gradlew jar
# Run directly:
java -jar build/libs/amade-pad-0.1.0.jar
```

### Run with Gradle
```bash
./gradlew run
# Open files on startup:
./gradlew run --args="file1.txt file2.txt"
```

### Build Debian Package
```bash
./gradlew deb
# Package created at: build/distributions/amade-pad_0.1.0_all.deb
```

### Install into amadeOS Live Build
```bash
./gradlew installToAmadeOs
# Copies the .deb into ../amadeos/config/packages.chroot/
```

---

## License

MIT License. Copyright &copy; 2026 datazuul.com.

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

MIT License. Copyright &copy; 2026 datazuul.org.

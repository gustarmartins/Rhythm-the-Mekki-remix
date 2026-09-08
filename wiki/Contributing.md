# Contributing to Rhythm

Thank you for your interest in contributing to Rhythm Music Player! This guide will help you get started.

---

## 🤝 Ways to Contribute

### 1. Report Bugs

Found a bug? Help us fix it!

**Before Reporting:**
- Check [existing issues](https://github.com/cromaguy/Rhythm/issues) to avoid duplicates
- Try the latest version to see if it's already fixed
- Gather information about your device and app version

**How to Report:**
1. Go to [GitHub Issues](https://github.com/cromaguy/Rhythm/issues/new)
2. Choose "Bug Report" template
3. Fill in all required information:
   - Android version (e.g., Android 14)
   - Device model (e.g., Pixel 7)
    - Rhythm version (e.g., v5.4.456.1196 Beta)
   - Steps to reproduce
   - Expected vs actual behavior
   - Screenshots/logs if applicable

### 2. Suggest Features

Have an idea? We'd love to hear it!

**Before Suggesting:**
- Check [existing feature requests](https://github.com/cromaguy/Rhythm/issues?q=is%3Aissue+label%3Aenhancement)
- Search [Discussions](https://github.com/cromaguy/Rhythm/discussions) for similar ideas
- Consider if it aligns with Rhythm's philosophy (privacy, FOSS, simplicity)

**How to Suggest:**
1. Open a [new issue](https://github.com/cromaguy/Rhythm/issues/new) with "Feature Request" label
2. Describe:
   - What feature you want
   - Why it's useful
   - How it should work
   - Potential implementation ideas (optional)

### 3. Improve Documentation

Documentation is crucial for user experience!

**Documentation Needs:**
- Fix typos and grammar errors
- Clarify confusing sections
- Add missing information
- Update outdated content
- Translate to other languages

**How to Contribute:**
1. Fork the repository
2. Edit `.md` files in `/wiki/` or `/docs/`
3. Submit a pull request
4. Include clear description of changes

### 4. Code Contributions

Ready to code? Here's how to start!

**What We Need:**
- Bug fixes
- Performance improvements
- UI/UX enhancements
- New feature implementations
- Code refactoring
- Test coverage

**Before Coding:**
1. Check [open issues](https://github.com/cromaguy/Rhythm/issues) for tasks
2. Comment on issue to claim it
3. Discuss implementation approach if needed
4. Fork the repository

---

## 🔧 Development Setup

### Prerequisites

- **Android Studio**: Latest stable version (Ladybug or newer)
- **JDK**: 21 or higher (required for Gradle 9.x)
- **Kotlin**: 2.4.x (bundled with Android Studio)
- **Git**: For version control
- **Android Device/Emulator**: API 26+ for testing

### Clone Repository

```bash
git clone https://github.com/cromaguy/Rhythm.git
cd Rhythm
```

### Build Project

1. Open Android Studio
2. Select **File → Open** and choose Rhythm folder
3. Wait for Gradle sync to complete
4. Click **Build → Make Project** (or Ctrl+F9)

### Run on Device

1. Connect Android device with USB debugging enabled
2. Or start Android emulator (API 26+)
3. Click **Run → Run 'app'** (or Shift+F10)

---

## 📝 Code Guidelines

### Kotlin Style

Follow [official Kotlin conventions](https://kotlinlang.org/docs/coding-conventions.html):

```kotlin
// Good
class MusicPlayer {
    fun playTrack(song: Song) {
        // Implementation
    }
}

// Bad
class musicPlayer {
    fun PlayTrack(Song: Song) {
        // Implementation
    }
}
```

### Jetpack Compose

- Use `@Composable` functions for UI
- Prefer `remember` and `rememberSaveable` for state
- Extract reusable components
- Follow Material 3 guidelines

```kotlin
@Composable
fun SongItem(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
    ) {
        // UI implementation
    }
}
```

### Architecture

- **MVVM Pattern**: ViewModels for business logic
- **Repository Pattern**: Data access abstraction
- **Dependency Injection**: Manual DI
- **Clean Architecture**: Separation of concerns

### File Organization

```
app/src/main/java/chromahub/rhythm/app/
├── core/
│   └── domain/           # Domain interfaces and models
├── features/
│   ├── local/            # Local playback features
│   │   ├── data/         # Data layer
│   │   ├── domain/       # Business logic
│   │   └── presentation/ # UI components
│   └── streaming/        # Streaming playback features
│       ├── data/         # Network and data layer
│       ├── domain/       # Business logic
│       ├── di/           # Streaming DI modules
│       └── presentation/ # UI components (GoSettings, service setup, discovery)
├── shared/               # Shared utilities, themes, navigation, settings screens
├── infrastructure/       # Audio processors, player engine, service, widgets, workers, network
└── util/ & utils/        # General utilities and extension functions
```

### Comments

- Write self-documenting code
- Add comments for complex logic
- Use KDoc for public APIs

```kotlin
/**
 * Plays the specified song and updates playback state.
 *
 * @param song The song to play
 * @param startPosition Position to start playback (milliseconds)
 */
fun playSong(song: Song, startPosition: Long = 0L) {
    // Implementation
}
```

---

## 🧪 Testing

### Write Tests

- Unit tests for ViewModels and utilities
- UI tests for critical user flows
- Integration tests for data layer

```kotlin
@Test
fun `test playback state updates correctly`() {
    val player = ExoPlayer.Builder(context).build()
    player.prepare()
    player.play()
    assertThat(player.playbackState).isEqualTo(Player.STATE_READY)
}
```

### Run Tests

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

---

## 📤 Pull Request Process

### 1. Create Feature Branch

```bash
git checkout -b feature/your-feature-name
# or
git checkout -b fix/bug-description
```

### 2. Make Changes

- Write clean, documented code
- Follow existing code style
- Keep commits focused and atomic
- Write descriptive commit messages

```bash
git add .
git commit -m "feat: add lyrics sync adjustment controls"
# or
git commit -m "fix: resolve crash on empty playlist"
```

### 3. Push to Fork

```bash
git push origin feature/your-feature-name
```

### 4. Open Pull Request

1. Go to [Rhythm repository](https://github.com/cromaguy/Rhythm)
2. Click **Pull Requests → New Pull Request**
3. Select your fork and branch
4. Fill in PR template:
   - **Title**: Clear, descriptive summary
   - **Description**: What, why, and how
   - **Related Issues**: Link with `Fixes #123`
   - **Screenshots**: For UI changes
   - **Testing**: How you tested changes

### 5. Code Review

- Respond to feedback promptly
- Make requested changes
- Push updates to same branch
- Be patient and respectful

### 6. Merge

Once approved:
- Maintainer will merge your PR
- Your contribution will be in next release!
- You'll be credited in changelog

---

## 🎨 UI/UX Contributions

### Design Principles

- **Material 3**: Follow Material Design guidelines
- **Consistency**: Match existing UI patterns
- **Accessibility**: Support screen readers, high contrast
- **Performance**: Smooth animations, no jank
- **Simplicity**: Intuitive, not cluttered

### UI Guidelines

- Use theme colors, not hardcoded values
- Implement both light and dark modes
- Test on different screen sizes
- Support landscape orientation
- Add haptic feedback for interactions

---

## 🌍 Translation

### Add New Language

1. Fork repository
2. Create new language resource folder:
   ```
   app/src/main/res/values-{language_code}/
   ```
3. Copy `strings.xml` from `values/`
4. Translate all strings
5. Submit pull request

### Update Translation

1. Check existing translation for missing/outdated strings
2. Update `values-{language_code}/strings.xml`
3. Submit pull request

**Translation Guidelines:**
- Maintain original meaning
- Use formal/informal tone appropriately
- Keep placeholders intact (`%s`, `%d`, etc.)
- Test UI to ensure text fits

---

## 📋 Commit Message Convention

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code formatting (no logic change)
- `refactor`: Code restructuring
- `perf`: Performance improvement
- `test`: Adding/updating tests
- `chore`: Maintenance tasks

**Examples:**
```
feat(player): add playback speed control

fix(lyrics): resolve sync offset not saving

docs(readme): update installation instructions

refactor(viewmodel): simplify state management
```

---

## 🔍 Code Review Checklist

Before submitting PR, verify:

- [ ] Code builds without errors
- [ ] No new compiler warnings
- [ ] Follows Kotlin code style
- [ ] UI matches Material 3 guidelines
- [ ] Works on API 26+ (compileSdk 37)
- [ ] Tested on physical device
- [ ] No hardcoded strings (use resources)
- [ ] Added/updated documentation
- [ ] Wrote/updated tests (if applicable)
- [ ] No merge conflicts

---

## 🤔 Questions?

**Get Help:**
- [Telegram Community](https://t.me/RhythmSupport) - Live chat
- [Discord Server](https://discord.gg/XjPyUYPQYc) - Community chat
- [GitHub Discussions](https://github.com/cromaguy/Rhythm/discussions) - Q&A
- [GitHub Issues](https://github.com/cromaguy/Rhythm/issues) - Specific issues

**Be Respectful:**
- Follow [Code of Conduct](https://github.com/cromaguy/Rhythm/blob/main/docs/CODE_OF_CONDUCT.md)
- Be patient with responses
- Help others when you can
- Respect maintainers' decisions

---

## 🏆 Recognition

Contributors are recognized in:
- [README.md](https://github.com/cromaguy/Rhythm#contributors) Credits section
- [CHANGELOG.md](https://github.com/cromaguy/Rhythm/blob/main/docs/CHANGELOG.md) for each release
- GitHub [Contributors page](https://github.com/cromaguy/Rhythm/graphs/contributors)
- App's "About" section (major contributors)

---

## 📄 License

By contributing, you agree that your contributions will be licensed under the [GPL-3.0 License](https://github.com/cromaguy/Rhythm/blob/main/docs/LICENSE).

---

**Thank you for contributing to Rhythm! 🎵**

Your contributions help make Rhythm better for everyone. Whether it's code, documentation, translations, or bug reports, every contribution matters!

---

**Quick Links:**
- [GitHub Repository](https://github.com/cromaguy/Rhythm)
- [Issue Tracker](https://github.com/cromaguy/Rhythm/issues)
- [Discussions](https://github.com/cromaguy/Rhythm/discussions)
- [Code of Conduct](https://github.com/cromaguy/Rhythm/blob/main/docs/CODE_OF_CONDUCT.md)
- [Wiki Home](https://github.com/cromaguy/Rhythm/wiki)

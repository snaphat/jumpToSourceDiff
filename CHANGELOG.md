<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# jumpToSourceDiff Changelog
## [0.0.3]
### Fixed
- Resolved shortcut conflict where `JumpToSourceDiffAction` could be overridden by EditSource-related actions like `Frontend.EditSource` and `RiderEditSource`.
- Implemented `ActionPromoter` to ensure correct action prioritization without modifying keymap bindings.
- Prevents infinite recursion during keymap access at startup (IJPL-5324).
## [0.0.2]
### Added
- Support for IntelliJ >=2024.2
## [0.0.1]
### Added
- Plugin implementation
## [Unreleased]
### Added
- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)

# Key files

*
See [IntelliJ docs](https://plugins.jetbrains.com/docs/intellij/creating-plugin-project.html#components-of-a-wizard-generated-gradle-intellij-platform-plugin)
for the standard files
* `src/main/.../filters/` used to match different `PsiElement`s - See `AbsPsiFilter` docs
* `src/main/.../settings/` contains settings persistence and UI
* `src/main/.../handlers/` contains IntelliJ navigation implementation
* `example/` project that can be opened by a test IDE instance

# IDE integration points

* `AppSettings` and `ProjectSettings` are services used to persist the configuration (rules)
* `UI` renders the settings page for them
* `XmlToJavaGotoHandler` handles IDE navigation requests by applying the filters in the rules

# Key files

* See [IntelliJ docs][1] for the standard plug-in files
* `src/main/.../filters/` used to match different `PsiElement`s - See `Primary` docs
* `src/main/.../settings/` provides settings persistence and UI
  * `NavigationRule` is the rule model which controls the behaviour of this plug-in
* `src/main/.../handlers/` contains IntelliJ navigation implementation
* `example/` project that can be opened by a test IDE instance

# IDE integration points

* `AppSettings` and `ProjectSettings` are services used to persist the configuration (rules)
* `UI` renders the settings page for them
* `XmlToJavaGotoHandler` handles IDE navigation requests by applying the filters in the rules
* `GotoHandler` handles IDE navigation requests by applying the filters in the rules

# Filters

They pick out the source/destination of a navigation or usage contribution.
The Primary filters determine the symbol/text of interest.
The Secondary ones filter the results from above based on other attributes like file path.


[1]: https://plugins.jetbrains.com/docs/intellij/creating-plugin-project.html#components-of-a-wizard-generated-gradle-intellij-platform-plugin
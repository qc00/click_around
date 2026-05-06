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
* `GotoHandler` handles IDE navigation requests by applying the filters in the rules

# Filters

They pick out the source/destination of a navigation or usage contribution.
The Primary filters determine the symbol/text of interest.
The Secondary ones filter the results from above based on other attributes like file path.

# XML first for configuration

After trying various model binding methods (`com.intellij.util.xmlb`, `DomElement`) and rule editing UI designs,
I realised nothing beats allowing the user to write the config XML directly, with these benefits:

* No serialisation needed
* IntelliJ already has a powerful XML editor with auto-complete, contextual help and validation display capabilities
* Free rule duplication, import and export

Implementation:

* jaxb2-maven-plugin is used to generate the XSD
* The `*Settings` services (which are persisted by XMLB) marshal the config XML as a string
* JAXB deserializes `NavigationRule`s from the string
* A `LanguageTextField` is used to edit the string in the Settings `UI` as validated by the XSD

[1]: https://plugins.jetbrains.com/docs/intellij/creating-plugin-project.html#components-of-a-wizard-generated-gradle-intellij-platform-plugin
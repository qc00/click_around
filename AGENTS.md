* When writing code to modify a UI component or call IntelliJ's API, double check if such logic needs to be on a "write
  thread".
    * Use the "find_{lock/threading}_requirements_usages" tools if unsure.
    * You can assume `ApplicationManager.getApplication().runWriteAction()` is sufficient.
* Don't blindly trust online guides, verify actual usage in IntelliJ's source code. I have it checked out at
    `/Volumes/References/intellij-community`
    * This folder is not indexed, so file search tools might not work. Try `rg` instead.
* Sometimes, reading the source or even running tests against a mock IDE (like in `SettingsIT`) suffers from
  confirmation bias.
  To verify a major UI interaction works, you must also write tests similar to `IdeDrivingIT`.
    * If you can't get such a test to work after a few tries, start a pair debug session (see below).

* Do not add JUnit 4 dependency!!

* Run `./gradlew unitTest` for plain JVM unit tests with JaCoCo coverage
* To see the generated XML Schema (used for validation), run `./gradlew schemagen`. The result goes to
  `build/generated-resources/schemagen/schema1.xsd`

## Pair debugging

1. Review existing breakpoints and ask the user if they're leftover from a previous pair debug session
2. Use tool to start debug session of `.run/Run Plugin.run.xml`. (Launching directly via command line/gradle won't
   work.)
3. Set breakpoints, e.g. to confirm your expectations or to figure out how the IDE actually works
4. Use the `ask_questions` tool to ask the user to perform action/report observations
5. Repeat
6. Stop the debug session before making code changes to prevent any code hot-swap dialog
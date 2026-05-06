* Avoid inline comments unless the logic is confusing or not well explained by the method/variable name
* Be very careful with IntelliJ's threading model
* Don't blindly trust online guides, verify actual usage in IntelliJ's source code. I have it checked out at
    `/Volumes/References/intellij-community`

* Run `./gradlew unitTest` for plain JVM unit tests with JaCoCo coverage
* To see the generated XML Schema (used for validation), run `./gradlew schemagen`. The result goes to
  `build/generated-resources/schemagen/schema1.xsd`
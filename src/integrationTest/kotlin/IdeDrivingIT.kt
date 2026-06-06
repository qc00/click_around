import com.intellij.driver.sdk.openFile
import com.intellij.driver.sdk.ui.components.common.editorTabs
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.ci.CIServer
import com.intellij.ide.starter.ci.NoCIServer
import com.intellij.ide.starter.di.di
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IdeProductProvider
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.plugins.PluginConfigurator
import com.intellij.ide.starter.project.LocalProjectInfo
import com.intellij.ide.starter.runner.Starter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import java.nio.file.Path
import kotlin.time.Duration.Companion.minutes

/**
 * Integration test that launches a full IDE instance with the plugin installed
 * and drives it using the JetBrains Driver framework (RPA-style UI testing).
 *
 * ## How it works
 * - The Starter framework downloads/locates an IntelliJ IDE, installs the plugin,
 *   and launches the IDE in a separate process.
 * - The Driver framework connects to the running IDE via JMX and provides a Kotlin DSL
 *   for interacting with Swing components (clicks, keyboard, assertions).
 * - Tests run in the *test process*; commands are sent to the *IDE process*.
 *
 * ## Key APIs for writing similar tests
 * ### Project sources
 * - `NoProject` — opens the welcome screen, no project
 * - `LocalProjectInfo(Path("..."))` — opens a local directory as project
 *
 * ### Waiting for IDE readiness
 * ```kotlin
 * waitForIndicators(5.minutes)  // waits for indexing, background tasks, etc.
 * ```
 *
 * ### Finding UI components (XPath-based, like Selenium)
 * ```kotlin
 * ideFrame {
 *     // Find by visible text, accessible name, Java class, or combine:
 *     x(xQuery { byVisibleText("Some Label") }).click()
 *     x(xQuery { byAccessibleName("My Button") }).shouldBe("visible", present)
 *     x(xQuery { and(byVisibleText("X"), byJavaClass("javax.swing.JButton")) })
 * }
 * ```
 *
 * ### Opening files
 * ```kotlin
 * openFile("src/main/java/Foo.java")  // relative to project root
 * ```
 *
 * ### Keyboard interaction
 * ```kotlin
 * keyboard {
 *     enterText("hello")
 *     enter()
 *     hotKey(KeyEvent.VK_META, KeyEvent.VK_A)  // Cmd+A on macOS
 *     backspace()
 * }
 * ```
 *
 * ### Asserting editor tabs
 * ```kotlin
 * // Prefer the typed EditorTabs API — avoids duplicate-match issues
 * // ('rules.xml' may match both the tab label and breadcrumb).
 * ideFrame {
 *     editorTabs {
 *         Assertions.assertTrue(isTabOpened("rules.xml"))
 *         clickTab("rules.xml")
 *     }
 * }
 * ```
 *
 * ### Inspecting the live component tree
 * Pause the IDE with `Thread.sleep(30.minutes.inWholeMilliseconds)` and open
 * the URL printed in logs to browse the Swing tree in your browser.
 */
class IdeDrivingIT {

    init {
        // Override the default CI reporter so IDE-side exceptions fail the test
        di = DI.Companion {
            extend(di)
            bindSingleton<CIServer>(overrides = true) {
                object : CIServer by NoCIServer {
                    override fun reportTestFailure(
                        testName: String,
                        message: String,
                        details: String,
                        linkToLogs: String?,
                    ) {
                        fail { "$testName fails: $message.\n$details" }
                    }
                }
            }
        }
    }

    /**
     * Opens the `example` project with the plugin installed, opens `rules.xml`,
     * and asserts that the editor tab is visible.
     */
    @Test
    fun openExampleProjectAndVerifyRulesXmlTab() {
        Starter.newContext(
            "openExampleProjectAndVerifyRulesXmlTab",
            TestCase(
                IdeProductProvider.IU,
                LocalProjectInfo(Path.of("example")),
            ).withVersion(System.getProperty("platform.version")),
        ).apply {
            val pluginPath = Path.of(System.getProperty("path.to.build.plugin"))
            PluginConfigurator(this).installPluginFromPath(pluginPath)
        }.runIdeWithDriver().useDriverAndCloseIde {
            waitForIndicators(5.minutes)

            openFile("src/rules.xml")

            // Assert the editor tab labelled "rules.xml" is present.
            // Use the EditorTabs SDK component to avoid duplicate-match issues
            // (e.g. breadcrumb also shows "rules.xml").
            ideFrame {
                editorTabs {
                    Assertions.assertTrue(
                        isTabOpened("rules.xml"),
                        "Editor tab 'rules.xml' should be visible, open tabs: ${getTabs().map { it.text }}"
                    )
                }
            }
        }
    }
}
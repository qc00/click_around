package software.nmr.click_around.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.junit5.RunInEdt;
import com.intellij.testFramework.junit5.TestApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@TestApplication
@RunInEdt(writeIntent = true)
class SettingsIT {
    private static final String VALID_XML = """
            <rules>
                <rule type="TO_DEFINITION">
                    <definition><JavaAnnotation attr="value" fqn="com.x.Y"/></definition>
                    <usage><Xml attr="" tag="tag"><ns>ns</ns></Xml></usage>
                </rule>
            </rules>""";

    private static final String INVALID_XML = """
            <rules>
                <rule type="TO_DEFINITION"/>
            </rules>""";

    private CodeInsightTestFixture fixture;
    private UI ui;

    @BeforeEach
    void setUp() throws Exception {
        var factory = IdeaTestFixtureFactory.getFixtureFactory();
        var builder = factory.createLightFixtureBuilder(null, "UIIdeTest");
        fixture = factory.createCodeInsightFixture(builder.getFixture());
        fixture.setUp();
        AppSettings.testOverride = new AppSettings();
    }

    @AfterEach
    void tearDown() throws Exception {
        try {
            if (ui != null) {
                ui.disposeUIResources();
                ui = null;
            }
            AppSettings.testOverride = null;
        } finally {
            fixture.tearDown();
        }
    }

    @Test
    void createComponentInsideReadActionDoesNotWrite() {
        ui = new UI(fixture.getProject());
        ApplicationManager.getApplication().runReadAction((Runnable) ui::createComponent);
        assertNotNull(ui.widget);
    }

    @Test
    void projectConfigurableUsesGivenProject() {
        ui = new UI(fixture.getProject());
        ui.createComponent();
        assertNotNull(ui.project);
        assertFalse(ui.project.isDefault(), "project configurable must not use the default project");
    }

    @Test
    void appConfigurableResolvesOpenProject() {
        ui = new UI();
        assertNull(ui.project);
        ui.createComponent();
        assertNotNull(ui.widget);
    }

    @Test
    void editorUsesSchemaWithoutXsiSchemaLocation() {
        ui = createUi();
        ui.widget.setText(VALID_XML);
        var project = fixture.getProject();
        var psiManager = PsiDocumentManager.getInstance(project);
        psiManager.commitDocument(ui.widget.getDocument());
        var psiFile = psiManager.getPsiFile(ui.widget.getDocument());
        assertInstanceOf(XmlFile.class, psiFile);
        var file = (XmlFile) psiFile;
        var root = file.getRootTag();
        assertNotNull(root);
        var rule = root.findFirstSubTag("rule");
        assertNotNull(rule);
        assertNotNull(root.getDescriptor());
        assertNotNull(root.getDescriptor().getElementDescriptor(rule, root));
    }

    @Test
    void settingsDocumentationFindsSchemaDocumentation() {
        ui = createUi();
        ui.widget.setText(VALID_XML);
        var editor = ui.widget.getEditor(true);
        assertNotNull(editor);

        editor.getCaretModel().moveToOffset(ui.widget.getText().indexOf("TO_DEFINITION"));
        var html = SettingsDocumentation.documentationHtml(editor);

        assertNotNull(html);
        assertTrue(html.contains("One way jump from usage to definition"), html);
    }

    @Test
    void settingsDocumentationRendersSimpleSchemaHtml() {
        var rendered = SettingsDocumentation.renderHtml(
                "before<br>\nline &lt;p&gt;para&lt;/p&gt; &lt;em&gt;em&lt;/em&gt; {@code value} &lt;br&gt;");

        assertFalse(rendered.contains("before<br>"), rendered);
        assertTrue(rendered.contains("<p>para</p>"), rendered);
        assertTrue(rendered.contains("<em>em</em>"), rendered);
        assertTrue(rendered.contains("<code>value</code>"), rendered);
        assertTrue(rendered.contains("<br>"), rendered);
        assertTrue(rendered.startsWith("<html><body>"), rendered);
    }

    @Test
    void settingsSchemaExposesNoNamespaceForMarkedFiles() {
        var file = fixture.addFileToProject("settings.xml", "<rules/>");
        SettingsSchema.markAsSettingsFile(file);
        fixture.configureFromExistingVirtualFile(file.getVirtualFile());
        assertRootHasDescriptor();

        assertEquals(Set.of(""), new SettingsSchema().getAvailableNamespaces((XmlFile) fixture.getFile(), null));
    }

    @Test
    void settingsSchemaMarkerSurvivesPsiCopiesUsedByCompletion() {
        var file = fixture.addFileToProject("settings-copy.xml", "<rules/>");
        SettingsSchema.markAsSettingsFile(file);

        assertTrue(new SettingsSchema().isAvailable((XmlFile) file.copy()));
    }

    @Test
    void applyRejectsInvalidRules() {
        ui = createUi();
        ui.widget.setText(INVALID_XML);
        assertThrows(ConfigurationException.class, () -> ui.apply());
        assertTrue(ui.isModified());
    }

    @Test
    void applyAcceptsValidRules() throws ConfigurationException {
        ui = createUi();
        ui.widget.setText(VALID_XML);
        ui.apply();
        assertFalse(ui.isModified());
    }

    @Test
    void resetRestoresOriginalText() {
        ui = createUi();
        var original = ui.widget.getText();
        ui.widget.setText(VALID_XML);
        assertTrue(ui.isModified());
        ui.reset();
        assertFalse(ui.isModified());
        assertEquals(original, ui.widget.getText());
    }

    @Test
    void disposeUIResourcesCleansUp() {
        ui = createUi();
        assertNotNull(ui.widget);
        ui.disposeUIResources();
        assertNull(ui.widget);
        assertFalse(ui.isModified());
        ui = null;
    }

    private UI createUi() {
        var created = new UI(fixture.getProject());
        created.createComponent();
        return created;
    }

    private void assertRootHasDescriptor() {
        assertInstanceOf(XmlFile.class, fixture.getFile());
        var root = ((XmlFile) fixture.getFile()).getRootTag();
        assertNotNull(root);
        assertNotNull(root.getDescriptor());
    }

}

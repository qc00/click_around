package software.nmr.click_around.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.concurrent.atomic.AtomicReference;

public class UIIdeTest extends BasePlatformTestCase {
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

    private UI ui;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        AppSettings.testOverride = new AppSettings();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            if (ui != null) {
                try {
                    onEdt(() -> ui.disposeUIResources());
                } catch (Throwable t) {
                    if (t instanceof Exception e) throw e;
                    if (t instanceof Error e) throw e;
                    throw new RuntimeException(t);
                }
                ui = null;
            }
            AppSettings.testOverride = null;
        } finally {
            super.tearDown();
        }
    }

    public void testCreateComponentInsideReadActionDoesNotWrite() throws Throwable {
        ui = new UI(getProject());

        onEdt(() -> ApplicationManager.getApplication().runReadAction((Runnable) ui::createComponent));
        onEdt(PlatformTestUtil::dispatchAllEventsInIdeEventQueue);
        assertNotNull(ui.widget);
    }

    public void testEditorUsesSchemaWithoutXsiSchemaLocation() throws Throwable {
        ui = createUi();

        onEdt(() -> {
            ui.widget.setText(VALID_XML);
            var psiManager = PsiDocumentManager.getInstance(ui.project);
            psiManager.commitDocument(ui.widget.getDocument());
            var psiFile = psiManager.getPsiFile(ui.widget.getDocument());
            assertTrue(psiFile instanceof XmlFile);
            var file = (XmlFile) psiFile;
            var root = file.getRootTag();
            assertNotNull(root);
            var rule = root.findFirstSubTag("rule");
            assertNotNull(rule);
            assertNotNull(root.getDescriptor());
            assertNotNull(root.getDescriptor().getElementDescriptor(rule, root));
        });
    }

    public void testApplyRejectsInvalidRules() throws Throwable {
        ui = createUi();

        onEdt(() -> {
            ui.widget.setText(INVALID_XML);
            assertApplyFails(ui);
            assertTrue(ui.isModified());
        });
    }

    public void testApplyAcceptsValidRules() throws Throwable {
        ui = createUi();

        onEdt(() -> {
            ui.widget.setText(VALID_XML);
            ui.apply();
        });

        assertFalse(ui.isModified());
        assertEquals(1, ProjectSettings.getInstance(getProject()).rules.size());
    }

    private UI createUi() throws Throwable {
        var result = new AtomicReference<UI>();
        onEdt(() -> {
            var created = new UI(getProject());
            created.createComponent();
            result.set(created);
        });
        onEdt(PlatformTestUtil::dispatchAllEventsInIdeEventQueue);
        return result.get();
    }

    private static void assertApplyFails(UI ui) throws ConfigurationException {
        try {
            ui.apply();
            fail("Expected invalid rules to be rejected");
        } catch (ConfigurationException expected) {
        }
    }

    private static void onEdt(ThrowingRunnable runnable) throws Throwable {
        var thrown = new AtomicReference<Throwable>();
        ApplicationManager.getApplication().invokeAndWait(() -> {
            try {
                runnable.run();
            } catch (Throwable t) {
                thrown.set(t);
            }
        }, ModalityState.any());
        if (thrown.get() != null) throw thrown.get();
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Throwable;
    }
}

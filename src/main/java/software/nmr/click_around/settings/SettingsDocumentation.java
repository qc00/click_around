package software.nmr.click_around.settings;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.ide.BrowserUtil;
import com.intellij.lang.LanguageDocumentation;
import com.intellij.lang.documentation.CompositeDocumentationProvider;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.editor.impl.EditorMouseHoverPopupControl;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.Alarm;
import com.intellij.util.Alarm.ThreadToUse;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.xml.util.documentation.XmlDocumentationProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.util.concurrent.ExecutorService;

/**
 * Mostly replacement of the built-in doc display logic which doesn't work in a modal dialogue.
 */
public class SettingsDocumentation implements EditorMouseMotionListener {

    private static final ExecutorService BACKGROUND = AppExecutorUtil.getAppExecutorService();
    static XmlDocumentationProvider xmlDocProvider;

    private final Editor editor;
    private final Alarm alarm;
    private JBPopup popup;
    private int requestedOffset = -1;

    public SettingsDocumentation(Editor editor, Disposable parentDisposable) {
        this.editor = editor;
        alarm = new Alarm(ThreadToUse.POOLED_THREAD, parentDisposable);

        if (getDocProvider() == null) return; // Don't register listeners

        // Override keyboard short-cut
        var quickDoc = ActionManager.getInstance().getAction(IdeActions.ACTION_QUICK_JAVADOC);
        var shortcutSet = quickDoc == null ? CustomShortcutSet.fromString("F1") : quickDoc.getShortcutSet();
        this.new DocumentationAction().registerCustomShortcutSet(
                shortcutSet,
                editor.getContentComponent(),
                parentDisposable
        );

        // Mouse hover
        EditorMouseHoverPopupControl.disablePopups(editor);
        Disposer.register(parentDisposable, () -> EditorMouseHoverPopupControl.enablePopups(editor));
        editor.addEditorMouseMotionListener(this, parentDisposable);
    }

    private static XmlDocumentationProvider getDocProvider() {
        if (xmlDocProvider == null) {
            var docProvider = LanguageDocumentation.INSTANCE.forLanguage(XMLLanguage.INSTANCE);
            if (docProvider instanceof CompositeDocumentationProvider composite) {
                for (var provider: composite.getProviders()) {
                    if (provider instanceof XmlDocumentationProvider xml) {
                        xmlDocProvider = xml;
                        break;
                    }
                }
            } else if (docProvider instanceof XmlDocumentationProvider xml) {
                xmlDocProvider = xml;
            }
        }
        return xmlDocProvider;
    }

    private void show() {
        if (editor.isDisposed() || requestedOffset < 0) return;
        if (popup != null) popup.cancel();

        ReadAction.nonBlocking(() -> documentationHtml(editor, requestedOffset))
                  .finishOnUiThread(ModalityState.any(), html -> popup = showPopUp(editor, html))
                  .submit(BACKGROUND);
    }

    // All logic that should not run on the UI thread
    @VisibleForTesting
    static @Nullable String documentationHtml(Editor editor, int offset) {
        var project = editor.getProject();
        if (project == null || project.isDisposed()) return null;
        var psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
        if (psiFile == null) return null;

        // Simplified logic of IdeDocumentationTargetProvider:
        var targetAndSource = LegacyDocumentationTargetFinder.getTarget(project, editor, offset, psiFile);
        if (targetAndSource == null) return null;
        // Logic of PsiElementDocumentationTarget:
        return xmlDocProvider.generateDoc(targetAndSource.first, targetAndSource.first);
    }

    // Can only contain UI drawing logic
    private static JBPopup showPopUp(Editor editor, String html) {
        if (html == null || html.isBlank()) return null;

        var pane = new JEditorPane("text/html", renderHtml(html));
        pane.setEditable(false);
        pane.setBorder(JBUI.Borders.empty(8));
        pane.addHyperlinkListener(event -> {
            if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED && event.getURL() != null) {
                BrowserUtil.browse(event.getURL());
            }
        });

        var scrollPane = new JBScrollPane(pane);
        var popup = JBPopupFactory.getInstance()
                                  .createComponentPopupBuilder(scrollPane, pane)
                                  .setProject(editor.getProject())
                                  .setResizable(true)
                                  .setMovable(true)
                                  .setFocusable(true)
                                  .setRequestFocus(false)
                                  .setCancelOnClickOutside(true)
                                  .setCancelOnWindowDeactivation(true)
                                  .setModalContext(false)
                                  .createPopup();
        popup.showInBestPositionFor(editor);
        return popup;
    }

    static String renderHtml(String html) {
        var rendered = html.replaceAll("(?i)<br>\\R\\s*", "\n")
                           .replaceAll("&lt;(/?(?:p|em|strong|b|i|code|pre|ul|ol|li))&gt;", "<$1>")
                           .replaceAll("&lt;(br\\s*/?)&gt;", "<$1>")
                           .replaceAll("\\{@code\\s+([^}]+)}", "<code>$1</code>")
                           .replaceAll("\\{@link\\s+([^}]+)}", "<code>$1</code>");
        return rendered.regionMatches(true, 0, "<html", 0, "<html".length())
                ? rendered
                : "<html><body>" + rendered + "</body></html>";
    }

    private class DocumentationAction extends DumbAwareAction {

        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            cancelPending();
            requestedOffset = editor.getCaretModel().getOffset();
            show();
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

    }

    @Override
    public void mouseMoved(@NotNull EditorMouseEvent event) {
        if (!event.isOverText()) {
            cancelPending();
            return;
        }

        var offset = event.getOffset();
        if (offset == requestedOffset && popup != null && popup.isVisible()) return;

        cancelPending();
        requestedOffset = offset;
        alarm.addRequest(this::show, CodeInsightSettings.getInstance().JAVADOC_INFO_DELAY);
    }

    private void cancelPending() {
        requestedOffset = -1;
        alarm.cancelAllRequests();
        if (popup != null && popup.isVisible()) popup.cancel();
        popup = null;
    }
}

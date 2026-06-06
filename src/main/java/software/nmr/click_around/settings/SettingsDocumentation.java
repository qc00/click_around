package software.nmr.click_around.settings;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.ide.BrowserUtil;
import com.intellij.lang.documentation.ide.IdeDocumentationTargetProvider;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.editor.impl.EditorMouseHoverPopupControl;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.platform.backend.documentation.DocumentationData;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.Alarm;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;

/**
 * Mostly vibe-coded replacement of the built-in doc display logic which doesn't work in a modal dialogue.
 */
@SuppressWarnings({"UnstableApiUsage", "OverrideOnly", "TestOnlyProblems"})
public class SettingsDocumentation {

    public static <E extends Editor> E install(E editor, Disposable parentDisposable) {
        var quickDoc = ActionManager.getInstance().getAction(IdeActions.ACTION_QUICK_JAVADOC);
        var shortcutSet = quickDoc == null ? CustomShortcutSet.fromString("F1") : quickDoc.getShortcutSet();
        new DocumentationAction(editor).registerCustomShortcutSet(
                shortcutSet,
                editor.getContentComponent(),
                parentDisposable
        );

        EditorMouseHoverPopupControl.disablePopups(editor);
        Disposer.register(parentDisposable, () -> EditorMouseHoverPopupControl.enablePopups(editor));
        editor.addEditorMouseMotionListener(new HoverDocumentation(editor, parentDisposable), parentDisposable);
        return editor;
    }

    static @Nullable String documentationHtml(Editor editor) {
        var project = editor.getProject();
        if (project == null || project.isDisposed()) return null;
        var offset = editor.getCaretModel().getOffset();

        return ReadAction.compute(() -> {
            if (project.isDisposed()) return null;
            var psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
            if (psiFile == null) return null;
            var targets = IdeDocumentationTargetProvider.getInstance(project)
                                                        .documentationTargets(editor, psiFile, offset);
            for (var target: targets) {
                var result = target.computeDocumentation();
                if (result instanceof DocumentationData data) return data.getHtml();
            }
            return null;
        });
    }

    private static JBPopup show(Editor editor) {
        var html = documentationHtml(editor);
        if (html == null || html.isBlank()) html = "No documentation found.";

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

    private static final class DocumentationAction extends DumbAwareAction {
        private final Editor editor;

        private DocumentationAction(Editor editor) {
            this.editor = editor;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            show(editor);
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }
    }

    private static final class HoverDocumentation implements EditorMouseMotionListener {
        private final Editor editor;
        private final Alarm alarm;
        private JBPopup popup;
        private int requestedOffset = -1;
        private int shownOffset = -1;

        private HoverDocumentation(Editor editor, Disposable parentDisposable) {
            this.editor = editor;
            alarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, parentDisposable);
        }

        @Override
        public void mouseMoved(@NotNull EditorMouseEvent event) {
            if (!event.isOverText()) {
                cancelPending();
                return;
            }

            event.consume();
            var offset = event.getOffset();
            if (offset == shownOffset && popup != null && popup.isVisible()) return;

            cancelPending();
            requestedOffset = offset;
            alarm.addRequest(() -> {
                if (editor.isDisposed() || requestedOffset < 0) return;
                if (popup != null) popup.cancel();
                popup = show(editor);
                shownOffset = requestedOffset;
            }, CodeInsightSettings.getInstance().JAVADOC_INFO_DELAY);
        }

        private void cancelPending() {
            requestedOffset = -1;
            alarm.cancelAllRequests();
            if (popup != null && popup.isVisible()) popup.cancel();
            popup = null;
            shownOffset = -1;
        }
    }
}

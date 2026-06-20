package software.nmr.click_around.settings;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.ide.BrowserUtil;
import com.intellij.lang.documentation.ide.IdeDocumentationTargetProvider;
import com.intellij.lang.documentation.psi.PsiElementDocumentationTarget;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.editor.impl.EditorMouseHoverPopupControl;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.platform.backend.documentation.AsyncDocumentation;
import com.intellij.platform.backend.documentation.DocumentationData;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.JBUI;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.lang.reflect.Method;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Replacement of the built-in doc display logic which doesn't work in a modal dialogue.
 */
public class SettingsDocumentation implements EditorMouseMotionListener {
    private static final Logger LOG = Logger.getInstance(SettingsDocumentation.class);
    private static final ScheduledExecutorService BACKGROUND = AppExecutorUtil.getAppScheduledExecutorService();
    private static final Method DO_COMPUTE_DOCUMENTATION = getDoComputeDocumentation();

    private static Method getDoComputeDocumentation() {
        try {
            @SuppressWarnings({"TestOnlyProblems", "UnstableApiUsage"})
            var method = PsiElementDocumentationTarget.class.getDeclaredMethod("doComputeDocumentation");
            method.setAccessible(true);
            return method;
        } catch (Exception e) {
            LOG.warn("Failed to reflect doComputeDocumentation()", e);
            return null;
        }
    }

    private final Editor editor;
    private JBPopup popup;
    private int requestedOffset = -1;
    private ScheduledFuture<?> scheduled;

    public SettingsDocumentation(Editor editor, Disposable parentDisposable) {
        this.editor = editor;

        if (DO_COMPUTE_DOCUMENTATION == null) return;

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
        editor.addEditorMouseMotionListener(this, parentDisposable);

        Disposer.register(parentDisposable, this::cancelPending);
    }

    private void show() {
        if (editor.isDisposed() || requestedOffset < 0) return;
        var project = editor.getProject();
        if (project == null || project.isDisposed()) return;

        ReadAction.nonBlocking(() -> documentationHtml(editor, requestedOffset))
                  .withDocumentsCommitted(project)
                  .finishOnUiThread(ModalityState.any(), this::showPopUp)
                  .submit(BACKGROUND);
    }

    // All logic that should not run on the UI thread
    @VisibleForTesting
    @SuppressWarnings({"UnstableApiUsage", "OverrideOnly", "TestOnlyProblems"})
    static @Nullable String documentationHtml(Editor editor, int offset) throws Exception {
        var project = editor.getProject();
        if (project == null || project.isDisposed()) return null;
        var psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
        if (psiFile == null) return null;
        var documentManager = PsiDocumentManager.getInstance(project);
        if (!documentManager.isCommitted(editor.getDocument())
                && ApplicationManager.getApplication().isDispatchThread()) {
            documentManager.commitDocument(editor.getDocument());
        }

        // var targets = TargetsKt.documentationTargets(psiFile, offset);
        var targets = IdeDocumentationTargetProvider.getInstance(project)
                                                    .documentationTargets(editor, psiFile, offset);
        for (var target: targets) {
            // PsiElementDocumentationTarget.computeDocumentation() require a job which we don't have
            Object result = target instanceof PsiElementDocumentationTarget ? DO_COMPUTE_DOCUMENTATION.invoke(target)
                    : target.computeDocumentation();
            if (result instanceof AsyncDocumentation async) {
                result = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE,
                        (scope, continuation) -> async.getSupplier().invoke(continuation));
            }
            if (result instanceof DocumentationData data) return data.getHtml();
        }
        return null;
    }

    // Can only contain UI drawing logic
    private void showPopUp(String html) {
        if (popup != null && popup.isVisible()) popup.cancel();
        if (html == null || html.isBlank()) return;

        var pane = new JEditorPane("text/html", renderHtml(html));
        pane.setEditable(false);
        pane.setBorder(JBUI.Borders.empty(8));
        pane.addHyperlinkListener(event -> {
            if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED && event.getURL() != null) {
                BrowserUtil.browse(event.getURL());
            }
        });

        var scrollPane = new JBScrollPane(pane);
        popup = JBPopupFactory.getInstance()
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
        scheduled = BACKGROUND.schedule(this::show, CodeInsightSettings.getInstance().JAVADOC_INFO_DELAY,
                TimeUnit.MILLISECONDS);
    }

    private void cancelPending() {
        requestedOffset = -1;
        if (scheduled != null) {
            scheduled.cancel(false);
            scheduled = null;
        }
        if (popup != null && popup.isVisible()) popup.cancel();
        popup = null;
    }
}

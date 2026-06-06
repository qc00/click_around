package software.nmr.click_around.settings;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.LanguageTextField;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.stream.Stream;

public class UI implements Configurable, DocumentListener {

    @VisibleForTesting
    Project project;
    final AbsSettings settings;

    private final Disposable disposable = Disposer.newDisposable("Click Around settings editor");
    EditorTextField widget;
    volatile boolean changed;

    public UI() {
        settings = AppSettings.getInstance();
    }

    public UI(Project project) {
        this.project = project;
        settings = ProjectSettings.getInstance(project);
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Click Around";
    }

    @Override
    public @Nullable JComponent createComponent() {
        if (widget == null) {
            var myProj = project;
            if (myProj == null) {
                // Try to find a valid project, so the text editor log less warnings:
                var pm = ProjectManager.getInstance();
                myProj = Stream.of(pm.getOpenProjects()).filter(project -> !project.isDisposed()).findFirst()
                               .orElse(null);
            }
            widget = new LanguageTextField(XMLLanguage.INSTANCE, myProj, settings.getState().xml,
                    SettingsSchema.DOC_CREATOR, false) {
                protected @NotNull EditorEx createEditor() {
                    return SettingsDocumentation.install(super.createEditor(), disposable);
                }
            };
            widget.setDisposedWith(disposable);
            widget.addDocumentListener(this);
        }
        return widget;
    }

    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        changed = true;
    }

    @Override
    public void reset() {
        if (changed && widget != null) {
            widget.setText(settings.getState().xml);
        }
        changed = false;
    }

    @Override
    public boolean isModified() {
        return changed;
    }

    @Override
    public void apply() throws ConfigurationException {
        try {
            var xml = widget.getText();
            SettingsSchema.validate(xml);
            var exception = settings.setRules(xml);
            if (exception != null) throw new ConfigurationException(exception.getMessage(), exception, "Invalid XML");
            changed = false;
        } catch (Exception e) {
            throw new ConfigurationException(e.getMessage(), e, "Cannot Apply");
        }
    }

    @Override
    public void disposeUIResources() {
        Disposer.dispose(disposable);
        widget = null;
        changed = false;
    }
}

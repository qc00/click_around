package software.nmr.click_around.settings;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.cellvalidators.CellComponentProvider;
import com.intellij.openapi.ui.cellvalidators.CellTooltipManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.event.MouseEvent;

public class UI implements Configurable {

    private final AbsSettings<?> settings;
    private final NavigationRuleTableModel tableModel;
    private final Disposable disposable = Disposer.newDisposable();
    private JBTable table;

    public UI() {
        settings = AppSettings.getInstance();
        tableModel = new NavigationRuleTableModel(settings.rules);
    }

    public UI(Project project) {
        settings = ProjectSettings.getInstance(project);
        tableModel = new NavigationRuleTableModel(settings.rules);
    }

    @Override
    public void reset() {
        tableModel.reset(settings.rules);
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Click Around";
    }

    private void createTable() {
        table = new JBTable(tableModel) {
            @Override
            protected @NotNull JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    @Override
                    public String getToolTipText(MouseEvent event) {
                        int viewColumn = columnAtPoint(event.getPoint());
                        if (viewColumn < 0) return null;

                        int modelColumn = columnModel.getColumn(viewColumn).getModelIndex();
                        return tableModel.getColumnTooltip(modelColumn);
                    }
                };
            }
        };
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    }

    @SuppressWarnings("UnstableApiUsage")
    private void addValidation() {
        tableModel.addValidation(table);

        new CellTooltipManager(disposable)
                .withCellComponentProvider(CellComponentProvider.forTable(table))
                .installOn(table);
    }

    @Override
    public @Nullable JComponent createComponent() {
        createTable();
        addValidation();

        var mainPanel = ToolbarDecorator.createDecorator(table)
                .setAddAction(button -> tableModel.addRule())
                .setRemoveAction(button -> {
                    int selectedRow = table.getSelectedRow();
                    if (selectedRow >= 0) {
                        tableModel.removeRule(selectedRow);
                    }
                })
                .createPanel();
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        return tableModel.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {
        if (table == null) return;
        table.editingStopped(null);

        settings.rules = tableModel.getOut(); // atomic
        settings.notifyRules();
    }

    @Override
    public void disposeUIResources() {
        table = null;
        Disposer.dispose(disposable);
    }
}


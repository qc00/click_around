package software.nmr.click_around.settings;

import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.Nls;
import software.nmr.click_around.filters.AbsPsiFilter.Descriptor;
import software.nmr.click_around.filters.JavaAnnotation;
import software.nmr.click_around.filters.Xml;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NavigationRuleTableModel extends AbstractTableModel {
    private static final Descriptor XML = Xml.DESC, JAVA = JavaAnnotation.DESC;
    public static final int COL_COUNT = XML.getColumnCount() + JAVA.getColumnCount();
    private static final int JAVA_START = XML.getColumnCount();
    private static final NavigationRule INVALID = new NavigationRule();

    private final List<NavigationRule> rules;

    public NavigationRuleTableModel(Collection<NavigationRule> rules) {
        this.rules = new ArrayList<>(rules);
    }

    public void reset(Collection<NavigationRule> rules) {
        this.rules.clear();
        this.rules.addAll(rules);
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return rules.size();
    }

    @Override
    public int getColumnCount() {
        return COL_COUNT;
    }

    @Override
    public @Nls String getColumnName(int column) {
        return column < JAVA_START ? XML.getColumnName(column) : JAVA.getColumnName(column - JAVA_START);
    }

    public @Nls String getColumnTooltip(int column) {
        return column < JAVA_START ? XML.getColumnTooltip(column) : JAVA.getColumnTooltip(column - JAVA_START);
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return column < JAVA_START ? XML.getColumnClass(column) : JAVA.getColumnClass(column - JAVA_START);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        var rule = rules.get(rowIndex);
        return columnIndex < JAVA_START ? rule.from.arrayView()[columnIndex] : rule.to.arrayView()[columnIndex - JAVA_START];
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        var rule = rules.get(rowIndex);
        if (columnIndex < JAVA_START) {
            rule.from.arrayView()[columnIndex] = (String) value;
        } else {
            rule.to.arrayView()[columnIndex - JAVA_START] = (String) value;
        }
        fireTableCellUpdated(rowIndex, columnIndex);
    }

    public static boolean needValidation(int column) {
        var issue = column < JAVA_START ? INVALID.from.validateField(column) : INVALID.to.validateField(column - JAVA_START);
        return issue != null;
    }

    public ValidationInfo validate(int rowIndex, int column) {
        var rule = rules.get(rowIndex);
        return column < JAVA_START ? rule.from.validateField(column) : rule.to.validateField(column - JAVA_START);
    }

    public void addRule() {
        int row = rules.size();
        rules.add(new NavigationRule());
        fireTableRowsInserted(row, row);
    }

    public void removeRule(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < rules.size()) {
            rules.remove(rowIndex);
            fireTableRowsDeleted(rowIndex, rowIndex);
        }
    }

    /** Do not modify the collection. */
    Collection<NavigationRule> stateView() {
        return rules;
    }
}

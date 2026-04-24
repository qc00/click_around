package software.nmr.click_around.settings;

import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.Nls;

import javax.swing.table.AbstractTableModel;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static software.nmr.click_around.settings.NavigationRule.FIELD_NAMES;

public class NavigationRuleTableModel extends AbstractTableModel {
    private final List<NavigationRule> rules;
    private final Field[] fields;

    public NavigationRuleTableModel(Collection<NavigationRule> rules) {
        this.rules = new ArrayList<>(rules);

        this.fields = new Field[FIELD_NAMES.length];
        Class<?> clazz = NavigationRule.class;
        for (int i = 0; i < FIELD_NAMES.length; i++) {
            try {
                Field f = clazz.getDeclaredField(FIELD_NAMES[i]);
                f.setAccessible(true);
                fields[i] = f;
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }
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
        return FIELD_NAMES.length;
    }

    @Override
    public @Nls String getColumnName(int column) {
        return NavigationRule.localise(FIELD_NAMES[column]);
    }

    public @Nls String getColumnTooltip(int column) {
        return NavigationRule.localise(FIELD_NAMES[column] + ".tip");
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return fields[columnIndex].getType();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        var rule = rules.get(rowIndex);
        try {
            return fields[columnIndex].get(rule);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        var rule = rules.get(rowIndex);
        try {
            fields[columnIndex].set(rule, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        fireTableCellUpdated(rowIndex, columnIndex);
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

    public ValidationInfo validate(int row, String field) {
        return rules.get(row).validateField(field);
    }

    /** Do not modify the collection. */
    Collection<NavigationRule> stateView() {
        return rules;
    }
}

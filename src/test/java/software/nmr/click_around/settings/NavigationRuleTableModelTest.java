package software.nmr.click_around.settings;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.nmr.click_around.filters.JavaAnnotation;
import software.nmr.click_around.filters.Xml;

import javax.swing.event.TableModelEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static software.nmr.click_around.settings.SettingsTestBase.exampleRule;
import static software.nmr.click_around.settings.SettingsTestBase.wildcardTag;

class NavigationRuleTableModelTest {

    private final NavigationRule ruleA = exampleRule();
    private final NavigationRule ruleB = wildcardTag("");
    private final NavigationRuleTableModel model = new NavigationRuleTableModel(List.of(ruleA, ruleB));
    private final List<TableModelEvent> events = new ArrayList<>();

    {
        model.addTableModelListener(events::add);
    }

    @Test
    void countsAreCorrect() {
        assertEquals(5, model.getColumnCount());
        assertEquals(2, model.getRowCount());
    }

    @Test
    void setValueAtUpdatesToFilterAndFiresCellEvent() {
        model.setValueAt("com.new.Anno", 1, 3);

        assertEquals("com.new.Anno", ruleB.to.getFqn());
        assertEquals(3, events.get(0).getColumn());
        assertEquals(1, events.get(0).getFirstRow());
    }

    @Test
    void columnNameRoutesToCorrectDescriptor() {
        assertEquals(Xml.DESC.getColumnName(0), model.getColumnName(0));
        assertEquals(JavaAnnotation.DESC.getColumnName(0), model.getColumnName(3));
    }

    @Test
    void columnTooltipRoutesToCorrectDescriptor() {
        assertEquals(Xml.DESC.getColumnTooltip(1), model.getColumnTooltip(1));
        assertEquals(JavaAnnotation.DESC.getColumnTooltip(1), model.getColumnTooltip(4));
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0, 0",
            "1, 0, 1",
            "2, 0, 2",
            "3, 1, 0",
            "4, 1, 1",
    })
    void getValueAtRoutesToCorrectFilter(int column, int which, int col) {
        Object expected = List.of(ruleA.from, ruleA.to).get(which).arrayView()[col];
        assertEquals(expected, model.getValueAt(0, column));
    }

    @Test
    void setValueAtUpdatesFromFilterAndFiresCellEvent() {
        model.setValueAt("NEW_TAG", 0, 1);

        assertEquals("NEW_TAG", ruleA.from.getTag());
        assertEquals(1, events.size());
        TableModelEvent ev = events.get(0);
        assertEquals(0, ev.getFirstRow());
        assertEquals(0, ev.getLastRow());
        assertEquals(1, ev.getColumn());
        assertEquals(TableModelEvent.UPDATE, ev.getType());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3, 4})
    void needValidation(int column) {
        assertTrue(NavigationRuleTableModel.needValidation(column));
    }

    @Test
    void validateRoutesToCorrectFilter() {
        ruleA.from.setTag("");
        assertNotNull(model.validate(0, 1));

        ruleA.from.setTag("ok");
        assertNull(model.validate(0, 1));

        ruleB.to.setFqn("");
        assertNotNull(model.validate(1, 3));
    }

    @Test
    void addRuleAppendsAndFiresInsertedEvent() {
        model.addRule();

        assertEquals(3, model.getRowCount());
        assertEquals(1, events.size());
        TableModelEvent ev = events.get(0);
        assertEquals(TableModelEvent.INSERT, ev.getType());
        assertEquals(2, ev.getFirstRow());
        assertEquals(2, ev.getLastRow());
    }

    @Test
    void removeRuleRemovesAndFiresDeletedEvent() {
        model.removeRule(0);

        assertEquals(1, model.getRowCount());
        assertEquals(ruleB, model.rules.iterator().next());
        TableModelEvent ev = events.get(0);
        assertEquals(TableModelEvent.DELETE, ev.getType());
        assertEquals(0, ev.getFirstRow());
    }

    @Test
    void resetReplacesRowsAndFiresDataChangedEvent() {
        var fresh = exampleRule();
        model.reset(Set.of(fresh));

        assertEquals(1, model.getRowCount());
        assertEquals(fresh, model.rules.iterator().next());
        assertEquals(1, events.size());
        TableModelEvent ev = events.get(0);
        assertEquals(0, ev.getFirstRow());
        assertEquals(Integer.MAX_VALUE, ev.getLastRow());
    }
}

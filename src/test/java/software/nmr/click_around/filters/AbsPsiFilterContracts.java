package software.nmr.click_around.filters;

import com.google.common.reflect.TypeToken;
import com.intellij.openapi.ui.ValidationInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;
import org.junit.jupiter.params.provider.MethodSource;
import software.nmr.click_around.filters.AbsPsiFilter.Descriptor;

import java.beans.PropertyDescriptor;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for any {@link AbsPsiFilter} subclass.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbsPsiFilterContracts<F extends AbsPsiFilter<?>> {

    // -- helpers -------------------------------------------------------------

    private final TypeToken<F> typeToken = new TypeToken<>(getClass()) {
    };
    @SuppressWarnings("unchecked")
    private final Class<F> type = (Class<F>) typeToken.getType();
    protected final String simpleName = type.getSimpleName();
    protected final Descriptor descriptor;
    protected Set<Integer> mandatoryColumns;

    {
        try {
            descriptor = (Descriptor) type.getField("DESC").get(null);
            descriptor.getColumnClass(0); // Init props field
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected final F callDefaultCtor() {
        try {
            return type.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected IntStream columnIndexes() {
        return IntStream.range(0, descriptor.getColumnCount());
    }

    protected Stream<Arguments> columnProperties() {
        return columnIndexes().mapToObj(i -> Arguments.of(i, descriptor.props[i]));
    }

    protected abstract F makeValidInstance();

    // -- tests ----------------------------------------------------

    @Test
    void descriptor_getColumnCount_MatchesDomainModel() {
        assertEquals(callDefaultCtor().arrayView().length, descriptor.getColumnCount());
    }

    @ParameterizedTest
    @MethodSource("columnIndexes")
    void descriptor_ColumnsHasLocalisation(int col) {
        assertTrue(descriptor.nlsKeys[col].startsWith(simpleName + "."));
        assertNotNull(descriptor.getColumnName(col));
        assertNotNull(descriptor.getColumnTooltip(col));
    }

    // getColumnClass is implemented using reflection, so no point in testing it using the same logic

    @Test
    void descriptor_ReturnsCorrectInstance() {
        assertSame(descriptor, callDefaultCtor().descriptor());
    }

    @Test
    void convenientConstructorIsCorrect() throws Exception {
        // Auto-generated c'tor can have wrong column order
        for (var ctor : type.getConstructors()) {
            if (ctor.getParameterCount() == descriptor.getColumnCount()) {
                var valid = makeValidInstance();
                var viaCtor = ctor.newInstance((Object[]) valid.arrayView());
                assertEquals(valid, viaCtor);
                break;
            }
        }
    }

    @ParameterizedTest
    @MethodSource("columnProperties")
    void setterAndGetterUseTheCorrectIndex(int col, PropertyDescriptor pd) throws Exception {
        F filter = callDefaultCtor();
        var unique = "VAL_" + Math.random();
        pd.getWriteMethod().invoke(filter, unique);
        assertEquals(unique, filter.arrayView()[col]);
        assertEquals(unique, pd.getReadMethod().invoke(filter));
    }

    @ParameterizedTest
    @FieldSource("mandatoryColumns")
    void validationTargetsCorrectColumn(int col) throws Exception {
        F filter = makeValidInstance();
        assertNull(filter.validateField(col), "filled column should be valid");

        descriptor.props[col].getWriteMethod().invoke(filter, "");
        assertNotNull(filter.validateField(col));

        assertNotNull(filter.firstInvalidField());
    }

    @Test
    void copy_EqualsButNotSame() throws Exception {
        F filter = makeValidInstance();
        var copy = filter.copy();
        assertNotSame(filter, copy);
        assertEquals(filter, copy);
        assertEquals(filter.hashCode(), copy.hashCode());
        assertNotSame(filter.arrayView(), copy.arrayView());
    }

    @Test
    void equals_RejectsDifferentType() {
        F field = callDefaultCtor();
        assertNotEquals(null, field);
        assertNotEquals(new AbsPsiFilter<Object>(field.arrayView()) {
            @Override
            public ValidationInfo validateField(int fieldIndex) {
                return null;
            }

            @Override
            public Descriptor descriptor() {
                return descriptor;
            }

            @Override
            public AbsPsiFilter<Object> copy() {
                return this;
            }
        }, field);
    }

    @Test
    void equals_hashcode_RejectsDifferentValues() {
        F valid = makeValidInstance();
        for (int col = 0; col < descriptor.getColumnCount(); col++) {
            AbsPsiFilter<?> f = valid.copy();
            f.arrayView()[col] = null;

            assertNotEquals(f, valid);
            assertNotEquals(f.hashCode(), valid.hashCode());
        }
    }

    @Test
    void toStringContainsType() {
        assertTrue(makeValidInstance().toString().contains(simpleName));
    }

    abstract void toStringContainsAllFields();
}

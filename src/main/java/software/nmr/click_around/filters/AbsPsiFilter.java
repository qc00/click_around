package software.nmr.click_around.filters;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.DynamicBundle;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;
import software.nmr.click_around.settings.NavigationRule;

import java.beans.FeatureDescriptor;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A subclass will hold parameters to filter out specific kind(s)/aspect(s) of {@link com.intellij.psi.PsiElement}s
 * and provide metadata for the configuration UI.
 * <p>
 * This base implementation handles most of the Object contracts by abstracting all the filter parameters into the
 * {@link #data} array. Subclasses must implement getters/setters following IntelliJ {@code PersistentStateComponent}
 * serialisation rules.
 */
public abstract class AbsPsiFilter<T> {

    public static class Descriptor {
        private final Class<?> cls;
        private final String prefix;
        @VisibleForTesting
        final String[] nlsKeys;
        @VisibleForTesting
        final PropertyDescriptor[] props;
        private final Class<?>[] columnClasses;

        /**
         * @param nlsKeys In the "SimpleName.propertyName" format. Matching order in the data[].
         */
        protected Descriptor(Class<?> cls, String... nlsKeys) {
            this.cls = cls;
            this.prefix = cls.getSimpleName() + ".";
            this.nlsKeys = nlsKeys;
            this.props = new PropertyDescriptor[nlsKeys.length];
            this.columnClasses = new Class<?>[nlsKeys.length];
        }

        public int getColumnCount() {
            return nlsKeys.length;
        }

        public @Nls String getColumnName(int column) {
            return localise(nlsKeys[column]);
        }

        public @Nls String getColumnTooltip(int column) {
            return localise(nlsKeys[column] + ".tip");
        }

        public Class<?> getColumnClass(int columnIndex) {
            if (columnClasses[0] == null) {
                try {
                    var descs = Introspector.getBeanInfo(cls, AbsPsiFilter.class).getPropertyDescriptors();
                    var map = Arrays.stream(descs).collect(Collectors.toMap(FeatureDescriptor::getName, p -> p));
                    for (int i = 0; i < nlsKeys.length; i++) {
                        var propName = nlsKeys[i].substring(prefix.length());
                        var prop = props[i] = map.get(propName);
                        columnClasses[i] = prop.getReadMethod().getReturnType();
                    }
                } catch (Exception e) {
                    throw new IllegalStateException("Unable to inspect " + cls.getName(), e);
                }
            }
            return columnClasses[columnIndex];
        }
    }

    public abstract ValidationInfo validateField(int fieldIndex);

    public @Nls String firstInvalidField() {
        for (int i = 0; i < descriptor().getColumnCount(); i++) {
            if (validateField(i) != null) return descriptor().getColumnName(i);
        }
        return null;
    }

    //region Simple
    protected final T[] data;

    @SafeVarargs
    protected AbsPsiFilter(T... data) {
        this.data = data;
    }

    public abstract Descriptor descriptor();

    public abstract AbsPsiFilter<T> copy();

    protected <R extends AbsPsiFilter<T>> R copyInto(R newInstance) {
        System.arraycopy(data, 0, newInstance.data, 0, data.length);
        return newInstance;
    }

    public T[] arrayView() { return data; }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        return Arrays.equals(arrayView(), ((AbsPsiFilter<?>) o).arrayView());
    }

    @Override
    public int hashCode() {
        return Objects.hash((Object[]) arrayView());
    }
    //endregion

    //region NLS
    private static final String BUNDLE = "messages.filters";
    private static final DynamicBundle instance = new DynamicBundle(NavigationRule.class, BUNDLE);
    protected static final ValidationInfo GENERIC_EMPTY = new ValidationInfo(localise("*.empty"));

    public static @NotNull @Nls String localise(
            @NotNull @PropertyKey(resourceBundle = BUNDLE) String key, @NotNull Object... params) {
        return instance.getMessage(key, Arrays.copyOf(params, params.length));
    }
    //endregion
}

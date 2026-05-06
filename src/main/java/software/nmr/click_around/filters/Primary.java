package software.nmr.click_around.filters;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A Primary filter locate exact {@link com.intellij.psi.PsiElement}s that are conceptually single token/symbols.
 * <p>
 * When used as the navigation source, matching is handled by the {@linkplain software.nmr.click_around.handlers.RulesIndex external index} this contributes to.<br>
 * When used as the destination, {@link #search} is normally called.
 */
public abstract class Primary {

    protected int hash;
    protected @NonNull Object[] fields;
    protected @NonNull Set<? extends Secondary> secondaries = new HashSet<>();

    protected Primary(Object... fields) {
        this.fields = fields;
    }

    @SuppressWarnings("unchecked")
    protected final <T> T field(int index) {
        return (T) fields[index];
    }

    protected final void field(int index, Object value) {
        if (hash != 0) throw new IllegalStateException("Cannot modify after hashcode() has been calculated");
        fields[index] = value;
    }

    /**
     * Because JAXB SchemaGenerator doesn't support interfaces, implementation should override with a
     * {@link jakarta.xml.bind.annotation.XmlElements @XmlElements} to list all the {@code SecondaryTag} impls.
     */
    public abstract Set<? extends Secondary> getSecondaries();

    @Override
    public final int hashCode() {
        if (hash == 0) {
            hash = Arrays.hashCode(fields) * 31 + secondaries.hashCode();
            if (hash == 0) hash = 1;
            secondaries = Collections.unmodifiableSet(secondaries);
        }
        return hash;
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Primary other) || getClass() != obj.getClass()) return false;
        if (hash != 0 && other.hash != 0 && hash != other.hash) return false;
        return Arrays.equals(fields, other.fields) && secondaries.equals(other.secondaries);
    }

    //

    public boolean matchAllSecondaries(PsiElement element) {
        return getSecondaries().stream().allMatch(s -> s.test(element));
    }

    public interface MaybeSynchronizedConsumer<T> extends Consumer<T> {
        default void synchronizedAccept(T t) {
            synchronized (this) {
                accept(t);
            }
        }
    }

    /**
     * Find the {@link PsiElement}s that match this filter and also has the exact given text.
     *
     * @implNote Don't forget to call {@link #matchAllSecondaries}
     */
    public abstract void search(Project project, String text, MaybeSynchronizedConsumer<PsiElement> downstream);
}

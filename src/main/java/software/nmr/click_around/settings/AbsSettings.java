package software.nmr.click_around.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared settings fields, serialisation and change tracking (for invalidating caches).
 */
public abstract class AbsSettings<Self extends AbsSettings<Self>> implements PersistentStateComponent<Self> {
    public LinkedHashSet<NavigationRule> rules = new LinkedHashSet<>();
    protected final AtomicInteger ruleVersion = new AtomicInteger(0);

    public void notifyRules() {
        ruleVersion.incrementAndGet();
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull Self getState() {
        return (Self) this;
    }

    @Override
    public void loadState(@NotNull Self state) {
        var cast = (AbsSettings<?>) state;
        rules = cast.rules;

        notifyRules();
    }
}

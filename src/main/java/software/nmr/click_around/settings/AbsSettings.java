package software.nmr.click_around.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Shared settings contents, serialisation and change notification.
 */
public abstract class AbsSettings<Self extends AbsSettings<Self>> implements PersistentStateComponent<Self> {
    public Set<NavigationRule> rules = new HashSet<>();
    private final ArrayList<WeakReference<Runnable>> ruleChangeListeners = new ArrayList<>();

    public void hookRules(Runnable listener) {
        ruleChangeListeners.add(new WeakReference<>(listener));
    }

    public void notifyRules() {
        ruleChangeListeners.stream().map(WeakReference::get).filter(Objects::nonNull).forEach(Runnable::run);
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

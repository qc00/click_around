package software.nmr.click_around.settings;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

@Service(Service.Level.PROJECT)
@State(name = "nmr.ClickAroundProjectSettings")
public final class ProjectSettings extends AbsSettings<ProjectSettings> {
    private final AppSettings app;
    private Set<NavigationRule> effectiveRules;

    public ProjectSettings() {
        app = AppSettings.getInstance();

        Runnable clearER = () -> effectiveRules = null;
        app.hookRules(clearER);
        hookRules(clearER);
    }

    public static ProjectSettings getInstance(@NotNull Project project) {
        return project.getService(ProjectSettings.class);
    }

    public Set<NavigationRule> getEffectiveRules() {
        if (effectiveRules == null) {
            effectiveRules = new HashSet<>();
            effectiveRules.addAll(app.rules);
            effectiveRules.addAll(rules);
        }
        return effectiveRules;
    }
}

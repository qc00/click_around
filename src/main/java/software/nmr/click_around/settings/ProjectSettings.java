package software.nmr.click_around.settings;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import software.nmr.click_around.handlers.RulesIndex;

import java.util.stream.Stream;

@Service(Service.Level.PROJECT)
@State(name = "nmr.ClickAroundProjectSettings")
public final class ProjectSettings extends AbsSettings {

    private final AppSettings app = AppSettings.getInstance();
    private final VersionedCache<RulesIndex> indexCache = new VersionedCache<>(this::buildRulesIndex);

    public static ProjectSettings getInstance(@NotNull Project project) {
        return project.getService(ProjectSettings.class);
    }

    RulesIndex buildRulesIndex() {
        return new RulesIndex(Stream.concat(app.rules.stream(), rules.stream()));
    }

    public RulesIndex getCombedRules() {
        return indexCache.getForVersion(app.ruleVersion.get() + ruleVersion.get());
    }
}

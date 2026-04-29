package software.nmr.click_around.settings;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import software.nmr.click_around.filters.JavaAnnotation;

import java.util.stream.Stream;

@Service(Service.Level.PROJECT)
@State(name = "nmr.ClickAroundProjectSettings")
public final class ProjectSettings extends AbsSettings<ProjectSettings> {
    @VisibleForTesting
    static final WildcardIndex.MultiLevelIndexer<NavigationRule, WildcardIndex<WildcardIndex<WildcardIndex<JavaAnnotation>>>> XML_INDEXER =
            new WildcardIndex.MultiLevelIndexer<NavigationRule, JavaAnnotation>(nr -> nr.to)
                    .addLevel(nr -> nr.from.getNamespace())
                    .addLevel(nr -> nr.from.getTag())
                    .addLevel(nr -> nr.from.getAttr());

    private final AppSettings app = AppSettings.getInstance();
    private final VersionedCache<WildcardIndex<WildcardIndex<WildcardIndex<JavaAnnotation>>>> indexByXml
            = new VersionedCache<>(() -> XML_INDEXER.index(Stream.concat(app.rules.stream(), rules.stream())));

    public static ProjectSettings getInstance(@NotNull Project project) {
        return project.getService(ProjectSettings.class);
    }

    public WildcardIndex<WildcardIndex<WildcardIndex<JavaAnnotation>>> getIndexByXml() {
        return indexByXml.getForVersion(app.ruleVersion.get() + ruleVersion.get());
    }
}

package software.nmr.click_around.settings;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import software.nmr.click_around.filters.JavaAnnotation;

import java.util.stream.Stream;

@Service(Service.Level.PROJECT)
@State(name = "nmr.ClickAroundProjectSettings")
public final class ProjectSettings extends AbsSettings<ProjectSettings> {
    private static final WildcardIndex.MultiLevelIndexer<NavigationRule, WildcardIndex<WildcardIndex<WildcardIndex<JavaAnnotation>>>> XML_INDEXER =
            new WildcardIndex.MultiLevelIndexer<NavigationRule, JavaAnnotation>(nr -> nr.to)
                    .addLevel(nr -> nr.from.getNamespace())
                    .addLevel(nr -> nr.from.getTag())
                    .addLevel(nr -> nr.from.getAttr());

    private final AppSettings app;
    private WildcardIndex<WildcardIndex<WildcardIndex<JavaAnnotation>>> indexByXml;

    public ProjectSettings() {
        app = AppSettings.getInstance();

        app.hookRules(this::clearDerived);
        hookRules(this::clearDerived);
    }

    public static ProjectSettings getInstance(@NotNull Project project) {
        return project.getService(ProjectSettings.class);
    }

    private void clearDerived() {
        indexByXml = null;
    }

    public WildcardIndex<WildcardIndex<WildcardIndex<JavaAnnotation>>> getIndexByXml() {
        if (indexByXml == null) {
            indexByXml = XML_INDEXER.index(Stream.concat(app.rules.stream(), rules.stream()));
        }
        return indexByXml;
    }
}

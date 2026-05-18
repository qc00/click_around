package software.nmr.click_around.settings;

import com.intellij.javaee.ExternalResourceManagerEx;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.jetbrains.annotations.NotNull;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.StringReader;
import java.net.URL;
import java.util.Objects;

public class SettingsSchema {
    private static final Logger LOG = Logger.getInstance(SettingsSchema.class);

    static final URL RESOURCE = SettingsSchema.class.getClassLoader().getResource("schema1.xsd");
    static final Schema SCHEMA = load();
    /** {@link #registerSchema()} uses this project, which is also used by the config editor. */
    static final Project PROJECT = ProjectManager.getInstance().getDefaultProject();
    static final String URN = "urn:click_around.xsd";

    private static Schema load() {
        try {
            var factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            return factory.newSchema(RESOURCE);
        } catch (Exception e) {
            LOG.warn("Unable to load Schema from resource " + RESOURCE, e);
        }
        return null;
    }

    public static void validate(@NotNull String xml) throws Exception {
        if (SCHEMA == null) throw new IllegalStateException("Cannot validate due to missing schema");
        var validator = SCHEMA.newValidator();
        validator.validate(new StreamSource(new StringReader(xml)));
    }

    /**
     * Points the {@value #URN} to {@link #RESOURCE}, so the editor can locate the file.
     */
    public static void registerSchema() {
        Objects.requireNonNull(RESOURCE, "Failed to locate the schema");
        var manager = ExternalResourceManagerEx.getInstanceEx();
        if (!URN.equals(manager.getResourceLocation(URN, PROJECT))) return;

        ApplicationManager.getApplication().runWriteAction(() -> {
            manager.addResource(URN, RESOURCE.toString(), PROJECT);
        });
    }
}

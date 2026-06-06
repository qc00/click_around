package software.nmr.click_around.settings;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.ui.LanguageTextField;
import com.intellij.ui.LanguageTextField.SimpleDocumentCreator;
import com.intellij.xml.XmlSchemaProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.StringReader;
import java.net.URL;
import java.util.Objects;
import java.util.Set;

public class SettingsSchema extends XmlSchemaProvider {
    private static final URL RESOURCE = SettingsSchema.class.getClassLoader().getResource("schema1.xsd");
    private static final Key<Object> FILE_MARKER = Key.create("click_around.SettingsSchema");
    private static Schema SCHEMA;

    static LanguageTextField.SimpleDocumentCreator DOC_CREATOR = new SimpleDocumentCreator() {
        public void customizePsiFile(PsiFile file) {
            markAsSettingsFile(file);
        }
    };

    @VisibleForTesting
    static void markAsSettingsFile(PsiFile file) {
        file.putCopyableUserData(FILE_MARKER, 1);
    }

    public static void validate(@NotNull String xml) throws Exception {
        if (SCHEMA == null) {
            var factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            SCHEMA = factory.newSchema(RESOURCE);
        }
        var validator = SCHEMA.newValidator();
        validator.validate(new StreamSource(new StringReader(xml)));
    }

    @Override
    public boolean isAvailable(@NotNull XmlFile file) {
        return file.getCopyableUserData(FILE_MARKER) != null;
    }

    @Override
    public @NotNull Set<String> getAvailableNamespaces(@NotNull XmlFile file, @Nullable String tagName) {
        return isAvailable(file) ? Set.of("") : Set.of();
    }

    @Override
    public @Nullable XmlFile getSchema(@NotNull String url, @Nullable Module module, @NotNull PsiFile baseFile) {
        if (!url.isEmpty()) return null;
        var vf = VfsUtil.findFileByURL(Objects.requireNonNull(SettingsSchema.RESOURCE));
        if (vf == null) return null;
        var psi = PsiManager.getInstance(baseFile.getProject()).findFile(vf);
        return psi instanceof XmlFile xf ? xf : null;
    }
}

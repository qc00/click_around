package software.nmr.click_around.settings;

import com.google.common.reflect.ClassPath;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.xmlb.annotations.Text;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.SchemaOutputResolver;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.eclipse.persistence.jaxb.JAXBMarshaller;
import org.jetbrains.annotations.NotNull;
import software.nmr.click_around.filters.Primary;

import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Shared settings fields, serialisation and change tracking (for invalidating caches).
 *
 * @implNote Data flow:
 * IDE config ⇔ intellij.util.xmlb ⇔ {@link State} ⇔ JAXB ⇔ {@link Rules} ⇔ {@link AbsSettings#rules}
 */
public abstract class AbsSettings implements PersistentStateComponent<AbsSettings.State> {
    private static final Logger LOG = Logger.getInstance(AbsSettings.class);

    static final JAXBContext CONTEXT;

    static {
        try {
            // Doing this manually is simpler than MOXy's built-in package metadata requirements
            var filters = ClassPath.from(Thread.currentThread().getContextClassLoader())
                                   .getTopLevelClasses(Primary.class.getPackageName())
                                   .stream().map(ClassPath.ClassInfo::load);
            CONTEXT = JAXBContextFactory.createContext(
                    Stream.concat(Stream.of(Rules.class), filters).toArray(Class[]::new), Map.of());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final State state = new State();
    protected List<NavigationRule> rules = new ArrayList<>();
    protected final AtomicInteger ruleVersion = new AtomicInteger(0);

    public void notifyRules() {
        ruleVersion.incrementAndGet();
    }

    /** The DTO for the {@link PersistentStateComponent} calls. */
    public static class State {
        @Text
        public String xml;
    }

    /** DTO for interacting with JAXB. */
    @XmlRootElement
    public static class Rules {
        @XmlElement(name = "rule")
        public List<NavigationRule> rules = new ArrayList<>();
    }

    @Override
    public @NotNull State getState() {
        if (state.xml == null) { // Only happens in tests
            state.xml = marshalXml();
        }
        return state;
    }

    private String marshalXml() {
        try {
            var marshaller = CONTEXT.createMarshaller();
            marshaller.setProperty(JAXBMarshaller.JAXB_FRAGMENT, Boolean.TRUE);
            var sw = new StringWriter();
            var obj = new Rules();
            obj.rules = rules;
            marshaller.marshal(obj, sw);
            return sw.toString();
        } catch (JAXBException e) {
            throw new RuntimeException("Failed to marshall", e);
        }
    }

    @Override
    public void loadState(@NotNull State state) {
        if (state.xml == null || state.xml.isEmpty()) return;
        var exception = setRules(state.xml);
        if (exception != null) {
            LOG.warn("Config unmarshalling failed. Problematic XML:" + state.xml.replaceAll("\\R\\s*", "⏎"), exception);
            this.rules = Collections.emptyList();
            var message = exception.getMessage();
            if (message == null) message = exception.getClass().getName();
            this.state.xml = "<!-- Config unmarshalling failed: " + message + " -->\n" +
                    state.xml.replaceFirst("^<-- Config unmarshalling failed:.*?-->\n", "");
        }
    }

    Exception setRules(@NotNull String xml) {
        try {
            if (xml.trim().isEmpty()) {
                rules = Collections.emptyList();
            } else {
                var unmarshaller = CONTEXT.createUnmarshaller();
                var obj = unmarshaller.unmarshal(new StringReader(xml));
                rules = Collections.unmodifiableList(((Rules) obj).rules);
            }
            state.xml = xml;
            notifyRules();
            return null;
        } catch (Exception e) {
            return e;
        }
    }

    public static void main(String[] args) throws Exception {
        var outputDir = Path.of(args[0]);
        Files.createDirectories(outputDir);

        AbsSettings.CONTEXT.generateSchema(new SchemaOutputResolver() {
            public Result createOutput(String namespaceUri, String fileName) {
                var outputFile = outputDir.resolve(fileName);
                var result = new StreamResult(outputFile.toFile());
                result.setSystemId(outputFile.toUri().toString());
                return result;
            }
        });
    }
}
package software.nmr.click_around.settings;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;

@Service(Service.Level.APP)
@State(name = "nmr.ClickAroundAppSettings", storages = @Storage("clickAround.xml"))
public final class AppSettings extends AbsSettings<AppSettings> {

    @VisibleForTesting
    static AppSettings testOverride;

    public static AppSettings getInstance() {
        return testOverride != null ? testOverride : ApplicationManager.getApplication().getService(AppSettings.class);
    }
}

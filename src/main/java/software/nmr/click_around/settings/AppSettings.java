package software.nmr.click_around.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;

@Service(Service.Level.APP)
@State(name = "nmr.ClickAroundAppSettings", storages = @Storage("clickAround.xml"))
public final class AppSettings extends AbsSettings<AppSettings> {

    public static AppSettings getInstance() {
        return ApplicationManager.getApplication().getService(AppSettings.class);
    }
}


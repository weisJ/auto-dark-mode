package com.github.weisj.darkmode;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.openapi.components.ServiceManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class AutoDarkModeStartupListener implements AppLifecycleListener {

    @Override
    public void appFrameCreated(@NotNull List<String> commandLineArgs) {
        ServiceManager.getService(AutoDarkMode.class).start();
    }

    @Override
    public void appClosing() {
        Optional.ofNullable(ServiceManager.getServiceIfCreated(AutoDarkMode.class)).ifPresent(AutoDarkMode::stop);
    }
}

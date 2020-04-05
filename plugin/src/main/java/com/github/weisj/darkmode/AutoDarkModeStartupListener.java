package com.github.weisj.darkmode;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.openapi.components.ServiceManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AutoDarkModeStartupListener implements AppLifecycleListener {

    @Override
    public void appFrameCreated(@NotNull List<String> commandLineArgs) {
        ServiceManager.getService(AutoDarkMode.class).start();
    }
}

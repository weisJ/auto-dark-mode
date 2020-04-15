package com.github.weisj.darkmode;

import com.intellij.ide.plugins.DynamicPluginListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.openapi.components.ServiceManager;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class AutoDarkModePluginListener implements DynamicPluginListener {

    @Override
    public void pluginLoaded(@NotNull IdeaPluginDescriptor pluginDescriptor) {
        ServiceManager.getService(AutoDarkMode.class).start();
    }

    @Override
    public void beforePluginUnload(@NotNull IdeaPluginDescriptor pluginDescriptor, boolean isUpdate) {
        Optional.ofNullable(ServiceManager.getServiceIfCreated(AutoDarkMode.class)).ifPresent(AutoDarkMode::stop);
    }
}

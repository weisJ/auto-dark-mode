package com.github.weisj.darkmode

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.components.ServiceManager

class AutoDarkModePluginListener : DynamicPluginListener {

    override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
        ServiceManager.getService(AutoDarkMode::class.java).install()
    }

    override fun beforePluginUnload(pluginDescriptor: IdeaPluginDescriptor, isUpdate: Boolean) {
        ServiceManager.getServiceIfCreated(AutoDarkMode::class.java)?.uninstall()
    }
}

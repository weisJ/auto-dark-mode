package com.github.weisj.darkmode

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project

class AutoDarkModeStartupListener : AppLifecycleListener {
    override fun appFrameCreated(commandLineArgs: List<String>) {
        ServiceManager.getService(AutoDarkMode::class.java).start()
    }

    override fun appStarting(projectFromCommandLine: Project?) {
        IntellijNotificationService.initialize()
    }

    override fun appClosing() {
        ApplicationManager.getApplication().getServiceIfCreated(AutoDarkMode::class.java)?.stop()
    }
}

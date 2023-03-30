package com.github.weisj.darkmode.platform.linux

import com.github.weisj.darkmode.platform.LibraryUtil
import com.github.weisj.darkmode.platform.Notifications
import com.github.weisj.darkmode.platform.OneTimeAction
import com.github.weisj.darkmode.platform.settings.DefaultSettingsContainer
import com.github.weisj.darkmode.platform.settings.SettingsContainerProvider
import com.github.weisj.darkmode.platform.settings.SingletonSettingsContainerProvider
import com.github.weisj.darkmode.platform.settings.activeIf
import com.github.weisj.darkmode.platform.settings.group
import com.github.weisj.darkmode.platform.settings.hidden
import com.github.weisj.darkmode.platform.settings.persistentBooleanProperty
import com.google.auto.service.AutoService

@AutoService(SettingsContainerProvider::class)
class AdvancedLinuxSettingsProvider :
    SingletonSettingsContainerProvider(
        { AdvancedLinuxSettings },
        enabled = LibraryUtil.isLinux
    )

object AdvancedLinuxSettings : DefaultSettingsContainer(identifier = "advanced_linux_settings") {

    private val advancedSettingsLogAction = OneTimeAction {
        Notifications.dispatchNotification(
            """
            Theme monitoring is currently not supported for your platform.
            If you are sure that you are using a desktop environment supporting gsettings you can enforce the usage of
            the GTK based implementation in the settings.
            """.trimIndent(),
            showSettingsLink = true
        )
    }

    var overrideGtkDetection = false
    var enableXdgImplementation = false

    init {
        group("Advanced") {
            if (!LibraryUtil.isGtk) {
                persistentBooleanProperty(
                    description = "Override Gtk detection (Enforce using Gtk implementation)",
                    value = ::overrideGtkDetection
                ).activeIf(::enableXdgImplementation.isFalse())
            }
            persistentBooleanProperty(
                description = "Enable Xdg-Desktop implementation (Will take precedence over Gtk)",
                value = ::enableXdgImplementation
            )
        }

        hidden {
            persistentBooleanProperty(value = advancedSettingsLogAction::executed)
        }
    }

    override fun onSettingsLoaded() {
        if (!overrideGtkDetection) advancedSettingsLogAction()
    }
}

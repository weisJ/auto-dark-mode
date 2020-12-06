/*
 * MIT License
 *
 * Copyright (c) 2020 Jannis Weis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package com.github.weisj.darkmode.platform.macos

import com.github.weisj.darkmode.platform.LibraryUtil
import com.github.weisj.darkmode.platform.Notifications
import com.github.weisj.darkmode.platform.OneTimeAction
import com.github.weisj.darkmode.platform.settings.*
import com.google.auto.service.AutoService

@AutoService(SettingsContainerProvider::class)
class MacOSSettingsProvider : SingletonSettingsContainerProvider({ MacOSSettings }, enabled = LibraryUtil.isMac)

object MacOSSettings : DefaultSettingsContainer(identifier = "macos_settings") {

    /*
     * Notify user that IDEA should be restarted at least once. This notice should only be logged once.
     */
    private val restartLogAction = OneTimeAction {
        Notifications.dispatchNotification(
            """
            You should restart (stop completely and start again) IDEA once for Auto Dark Mode to be working properly.
            """.trimIndent(),
            showSettingsLink = false
        )
    }

    init {
        hidden {
            persistentBooleanProperty(value = restartLogAction::executed)
        }
    }

    override fun onSettingsLoaded() {
        restartLogAction()
    }
}

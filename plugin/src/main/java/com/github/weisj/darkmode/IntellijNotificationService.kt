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
package com.github.weisj.darkmode

import com.github.weisj.darkmode.platform.NotificationType
import com.github.weisj.darkmode.platform.NotificationsService
import com.google.auto.service.AutoService
import com.intellij.ide.impl.ProjectUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.util.IconLoader
import java.util.*

typealias IntelliJNotificationType = com.intellij.notification.NotificationType

/**
 * Workaround for the fact that auto service currently doesn't work with singleton objects.
 * https://github.com/google/auto/issues/785
 */
@AutoService(NotificationsService::class)
class IntellijNotificationServiceProxy : NotificationsService by IntellijNotificationService

object IntellijNotificationService : NotificationsService {

    private val ICON = IconLoader.getIcon("/META-INF/pluginIcon.svg", AutoDarkMode::class.java)

    private val NOTIFICATION_GROUP = NotificationGroupManager.getInstance()
        .getNotificationGroup("com.github.weisj.darkmode")

    /*
     * Notifications may not be displayed of they are dispatched before the application frame has been
     * fully created. We queue any incoming messages and dispatch them as soon as the application is ready.
     */
    private val messageQueue: Queue<Notification> = LinkedList()
    private var started = false

    fun initialize() {
        started = true
        val project = ProjectUtil.getOpenProjects().firstOrNull()
        while (messageQueue.isNotEmpty()) {
            messageQueue.poll().notify(project)
        }
    }

    override fun dispatchNotification(message: String, type: NotificationType, showSettingsLink: Boolean) {
        val notification = NOTIFICATION_GROUP.createNotification(
            title = "Auto Dark Mode",
            subtitle = null,
            content = message,
            type = type.toIntelliJType()
        ).also {
            it.icon = ICON
            if (showSettingsLink) {
                it.addAction(NotificationAction.create("View settings") { _, _ ->
                    ShowSettingsUtil.getInstance().showSettingsDialog(null, DarkModeConfigurable::class.java)
                })
            }
        }
        if (!started) {
            messageQueue.offer(notification)
        } else {
            notification.notify(ProjectUtil.getOpenProjects().firstOrNull())
        }
    }

    private fun NotificationType.toIntelliJType(): IntelliJNotificationType = when (this) {
        NotificationType.INFO -> IntelliJNotificationType.INFORMATION
        NotificationType.WARNING -> IntelliJNotificationType.WARNING
        NotificationType.ERROR -> IntelliJNotificationType.ERROR
    }
}

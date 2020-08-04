package com.github.weisj.darkmode

import com.github.weisj.darkmode.platform.NotificationType
import com.github.weisj.darkmode.platform.NotificationsService
import com.google.auto.service.AutoService
import com.intellij.ide.impl.ProjectUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.openapi.options.ShowSettingsUtil
import java.util.*

typealias IntelliJNotificationType = com.intellij.notification.NotificationType

/**
 * Workaround for the fact that auto service currently doesn't work with singleton objects.
 * https://github.com/google/auto/issues/785
 */
@AutoService(NotificationsService::class)
class IntellijNotificationServiceProxy : NotificationsService by IntellijNotificationService

object IntellijNotificationService : NotificationsService {

    private val NOTIFICATION_GROUP = NotificationGroup(
        "Auto Dark Mode",
        NotificationDisplayType.STICKY_BALLOON,
        true
    )

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
            "Auto Dark Mode",
            null,
            message,
            type.toIntelliJType()
        ).also {
            if (showSettingsLink) {
                it.addAction(NotificationAction.create("View Settings") { _, _ ->
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
}

fun NotificationType.toIntelliJType(): IntelliJNotificationType = when (this) {
    NotificationType.INFO -> IntelliJNotificationType.INFORMATION
    NotificationType.WARNING -> IntelliJNotificationType.WARNING
    NotificationType.ERROR -> IntelliJNotificationType.ERROR
}

package com.github.weisj.darkmode

import com.github.weisj.darkmode.platform.NotificationType
import com.github.weisj.darkmode.platform.NotificationsService
import com.google.auto.service.AutoService
import com.intellij.ide.impl.ProjectUtil
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup

typealias IntelliJNotificationType = com.intellij.notification.NotificationType

@AutoService(NotificationsService::class)
class IntellijNotificationService : NotificationsService {

    companion object {
        private val NOTIFICATION_GROUP = NotificationGroup(
            displayId = "Auto Dark Mode",
            displayType = NotificationDisplayType.STICKY_BALLOON,
            isLogByDefault = true
        )
    }

    override fun dispatchNotification(message: String, type: NotificationType) {
        NOTIFICATION_GROUP.createNotification(
            title = "Auto Dark Mode",
            subtitle = null,
            content = message,
            type = type.toIntelliJType()
        ).notify(ProjectUtil.getOpenProjects().getOrNull(0))
    }
}

fun NotificationType.toIntelliJType(): IntelliJNotificationType = when (this) {
    NotificationType.INFO -> IntelliJNotificationType.INFORMATION
    NotificationType.WARNING -> IntelliJNotificationType.WARNING
    NotificationType.ERROR -> IntelliJNotificationType.ERROR
}

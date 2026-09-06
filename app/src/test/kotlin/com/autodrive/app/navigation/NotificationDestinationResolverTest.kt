package com.autodrive.app.navigation

import com.autodrive.app.feature.notifications.domain.model.NotificationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationDestinationResolverTest {
    @Test fun `explicit route wins`() = assertEquals(
        "custom/route",
        NotificationDestinationResolver.resolve("custom/route", NotificationType.NEW_INVOICE),
    )

    @Test fun `blank explicit route is ignored`() = assertEquals(
        Screen.Balance.route,
        NotificationDestinationResolver.resolve("  ", NotificationType.NEW_COMMISSION),
    )

    @Test fun `commission opens balance`() = assertEquals(
        Screen.Balance.route,
        NotificationDestinationResolver.resolve(null, NotificationType.COMMISSION_PAID),
    )

    @Test fun `withdrawal opens balance`() = assertEquals(
        Screen.Balance.route,
        NotificationDestinationResolver.resolve(null, NotificationType.WITHDRAWAL_COMPLETED),
    )

    @Test fun `invoice opens achievements`() = assertEquals(
        Screen.Achievements.route,
        NotificationDestinationResolver.resolve(null, NotificationType.NEW_INVOICE),
    )

    @Test fun `chat message opens recent activity`() = assertEquals(
        Screen.RecentActivity.createRoute(),
        NotificationDestinationResolver.resolve(null, NotificationType.NEW_CHAT_MESSAGE),
    )

    @Test fun `profile warning opens profile`() = assertEquals(
        Screen.Profile.route,
        NotificationDestinationResolver.resolve(null, NotificationType.PROFILE_INCOMPLETE),
    )

    @Test fun `admin reminder opens home`() = assertEquals(
        Screen.Home.route,
        NotificationDestinationResolver.resolve(null, NotificationType.ADMIN_REMINDER),
    )

    @Test fun `welcome has no destination`() = assertNull(
        NotificationDestinationResolver.resolve(null, NotificationType.WELCOME),
    )

    @Test fun `unknown notification has no destination`() = assertNull(
        NotificationDestinationResolver.resolve(null, null),
    )
}

package com.autodrive.app.di

import com.autodrive.app.feature.balance.data.realtime.BalanceRealtimeParticipant
import com.autodrive.app.feature.commission.data.realtime.BillingRealtimeParticipant
import com.autodrive.app.feature.chat.data.realtime.ChatRealtimeParticipant
import com.autodrive.app.feature.notifications.data.realtime.NotificationsRealtimeParticipant
import com.autodrive.app.core.sync.realtime.RealtimeParticipant
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class RealtimeModule {

    @Binds
    @IntoSet
    abstract fun bindBillingRealtime(impl: BillingRealtimeParticipant): RealtimeParticipant

    @Binds
    @IntoSet
    abstract fun bindBalanceRealtime(impl: BalanceRealtimeParticipant): RealtimeParticipant

    @Binds
    @IntoSet
    abstract fun bindChatRealtime(impl: ChatRealtimeParticipant): RealtimeParticipant

    @Binds
    @IntoSet
    abstract fun bindNotificationsRealtime(
        impl: NotificationsRealtimeParticipant,
    ): RealtimeParticipant
}

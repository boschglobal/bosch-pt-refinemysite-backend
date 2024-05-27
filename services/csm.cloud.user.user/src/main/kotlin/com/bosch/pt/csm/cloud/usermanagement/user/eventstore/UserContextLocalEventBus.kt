/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.eventstore

import com.bosch.pt.csm.cloud.common.eventstore.BaseLocalEventBusWithTombstoneSupport
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class UserContextLocalEventBus(
    eventStore: UserContextEventStore,
    snapshotStores: List<UserContextSnapshotStore>,
    applicationEventPublisher: ApplicationEventPublisher
) :
    BaseLocalEventBusWithTombstoneSupport<UserContextEventStore, UserContextSnapshotStore>(
        eventStore, snapshotStores, applicationEventPublisher)

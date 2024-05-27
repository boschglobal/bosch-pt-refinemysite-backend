/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.craft.eventstore

import com.bosch.pt.csm.cloud.common.eventstore.BaseLocalEventBus
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class CraftContextLocalEventBus(
    eventStore: CraftContextEventStore,
    snapshotStores: List<CraftContextSnapshotStore>,
    eventPublisher: ApplicationEventPublisher,
) :
    BaseLocalEventBus<CraftContextEventStore, CraftContextSnapshotStore>(
        eventStore, snapshotStores, eventPublisher)

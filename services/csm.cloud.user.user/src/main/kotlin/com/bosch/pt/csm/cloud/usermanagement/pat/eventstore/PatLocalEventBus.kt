/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.pat.eventstore

import com.bosch.pt.csm.cloud.common.eventstore.BaseLocalEventBusWithTombstoneSupport
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class PatLocalEventBus(
    eventStore: PatEventStore,
    snapshotStores: List<PatSnapshotStore>,
    applicationEventPublisher: ApplicationEventPublisher,
    avroEventMappers: List<PatAvroEventMapper>
) :
    BaseLocalEventBusWithTombstoneSupport<PatEventStore, PatSnapshotStore>(
        eventStore,
        snapshotStores,
        applicationEventPublisher,
        avroEventMappers,
    )

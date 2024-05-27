/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.command.snapshotstore.command.handler

import com.bosch.pt.csm.cloud.common.eventstore.BaseLocalEventBusWithTombstoneSupport
import com.bosch.pt.csm.cloud.common.eventstore.SnapshotStore
import org.springframework.context.ApplicationEventPublisher

class TestEventBusWithTombstoneSupport(
    eventStore: TestEventStore,
    eventPublisher: ApplicationEventPublisher
) :
    BaseLocalEventBusWithTombstoneSupport<TestEventStore, SnapshotStore>(
        eventStore, emptyList(), eventPublisher)

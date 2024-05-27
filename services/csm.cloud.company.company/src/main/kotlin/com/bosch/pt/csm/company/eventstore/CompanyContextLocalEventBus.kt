/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.eventstore

import com.bosch.pt.csm.cloud.common.eventstore.BaseLocalEventBus
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class CompanyContextLocalEventBus(
    eventStore: CompanyContextEventStore,
    snapshotStores: List<CompanyContextSnapshotStore>,
    applicationEventPublisher: ApplicationEventPublisher
) :
    BaseLocalEventBus<CompanyContextEventStore, CompanyContextSnapshotStore>(
        eventStore, snapshotStores, applicationEventPublisher)

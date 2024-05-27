/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.eventstore

import com.bosch.pt.csm.cloud.common.eventstore.AbstractLocalEventBus
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
open class ProjectContextLocalEventBus(
    eventStore: ProjectContextEventStore,
    snapshotStores: List<ProjectContextSnapshotStore>,
    applicationEventPublisher: ApplicationEventPublisher
) :
    AbstractLocalEventBus<ProjectContextEventStore, ProjectContextSnapshotStore>(
        eventStore, snapshotStores, applicationEventPublisher)

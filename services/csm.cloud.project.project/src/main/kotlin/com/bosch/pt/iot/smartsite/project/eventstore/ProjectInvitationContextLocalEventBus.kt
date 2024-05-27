/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.eventstore

import com.bosch.pt.csm.cloud.common.eventstore.AbstractLocalEventBusWithTombstoneSupport
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
open class ProjectInvitationContextLocalEventBus(
    eventStore: ProjectInvitationContextEventStore,
    snapshotStores: List<ProjectInvitationContextSnapshotStore>,
    applicationEventPublisher: ApplicationEventPublisher
) :
    AbstractLocalEventBusWithTombstoneSupport<
        ProjectInvitationContextEventStore, ProjectInvitationContextSnapshotStore>(
        eventStore, snapshotStores, applicationEventPublisher)

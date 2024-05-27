/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.eventstore

import com.bosch.pt.csm.cloud.common.eventstore.BaseLocalEventBus
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class FeaturetoggleContextLocalEventBus(
    eventStore: FeaturetoggleContextEventStore,
    snapshotStores: List<FeaturetoggleContextSnapshotStore>,
    applicationEventPublisher: ApplicationEventPublisher,
    avroEventMappers: List<FeaturetoggleAvroEventMapper>
) :
    BaseLocalEventBus<FeaturetoggleContextEventStore, FeaturetoggleContextSnapshotStore>(
        eventStore, snapshotStores, applicationEventPublisher, avroEventMappers)

/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.consents.eventstore

import com.bosch.pt.csm.cloud.common.command.mapper.AvroEventMapper
import com.bosch.pt.csm.cloud.common.eventstore.BaseLocalEventBus
import com.bosch.pt.csm.cloud.usermanagement.consents.consents.command.mapper.ConsentDelayedAvroEventMapper
import com.bosch.pt.csm.cloud.usermanagement.consents.consents.command.mapper.UserConsentedAvroEventMapper
import com.bosch.pt.csm.cloud.usermanagement.consents.document.command.mapper.DocumentChangedAvroEventMapper
import com.bosch.pt.csm.cloud.usermanagement.consents.document.command.mapper.DocumentCreatedAvroEventMapper
import com.bosch.pt.csm.cloud.usermanagement.consents.document.command.mapper.DocumentVersionIncrementedAvroEventMapper
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

class ConsentsContextLocalEventBus(
    eventStore: ConsentsContextEventStore,
    snapshotStores: List<ConsentsContextSnapshotStore>,
    applicationEventPublisher: ApplicationEventPublisher,
    avroEventMappers: List<AvroEventMapper>
) :
    BaseLocalEventBus<ConsentsContextEventStore, ConsentsContextSnapshotStore>(
        eventStore, snapshotStores, applicationEventPublisher, avroEventMappers)

@Configuration
class ConsentsContextLocalEventBusConfiguration {
  @Bean
  fun consentsContextLocalEventBus(
      eventStore: ConsentsContextEventStore,
      snapshotStores: List<ConsentsContextSnapshotStore>,
      applicationEventPublisher: ApplicationEventPublisher
  ) =
      ConsentsContextLocalEventBus(
          eventStore,
          snapshotStores,
          applicationEventPublisher,
          listOf(
              ConsentDelayedAvroEventMapper(),
              DocumentCreatedAvroEventMapper(),
              DocumentVersionIncrementedAvroEventMapper(),
              DocumentChangedAvroEventMapper(),
              UserConsentedAvroEventMapper(),
          ))
}

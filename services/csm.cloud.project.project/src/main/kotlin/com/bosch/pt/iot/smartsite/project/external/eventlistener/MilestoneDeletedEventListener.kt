/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.external.eventlistener

import com.bosch.pt.csm.cloud.projectmanagement.external.messages.ExternalIdEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro.DELETED
import com.bosch.pt.iot.smartsite.project.external.repository.ExternalIdRepository
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class MilestoneDeletedEventListener(private val externalIdRepository: ExternalIdRepository) {

  @EventListener
  fun handle(event: MilestoneEventAvro) {
    if (event.name == DELETED) {
      externalIdRepository.findAllByObjectIdentifier(event.aggregate.getIdentifier()).forEach {
        externalIdRepository.delete(it, ExternalIdEventEnumAvro.DELETED)
      }
    }
  }
}

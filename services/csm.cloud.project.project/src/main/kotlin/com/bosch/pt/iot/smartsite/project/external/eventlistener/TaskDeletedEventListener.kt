/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.external.eventlistener

import com.bosch.pt.csm.cloud.projectmanagement.external.messages.ExternalIdEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.DELETED
import com.bosch.pt.iot.smartsite.project.external.repository.ExternalIdRepository
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class TaskDeletedEventListener(private val externalIdRepository: ExternalIdRepository) {

  @EventListener
  fun handle(event: TaskEventAvro) {
    if (event.name == DELETED) {
      externalIdRepository.findAllByObjectIdentifier(event.aggregate.getIdentifier()).forEach {
        externalIdRepository.delete(it, ExternalIdEventEnumAvro.DELETED)
      }
    }
  }
}

/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.external.eventlistener

import com.bosch.pt.csm.cloud.projectmanagement.external.messages.ExternalIdEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro.DELETED
import com.bosch.pt.iot.smartsite.project.external.repository.ExternalIdRepository
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ProjectDeletedEventListener(private val externalIdRepository: ExternalIdRepository) {

  @EventListener
  fun handle(event: ProjectEventAvro) {
    if (event.name == DELETED) {
      val externalIds =
          externalIdRepository.findAllByProjectId(event.aggregate.getIdentifier().asProjectId())
      externalIds.forEach { externalIdRepository.delete(it, ExternalIdEventEnumAvro.DELETED) }
    }
  }
}

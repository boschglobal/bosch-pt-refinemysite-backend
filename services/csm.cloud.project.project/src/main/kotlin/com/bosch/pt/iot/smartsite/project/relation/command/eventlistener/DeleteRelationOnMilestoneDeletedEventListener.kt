/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.command.eventlistener

import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.message.getIdentifier
import com.bosch.pt.iot.smartsite.project.relation.boundary.RelationService
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class DeleteRelationOnMilestoneDeletedEventListener(private val relationService: RelationService) {

  @EventListener
  fun handle(event: MilestoneEventAvro) {
    if (event.name == DELETED) {
      relationService.deleteByMilestoneIdentifier(event.getIdentifier())
    }
  }
}

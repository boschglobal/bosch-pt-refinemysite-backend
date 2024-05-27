/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.projectmanagement.project.participant.boundary.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.model.Participant
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.model.ParticipantRoleEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.task.model.Task
import com.bosch.pt.csm.cloud.projectmanagement.project.task.model.TaskStatusEnum
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class RecipientDeterminator(
    @Value("\${testadmin.user.identifier}") private val testadminUserIdentifier: UUID,
    private val participantService: ParticipantService,
) {

  fun determineDefaultRecipients(task: Task, lastModifiedByUserIdentifier: UUID): Set<UUID> {

    // workaround in case the data was created by the testadmin user
    if (lastModifiedByUserIdentifier == testadminUserIdentifier) {
      return emptySet()
    }

    // if the task is still in status "DRAFT" nobody is notified
    if (task.status == TaskStatusEnum.DRAFT) return emptySet()

    val csmParticipants =
        participantService
            .findAllByProjectIdentifierAndRole(task.projectIdentifier, ParticipantRoleEnum.CSM)
            .toSet()

    // if the task is not assigned or is assigned to one of the CSM's
    if (task.assigneeIdentifier == null ||
        csmParticipants.stream().anyMatch { csm -> task.assigneeIdentifier == csm.identifier }) {
      // The CSM's that are not the originators of the event are notified
      return mapToIdentifier(csmParticipants.toSet(), lastModifiedByUserIdentifier)
    }

    val assigneeParticipant =
        participantService.findOneByIdentifierAndProjectIdentifier(
            task.assigneeIdentifier, task.projectIdentifier)

    val crParticipants =
        participantService.findCrsForCompany(
            task.projectIdentifier, assigneeParticipant.companyIdentifier)

    // if task is assigned to a participant being one CR
    if (assigneeParticipant.role == ParticipantRoleEnum.CR) {
      // the CSM's and CR's that are not the originators of the event are notified
      return mapToIdentifier(
          csmParticipants.union(crParticipants).toSet(), lastModifiedByUserIdentifier)
    }

    // task is assigned to an FM (the remaining case)
    // the CSM's, CR's and FM assignee that are not the originators of the event are notified
    return mapToIdentifier(
        csmParticipants.union(crParticipants).union(setOf(assigneeParticipant)).toSet(),
        lastModifiedByUserIdentifier)
  }

  /**
   * Function that maps to the user identifier a set of participants, removing the originator and
   * inactive participants
   */
  private fun mapToIdentifier(recipients: Set<Participant>, originatorIdentifier: UUID) =
      recipients
          .filter { recipient ->
            recipient.active && recipient.userIdentifier != originatorIdentifier
          }
          .map { recipient -> recipient.userIdentifier }
          .toSet()
}

/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.command.handler

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.REACTIVATED
import com.bosch.pt.iot.smartsite.application.security.AdminAuthorization
import com.bosch.pt.iot.smartsite.common.i18n.Key.PROJECT_VALIDATION_ERROR_USER_ALREADY_ASSIGNED
import com.bosch.pt.iot.smartsite.company.api.CompanyId
import com.bosch.pt.iot.smartsite.company.api.asCompanyId
import com.bosch.pt.iot.smartsite.company.model.Employee
import com.bosch.pt.iot.smartsite.company.repository.EmployeeRepository
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.command.api.AssignParticipantAsAdminCommand
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.ParticipantSnapshot
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.ParticipantSnapshotStore
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.toCommandHandler
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CSM
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.ACTIVE
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class AssignParticipantAsAdminCommandHandler(
    private val employeeRepository: EmployeeRepository,
    private val participantRepository: ParticipantRepository,
    private val projectRepository: ProjectRepository,
    private val participantSnapshotStore: ParticipantSnapshotStore,
    private val projectContextLocalEventBus: ProjectContextLocalEventBus,
) {

  @Trace
  @Transactional
  @AdminAuthorization
  open fun handle(command: AssignParticipantAsAdminCommand): ParticipantId {
    val project =
        requireNotNull(projectRepository.findOneByIdentifier(command.projectRef)) {
          "Project not found"
        }
    val employee =
        requireNotNull(employeeRepository.findOneByUserEmail(command.email)) {
          "Employee of user to assign not found"
        }

    return assignAsActive(project, employee)
  }

  private fun assignAsActive(
      project: Project,
      employee: Employee,
  ): ParticipantId {

    val existingParticipant =
        participantRepository.findOneByUserIdentifierAndCompanyIdentifierAndProjectIdentifier(
            employee.user!!.identifier!!, employee.company!!.identifier!!, project.identifier)

    if (existingParticipant == null) {
      return addActiveParticipant(
          ParticipantId(),
          project.identifier,
          employee.company!!.identifier!!.asCompanyId(),
          employee.user!!.identifier!!.asUserId(),
      )
    } else {

      if (existingParticipant.isActive()) {
        // Check if user is already an active participant
        throw PreconditionViolationException(PROJECT_VALIDATION_ERROR_USER_ALREADY_ASSIGNED)
      }

      // Reactivate the participant in case the user was already assigned for the same company
      return reactivateParticipant(existingParticipant.identifier)
    }
  }

  private fun reactivateParticipant(participantId: ParticipantId) =
      participantSnapshotStore
          .findOrFail(participantId)
          .toCommandHandler()
          .update { it.copy(role = CSM, status = ACTIVE) }
          .emitEvent(REACTIVATED)
          .ifSnapshotWasChanged()
          .to(projectContextLocalEventBus)
          .andReturnSnapshot()
          .identifier

  private fun addActiveParticipant(
      identifier: ParticipantId,
      projectRef: ProjectId,
      companyRef: CompanyId,
      userRef: UserId,
  ) =
      ParticipantSnapshot(
              identifier = identifier,
              projectRef = projectRef,
              companyRef = companyRef,
              userRef = userRef,
              role = CSM,
              status = ACTIVE)
          .toCommandHandler()
          .emitEvent(CREATED)
          .to(projectContextLocalEventBus)
          .andReturnSnapshot()
          .identifier
}

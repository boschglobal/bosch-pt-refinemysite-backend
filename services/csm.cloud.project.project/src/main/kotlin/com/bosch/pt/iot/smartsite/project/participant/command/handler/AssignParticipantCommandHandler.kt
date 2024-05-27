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
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.REACTIVATED
import com.bosch.pt.iot.smartsite.application.security.SecurityContextHelper
import com.bosch.pt.iot.smartsite.common.authorization.cache.AuthorizationCacheKey
import com.bosch.pt.iot.smartsite.common.authorization.cache.InvalidatesAuthorizationCache
import com.bosch.pt.iot.smartsite.common.i18n.Key.PROJECT_VALIDATION_ERROR_INACTIVE_PARTICIPANT_EXISTS_WITH_DIFFERENT_IDENTIFIER
import com.bosch.pt.iot.smartsite.common.i18n.Key.PROJECT_VALIDATION_ERROR_USER_ALREADY_ASSIGNED
import com.bosch.pt.iot.smartsite.company.api.CompanyId
import com.bosch.pt.iot.smartsite.company.api.asCompanyId
import com.bosch.pt.iot.smartsite.company.model.Employee
import com.bosch.pt.iot.smartsite.company.repository.EmployeeRepository
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectInvitationContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.participant.InvitationId
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.command.api.AssignParticipantCommand
import com.bosch.pt.iot.smartsite.project.participant.command.sideeffects.ParticipantMailService
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.InvitationSnapshot
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.ParticipantSnapshot
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.ParticipantSnapshotStore
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.toCommandHandler
import com.bosch.pt.iot.smartsite.project.participant.facade.job.InvitationExpirationJob.Companion.EXPIRE_AFTER_DAYS
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.ACTIVE
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.INVITED
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.VALIDATION
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.InvitationRepository
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.user.repository.UserRepository
import datadog.trace.api.Trace
import java.time.LocalDateTime.now
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class AssignParticipantCommandHandler(
    private val employeeRepository: EmployeeRepository,
    private val participantRepository: ParticipantRepository,
    private val projectRepository: ProjectRepository,
    private val invitationRepository: InvitationRepository,
    private val userRepository: UserRepository,
    private val participantMailService: ParticipantMailService,
    private val participantSnapshotStore: ParticipantSnapshotStore,
    private val projectContextLocalEventBus: ProjectContextLocalEventBus,
    private val projectInvitationContextLocalEventBus: ProjectInvitationContextLocalEventBus
) {

  @Trace
  @Transactional
  @PreAuthorize(
      "@participantAuthorizationComponent.hasAssignPermissionOnParticipantsOfProject(#command.projectRef)")
  @InvalidatesAuthorizationCache
  open fun handle(
      @AuthorizationCacheKey("identifier") command: AssignParticipantCommand
  ): ParticipantId {
    val project = projectRepository.findOneByIdentifier(command.projectRef)!!
    val employee = employeeRepository.findOneByUserEmail(command.email)

    return if (employee == null) {
      userRepository.findOneByEmail(command.email).let {
        when (it == null) {
          true ->
              assignAsInvited(
                  command.identifier ?: ParticipantId(),
                  command.projectRef,
                  command.email,
                  command.role)
          false ->
              assignAsInValidation(
                  command.identifier ?: ParticipantId(),
                  command.projectRef,
                  it.identifier!!.asUserId(),
                  command.role)
        }
      }
    } else {
      assignAsActive(project, command.identifier, employee, command.role)
    }
  }

  private fun assignAsInvited(
      participantId: ParticipantId,
      projectRef: ProjectId,
      email: String,
      role: ParticipantRoleEnum
  ): ParticipantId {

    val participantSnapshot =
        ParticipantSnapshot(
                identifier = participantId, projectRef = projectRef, role = role, status = INVITED)
            .toCommandHandler()
            .checkPrecondition { userNotYetInvited(projectRef, email) }
            .onFailureThrow(PROJECT_VALIDATION_ERROR_USER_ALREADY_ASSIGNED)
            .emitEvent(ParticipantEventEnumAvro.CREATED)
            .to(projectContextLocalEventBus)
            .andReturnSnapshot()

    InvitationSnapshot(
            identifier = InvitationId(),
            projectRef = projectRef,
            participantRef = participantId,
            email = email)
        .toCommandHandler()
        .emitEvent(InvitationEventEnumAvro.CREATED)
        .to(projectInvitationContextLocalEventBus)
        .withSideEffects { sendInvitationMail(projectRef, email) }

    return participantSnapshot.identifier
  }

  private fun assignAsInValidation(
      participantId: ParticipantId,
      projectRef: ProjectId,
      userRef: UserId,
      role: ParticipantRoleEnum
  ): ParticipantId =
      ParticipantSnapshot(
              identifier = participantId,
              projectRef = projectRef,
              userRef = userRef,
              role = role,
              status = VALIDATION)
          .toCommandHandler()
          .checkPrecondition { userNotYetAssigned(projectRef, userRef) }
          .onFailureThrow(PROJECT_VALIDATION_ERROR_USER_ALREADY_ASSIGNED)
          .emitEvent(ParticipantEventEnumAvro.CREATED)
          .to(projectContextLocalEventBus)
          .andReturnSnapshot()
          .identifier

  private fun assignAsActive(
      project: Project,
      participantId: ParticipantId?,
      employee: Employee,
      role: ParticipantRoleEnum
  ): ParticipantId {

    val existingParticipant =
        participantRepository.findOneByUserIdentifierAndCompanyIdentifierAndProjectIdentifier(
            employee.user!!.identifier!!, employee.company!!.identifier!!, project.identifier)

    // Check if user is already assigned
    if (existingParticipant != null) {
      if (existingParticipant.isActive()) {
        throw PreconditionViolationException(PROJECT_VALIDATION_ERROR_USER_ALREADY_ASSIGNED)
      } else if (participantId != null && existingParticipant.identifier != participantId) {
        // Check if no inactive user exists with a different identifier than the given one
        throw PreconditionViolationException(
            PROJECT_VALIDATION_ERROR_INACTIVE_PARTICIPANT_EXISTS_WITH_DIFFERENT_IDENTIFIER)
      }
    }

    // Reactivate the participant in case the user was already assigned for the same company
    return if (existingParticipant != null) {
      reactiveParticipant(existingParticipant.identifier, role)
    } else {
      addActiveParticipant(
          participantId ?: ParticipantId(),
          project.identifier,
          employee.company!!.identifier!!.asCompanyId(),
          employee.user!!.identifier!!.asUserId(),
          role)
    }
  }

  private fun reactiveParticipant(participantId: ParticipantId, role: ParticipantRoleEnum) =
      participantSnapshotStore
          .findOrFail(participantId)
          .toCommandHandler()
          .update { it.copy(role = role, status = ACTIVE) }
          .emitEvent(REACTIVATED)
          .ifSnapshotWasChanged()
          .to(projectContextLocalEventBus)
          .withSideEffects { sendParticipantAddedMail(it.projectRef, participantId) }
          .andReturnSnapshot()
          .identifier

  private fun addActiveParticipant(
      identifier: ParticipantId,
      projectRef: ProjectId,
      companyRef: CompanyId,
      userRef: UserId,
      role: ParticipantRoleEnum
  ) =
      ParticipantSnapshot(
              identifier = identifier,
              projectRef = projectRef,
              companyRef = companyRef,
              userRef = userRef,
              role = role,
              status = ACTIVE)
          .toCommandHandler()
          .emitEvent(ParticipantEventEnumAvro.CREATED)
          .to(projectContextLocalEventBus)
          .withSideEffects { sendParticipantAddedMail(projectRef, identifier) }
          .andReturnSnapshot()
          .identifier

  private fun userNotYetInvited(projectRef: ProjectId, email: String) =
      !invitationRepository.existsByProjectIdentifierAndEmail(projectRef, email)

  private fun userNotYetAssigned(projectRef: ProjectId, userRef: UserId) =
      !participantRepository.existsByUserIdentifierAndProjectIdentifier(
          userRef.toUuid(), projectRef)

  private fun sendInvitationMail(projectRef: ProjectId, email: String) {

    val invitingParticipant =
        participantRepository.findOneByUserIdentifierAndProjectIdentifierAndActiveTrue(
            SecurityContextHelper.getInstance().getCurrentUser().identifier!!, projectRef)!!

    participantMailService.sendParticipantInvited(
        projectRef, email, now().plusDays(EXPIRE_AFTER_DAYS), invitingParticipant)
  }

  private fun sendParticipantAddedMail(projectRef: ProjectId, participantId: ParticipantId) {

    val participant = participantRepository.findOneByIdentifier(participantId)!!

    val invitingParticipant =
        participantRepository.findOneByUserIdentifierAndProjectIdentifierAndActiveTrue(
            SecurityContextHelper.getInstance().getCurrentUser().identifier!!, projectRef)!!

    participantMailService.sendParticipantAdded(projectRef, participant, invitingParticipant)
  }
}

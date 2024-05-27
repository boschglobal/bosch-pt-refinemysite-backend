/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.project.authorization

import com.bosch.pt.iot.smartsite.application.security.AuthorizationUtils.getCurrentUser
import com.bosch.pt.iot.smartsite.application.security.AuthorizationUtils.hasRoleAdmin
import com.bosch.pt.iot.smartsite.application.security.SecurityContextHelper
import com.bosch.pt.iot.smartsite.company.boundary.EmployeeQueryService
import com.bosch.pt.iot.smartsite.company.model.EmployeeRoleEnum
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CSM
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantAuthorizationRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
open class ProjectAuthorizationComponent(
    private val participantAuthorizationRepository: ParticipantAuthorizationRepository,
    private val employeeService: EmployeeQueryService
) {

  open fun hasCreateAndAssignTaskPermissionOnProject(
      projectIdentifier: ProjectId,
      assigneeIdentifier: ParticipantId?
  ): Boolean =
      hasCreateAndAssignTaskPermissionOnProjectInternal(
          projectIdentifier, assigneeIdentifier != null)

  open fun hasCreatePermissionOnProject(): Boolean =
      employeeService.findOneWithRolesByUser(getCurrentUser()).let {
        it != null && it.roles!!.contains(EmployeeRoleEnum.CSM)
      }

  open fun hasUpdatePermissionOnProject(projectIdentifier: ProjectId): Boolean =
      participantAuthorizationRepository.getParticipantOfCurrentUser(projectIdentifier).let {
        it != null && it.role == CSM
      }

  open fun getProjectsWithUpdatePermission(projectIdentifiers: Set<ProjectId>): Set<ProjectId> =
      participantAuthorizationRepository
          .getParticipantsOfCurrentUser(projectIdentifiers)
          .filter { it.role == CSM }
          .map { it.projectIdentifier }
          .toSet()

  open fun hasDeletePermissionOnProject(projectIdentifier: ProjectId) =
      getProjectsWithDeletePermission(setOf(projectIdentifier)).contains(projectIdentifier)

  open fun getProjectsWithDeletePermission(projectIdentifiers: Set<ProjectId>): Set<ProjectId> {
    return if (hasRoleAdmin()) {
      projectIdentifiers
    } else {
      participantAuthorizationRepository
          .getParticipantsOfCurrentUser(projectIdentifiers)
          .filter { it.role == CSM }
          .map { it.projectIdentifier }
          .toSet()
    }
  }

  open fun hasReadPermissionOnProject(projectIdentifier: ProjectId?) =
      isCurrentUserActiveParticipantOfProject(projectIdentifier)

  open fun hasReadPermissionOnProjectIncludingAdmin(projectIdentifier: ProjectId?) =
      hasRoleAdmin() || isCurrentUserActiveParticipantOfProject(projectIdentifier)

  open fun hasReadPermissionOnProjects(projectIdentifiers: Set<ProjectId>) =
      getProjectsWithReadPermissions(projectIdentifiers).containsAll(projectIdentifiers)

  open fun hasCreateTaskPermissionOnProject(projectIdentifier: ProjectId?) =
      isCurrentUserActiveParticipantOfProject(projectIdentifier)

  open fun hasAssignPermissionOnProject(projectIdentifier: ProjectId?) =
      isCurrentUserActiveParticipantOfProject(projectIdentifier)

  open fun hasOpenPermissionOnProject(projectIdentifier: ProjectId?) =
      isCurrentUserActiveParticipantOfProject(projectIdentifier)

  open fun hasCopyPermissionOnProject(projectIdentifier: ProjectId): Boolean {
    val participant =
        participantAuthorizationRepository.getParticipantOfCurrentUser(projectIdentifier)

    // Only Employees with role CSM are allowed to create a project
    val employee =
        employeeService.findOneByUser(SecurityContextHelper.getInstance().getCurrentUser())
    val employeeIsCsm = employee?.roles?.contains(EmployeeRoleEnum.CSM) == true

    return participant?.role == CSM && employeeIsCsm
  }

  private fun isCurrentUserActiveParticipantOfProject(projectIdentifier: ProjectId?): Boolean =
      projectIdentifier?.let {
        participantAuthorizationRepository.getParticipantOfCurrentUser(projectIdentifier)
      } != null

  private fun getProjectsWithReadPermissions(projectIdentifiers: Set<ProjectId>): Set<ProjectId> =
      participantAuthorizationRepository
          .getParticipantsOfCurrentUser(projectIdentifiers)
          .map { it.projectIdentifier }
          .toSet()

  private fun hasCreateAndAssignTaskPermissionOnProjectInternal(
      projectIdentifier: ProjectId,
      hasAssignee: Boolean
  ): Boolean =
      (hasCreateTaskPermissionOnProject(projectIdentifier) &&
          (!hasAssignee || hasAssignPermissionOnProject(projectIdentifier)))
}

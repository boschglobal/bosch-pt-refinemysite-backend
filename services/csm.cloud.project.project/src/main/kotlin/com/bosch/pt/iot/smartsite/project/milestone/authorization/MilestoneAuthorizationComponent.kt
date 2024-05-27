/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.authorization

import com.bosch.pt.iot.smartsite.application.security.SecurityContextHelper
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.CRAFT
import com.bosch.pt.iot.smartsite.project.milestone.shared.repository.MilestoneRepository
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CR
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CSM
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.FM
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantAuthorizationRepository
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.authorization.ProjectAuthorizationComponent
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
open class MilestoneAuthorizationComponent(
    val projectAuthorizationComponent: ProjectAuthorizationComponent,
    val milestoneRepository: MilestoneRepository,
    val participantRepository: ParticipantRepository,
    val participantAuthorizationRepository: ParticipantAuthorizationRepository
) {

  open fun hasCreateMilestonePermissionOnProject(
      projectIdentifier: ProjectId,
      milestoneType: MilestoneTypeEnum
  ) =
      if (milestoneType == CRAFT) {
        isCurrentUserActiveParticipantOfProject(projectIdentifier)
      } else {
        isCurrentUserCsmOfProject(projectIdentifier)
      }

  open fun hasViewPermissionOnMilestone(milestoneIdentifier: MilestoneId) =
      milestoneRepository.findProjectIdentifierByIdentifier(milestoneIdentifier).let {
        it != null && isCurrentUserActiveParticipantOfProject(it)
      }

  open fun hasViewPermissionsOnMilestonesOfProject(projectIdentifier: ProjectId) =
      isCurrentUserActiveParticipantOfProject(projectIdentifier)

  open fun hasViewPermissionsOnMilestonesOfProject(
      identifiers: Set<MilestoneId>,
      projectIdentifier: ProjectId
  ) =
      identifiers.isEmpty() ||
          filterMilestonesWithViewPermission(identifiers, projectIdentifier)
              .containsAll(identifiers)

  open fun hasUpdateAndDeletePermissionOnMilestone(milestoneIdentifier: MilestoneId): Boolean {
    val milestone =
        milestoneRepository.findWithDetailsByIdentifier(milestoneIdentifier) ?: return false
    return when {
      isCurrentUserCsmOfProject(milestone.project.identifier) -> true
      isCurrentUserCompanyRepresentativeOfProject(milestone.project.identifier) ->
          isMilestoneACraftMilestoneAndCreatedByOwnCompany(
              milestone.project.identifier, milestoneIdentifier)
      isCurrentUserForemanOfProject(milestone.project.identifier) ->
          isMilestoneACraftMilestoneAndCreatedByCurrentUser(milestoneIdentifier)
      else -> false
    }
  }

  open fun hasReschedulePermissionOnProject(projectIdentifier: ProjectId): Boolean =
      projectIdentifier.let {
        participantAuthorizationRepository.getParticipantOfCurrentUser(projectIdentifier)?.role ==
            CSM
      }

  open fun filterMilestonesWithViewPermission(
      identifiers: Set<MilestoneId>,
      projectIdentifier: ProjectId
  ): Set<MilestoneId> {
    if (identifiers.isEmpty() || !isCurrentUserActiveParticipantOfProject(projectIdentifier)) {
      return emptySet()
    }
    return milestoneRepository.findMilestoneIdentifiersByIdentifierInAndProjectIdentifier(
        identifiers, projectIdentifier)
  }

  open fun filterMilestonesWithUpdateAndDeletePermission(
      milestones: Set<Milestone>
  ): Set<MilestoneId> {
    val milestoneIdentifiers = milestones.map { milestone -> milestone.identifier }.toSet()
    val projectIdentifiers = milestones.map { milestone -> milestone.project.identifier }.toSet()
    assert(projectIdentifiers.size == 1)
    return when {
      isCurrentUserCsmOfProject(projectIdentifiers.first()) -> milestoneIdentifiers
      isCurrentUserCompanyRepresentativeOfProject(projectIdentifiers.first()) ->
          filterCraftMilestonesCreatedByOwnCompany(projectIdentifiers.first(), milestoneIdentifiers)
      isCurrentUserForemanOfProject(projectIdentifiers.first()) ->
          filterCraftMilestonesCreatedByCurrentUser(milestoneIdentifiers)
      else -> emptySet()
    }
  }

  open fun getProjectsWithCreateAnyMilestonePermissions(
      projectIdentifiers: Set<ProjectId>
  ): Set<ProjectId> =
      participantAuthorizationRepository
          .getParticipantsOfCurrentUser(projectIdentifiers)
          .filter { it.role == CSM }
          .map { it.projectIdentifier }
          .toSet()

  open fun getProjectsWithCreateCraftMilestonePermissions(
      projectIdentifiers: Set<ProjectId>
  ): Set<ProjectId> =
      participantAuthorizationRepository
          .getParticipantsOfCurrentUser(projectIdentifiers)
          .map { it.projectIdentifier }
          .toSet()

  private fun isMilestoneACraftMilestoneAndCreatedByCurrentUser(milestoneIdentifier: MilestoneId) =
      filterCraftMilestonesCreatedByCurrentUser(setOf(milestoneIdentifier)).isNotEmpty()

  private fun isMilestoneACraftMilestoneAndCreatedByOwnCompany(
      projectIdentifier: ProjectId,
      milestoneIdentifier: MilestoneId
  ) =
      filterCraftMilestonesCreatedByOwnCompany(projectIdentifier, setOf(milestoneIdentifier))
          .isNotEmpty()

  private fun filterCraftMilestonesCreatedByOwnCompany(
      projectIdentifier: ProjectId,
      milestoneIdentifiers: Set<MilestoneId>
  ): Set<MilestoneId> {
    val currentUsersParticipant =
        participantAuthorizationRepository.getParticipantOfCurrentUser(projectIdentifier)
            ?: return emptySet()
    val userIdentifiersOfOwnCompaniesParticipants =
        participantRepository.findIdentifiersOfUsersParticipatingInProjectForGivenCompany(
            projectIdentifier, currentUsersParticipant.companyIdentifier)
    return milestoneRepository.filterMilestonesCreatedByGivenSetOfUsers(
        CRAFT, milestoneIdentifiers, userIdentifiersOfOwnCompaniesParticipants)
  }

  private fun filterCraftMilestonesCreatedByCurrentUser(milestoneIdentifiers: Set<MilestoneId>) =
      milestoneRepository.filterMilestonesCreatedByGivenSetOfUsers(
          CRAFT,
          milestoneIdentifiers,
          setOf(SecurityContextHelper.getInstance().getCurrentUser().identifier!!))

  private fun isCurrentUserCsmOfProject(projectIdentifier: ProjectId): Boolean =
      participantAuthorizationRepository.getParticipantOfCurrentUser(projectIdentifier)?.role == CSM

  private fun isCurrentUserCompanyRepresentativeOfProject(projectIdentifier: ProjectId): Boolean =
      participantAuthorizationRepository.getParticipantOfCurrentUser(projectIdentifier)?.role == CR

  private fun isCurrentUserForemanOfProject(projectIdentifier: ProjectId): Boolean =
      participantAuthorizationRepository.getParticipantOfCurrentUser(projectIdentifier)?.role == FM

  private fun isCurrentUserActiveParticipantOfProject(projectIdentifier: ProjectId?) =
      projectIdentifier != null &&
          participantAuthorizationRepository.getParticipantOfCurrentUser(projectIdentifier) != null
}

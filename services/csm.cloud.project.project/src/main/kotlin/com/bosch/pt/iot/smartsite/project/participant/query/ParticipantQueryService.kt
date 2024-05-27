/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.query

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.api.asUuidIds
import com.bosch.pt.iot.smartsite.application.security.AdminAuthorization
import com.bosch.pt.iot.smartsite.application.security.AuthorizationUtils
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.common.repository.SortCriteriaFilter.filterAndTranslate
import com.bosch.pt.iot.smartsite.common.uuid.toUuids
import com.bosch.pt.iot.smartsite.company.api.CompanyId
import com.bosch.pt.iot.smartsite.company.api.asCompanyId
import com.bosch.pt.iot.smartsite.company.model.Company
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CR
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CSM
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.FM
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import datadog.trace.api.Trace
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class ParticipantQueryService(private val participantRepository: ParticipantRepository) {

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  open fun findParticipantsWithRole(
      projectIdentifiers: Set<ProjectId>,
      role: ParticipantRoleEnum
  ): Map<ProjectId, List<Participant>> {
    val projectToParticipantsWithRoleMap: MutableMap<ProjectId, MutableList<Participant>> =
        HashMap()

    participantRepository
        .findAllByProjectIdentifierInAndRoleAndActiveTrue(projectIdentifiers, role)
        .forEach {
          val participantProjectIdentifier = it.project!!.identifier

          if (projectToParticipantsWithRoleMap.containsKey(participantProjectIdentifier)) {
            projectToParticipantsWithRoleMap[participantProjectIdentifier]!!.add(it)
          } else {
            projectToParticipantsWithRoleMap[participantProjectIdentifier] =
                ArrayList<Participant>().apply { add(it) }
          }
        }
    return projectToParticipantsWithRoleMap
  }

  @Trace
  @PreAuthorize("@participantAuthorizationComponent.hasReadPermissionOnParticipant(#participantId)")
  @Transactional(readOnly = true)
  open fun findParticipantWithDetails(participantId: ParticipantId): Participant? =
      participantRepository.findOneWithDetailsByIdentifier(participantId)

  @Trace
  @AdminAuthorization
  @Transactional(readOnly = true)
  open fun findParticipantWithDetailsAsAdmin(participantId: ParticipantId): Participant? =
      participantRepository.findOneWithDetailsByIdentifier(participantId)

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  open fun findActiveAndInactiveParticipants(
      userIdentifiers: Set<UserId>,
      projectIdentifiers: Set<ProjectId>
  ): Map<UserId, MutableMap<ProjectId, Participant>> {
    if (userIdentifiers.isEmpty()) {
      return emptyMap()
    }

    val map: MutableMap<UserId, MutableMap<ProjectId, Participant>> = HashMap()
    participantRepository
        .findAllByProjectIdentifierInAndUserIdentifierIn(
            projectIdentifiers, userIdentifiers.asUuidIds())
        .forEach {
          val userIdentifier = it.user!!.identifier!!.asUserId()
          val participantsByProjectId = map.computeIfAbsent(userIdentifier) { HashMap() }
          val projectIdentifier = it.project!!.identifier
          val existingParticipant = participantsByProjectId[projectIdentifier]

          if (existingParticipant == null ||
              it.isActive() ||
              (!existingParticipant.isActive() &&
                  isLastModifiedDateNewer(it, existingParticipant))) {
            participantsByProjectId[projectIdentifier] = it
          }
        }

    return map
  }

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  open fun findActiveAndInactiveParticipants(
      projectIdentifier: ProjectId,
      userIdentifiers: Set<UserId>
  ): Map<UserId, Participant> {
    if (userIdentifiers.isEmpty()) {
      return emptyMap()
    }

    val map: MutableMap<UserId, Participant> = HashMap()
    participantRepository
        .findAllByProjectIdentifierAndUserIdentifierIn(
            projectIdentifier, userIdentifiers.asUuidIds())
        .forEach {
          val userIdentifier = it.user!!.identifier!!.asUserId()
          val existingParticipant = map[userIdentifier]

          if (existingParticipant == null ||
              it.isActive() ||
              (!existingParticipant.isActive() &&
                  isLastModifiedDateNewer(it, existingParticipant))) {
            map[userIdentifier] = it
          }
        }

    return map
  }

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  open fun findAllParticipants(
      userIdentifiers: Set<UserId>,
      projectIdentifiers: Set<ProjectId>
  ): Map<UserId, Map<ProjectId, Participant>> {
    return if (userIdentifiers.isEmpty()) {
      emptyMap()
    } else {
      participantRepository
          .findAllByProjectIdentifierInAndUserIdentifierInAndActiveTrue(
              projectIdentifiers, userIdentifiers.toUuids())
          .groupingBy { it.user!!.identifier!!.asUserId() }
          .aggregate { _, accumulator: MutableMap<ProjectId, Participant>?, element, first ->
            if (first) {
              val map = HashMap<ProjectId, Participant>()
              map[element.project!!.identifier] = element
              map
            } else {
              accumulator!![element.project!!.identifier] = element
              accumulator
            }
          }
    }
  }

  @Trace
  @PreAuthorize(
      "@participantAuthorizationComponent.hasReadPermissionOnParticipantsOfProject(#projectIdentifier)")
  @Transactional(readOnly = true)
  open fun findAllParticipants(
      projectIdentifier: ProjectId,
      status: Set<ParticipantStatusEnum>?,
      companyIdentifier: CompanyId?,
      roles: Set<ParticipantRoleEnum>?,
      pageable: Pageable
  ): Page<Participant> {
    val translatedPageable = filterAndTranslate(pageable, PARTICIPANT_ALLOWED_SORTING_PROPERTIES)
    val orderedParticipantIdentifiers =
        participantRepository.findAllParticipantIdentifiersByFilters(
            projectIdentifier,
            status?.isEmpty() ?: true,
            status,
            companyIdentifier?.toUuid(),
            roles?.isEmpty() ?: true,
            roles,
            translatedPageable)

    val participants =
        participantRepository
            .findAllWithDetailsByIdentifierIn(orderedParticipantIdentifiers.content)
            .sortedWith(
                Comparator.comparingInt { left ->
                  orderedParticipantIdentifiers.content.indexOf(left.identifier)
                })

    return PageImpl(
        participants,
        orderedParticipantIdentifiers.pageable,
        orderedParticipantIdentifiers.totalElements)
  }

  @Trace
  @PreAuthorize(
      "@participantAuthorizationComponent.hasReadPermissionOnParticipantsOfProjects(#projectIdentifier)")
  @Transactional(readOnly = true)
  open fun findAllAssignableParticipants(
      projectIdentifier: ProjectId,
      companyIdentifier: CompanyId?,
      pageable: Pageable
  ): Page<Participant> {
    val translatedPageable = filterAndTranslate(pageable, PARTICIPANT_ALLOWED_SORTING_PROPERTIES)

    // Load current users project participant to check for participant's role
    val currentUser = AuthorizationUtils.getCurrentUser()
    val currentParticipant =
        participantRepository.findOneByUserIdentifierAndProjectIdentifierAndActiveTrue(
            currentUser.identifier!!, projectIdentifier)

    return when (currentParticipant!!.role) {
      CSM ->
          findAssignableParticipantsForCsm(projectIdentifier, companyIdentifier, translatedPageable)
      CR ->
          findAssignableParticipantsForCr(
              projectIdentifier, companyIdentifier, translatedPageable, currentParticipant)
      FM ->
          findAssignableParticipantsForFm(companyIdentifier, translatedPageable, currentParticipant)
      else -> Page.empty(pageable)
    }
  }

  @Trace
  @PreAuthorize(
      "@participantAuthorizationComponent.hasReadPermissionOnParticipantsOfProjects(#projectIdentifier)")
  @Transactional(readOnly = true)
  open fun findAllAssignableCompanies(
      projectIdentifier: ProjectId,
      includeInactive: Boolean,
      pageable: Pageable
  ): Page<Company> {
    val translatedPageable =
        filterAndTranslate(pageable, PARTICIPANT_COMPANIES_ALLOWED_SORTING_PROPERTIES, false)

    // Load current users project participant to check for participant's role
    val currentUser = AuthorizationUtils.getCurrentUser()
    val currentParticipant =
        participantRepository.findOneByUserIdentifierAndProjectIdentifierAndActiveTrue(
            currentUser.identifier!!, projectIdentifier)

    return if (currentParticipant == null) {
      PageImpl(emptyList<Company>())
    } else if (FM === currentParticipant.role || CR === currentParticipant.role) {
      PageImpl(listOf(currentParticipant.company))
    } else {
      participantRepository.findAllParticipantCompanies(
          projectIdentifier, includeInactive, translatedPageable)
    }
  }

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  open fun countActiveParticipantsPerProject(
      projectIdentifiers: Set<ProjectId>
  ): Map<ProjectId, Long> =
      participantRepository.countActiveParticipantsPerProject(projectIdentifiers).associate {
        it.projectIdentifier to it.numberOfParticipants
      }

  @NoPreAuthorize
  @Transactional(readOnly = true)
  open fun findAllParticipantsByCompanyAndUser(
      companyIdentifier: CompanyId,
      userIdentifier: UserId
  ): List<Participant> =
      participantRepository.findAllByCompanyIdentifierAndUserIdentifier(
          companyIdentifier.toUuid(), userIdentifier.toUuid())

  @NoPreAuthorize
  @Transactional(readOnly = true)
  open fun countAllByCompany(company: Company): Int =
      participantRepository.countAllByCompany(company)

  private fun findAllActiveWithDetailsByIdentifiers(
      identifiers: Page<ParticipantId>
  ): Page<Participant> =
      PageImpl(
          participantRepository.findAllWithDetailsByIdentifierInAndActiveTrue(
              identifiers.content, identifiers.pageable.getSortOr(Sort.unsorted())),
          identifiers.pageable,
          identifiers.totalElements)

  private fun findAssignableParticipantsForCsm(
      projectIdentifier: ProjectId,
      companyIdentifier: CompanyId?,
      pageable: Pageable
  ): Page<Participant> {
    val identifiers =
        participantRepository.findAllParticipantIdentifiersByOptionalCompany(
            projectIdentifier, companyIdentifier?.toUuid(), pageable)
    return findAllActiveWithDetailsByIdentifiers(identifiers)
  }

  private fun findAssignableParticipantsForCr(
      projectIdentifier: ProjectId,
      companyIdentifier: CompanyId?,
      pageable: Pageable,
      participantOfCr: Participant
  ): Page<Participant> {
    val participantCompanyIdentifier: CompanyId =
        participantOfCr.company!!.identifier!!.asCompanyId()
    val identifiers =
        if (companyIdentifier == null || companyIdentifier == participantCompanyIdentifier)
            participantRepository.findAllParticipantIdentifiersByOptionalCompany(
                projectIdentifier, participantCompanyIdentifier.toUuid(), pageable)
        else Page.empty(pageable)

    return findAllActiveWithDetailsByIdentifiers(identifiers)
  }

  private fun findAssignableParticipantsForFm(
      companyIdentifier: CompanyId?,
      pageable: Pageable,
      participantOfFm: Participant
  ): Page<Participant> =
      if (companyIdentifier == null ||
          companyIdentifier == participantOfFm.company!!.identifier!!.asCompanyId()) {
        findAllActiveWithDetailsByIdentifiers(
            PageImpl(listOf(participantOfFm.identifier), pageable, 1))
      } else {
        Page.empty(pageable)
      }

  private fun isLastModifiedDateNewer(
      participant: Participant,
      existingParticipant: Participant
  ): Boolean =
      existingParticipant.lastModifiedDate.isPresent &&
          participant.lastModifiedDate.isPresent &&
          participant.lastModifiedDate.get().isAfter(existingParticipant.lastModifiedDate.get())

  companion object {
    val PARTICIPANT_ALLOWED_SORTING_PROPERTIES =
        mapOf(
            "user.firstName" to "user.firstName",
            "user.lastName" to "user.lastName",
            "company.displayName" to "company.name",
            "role" to "role",
            "status" to "status",
            "email" to "email")

    val PARTICIPANT_COMPANIES_ALLOWED_SORTING_PROPERTIES = mapOf("displayName" to "company.name")
  }
}

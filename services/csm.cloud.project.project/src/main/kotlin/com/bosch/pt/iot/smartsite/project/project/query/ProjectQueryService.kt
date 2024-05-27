/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.project.query

import com.bosch.pt.iot.smartsite.application.security.AdminAuthorization
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.common.repository.SortCriteriaFilter.filterAndTranslate
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.model.dto.NameByIdentifierDto
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.user.authorization.boundary.AdminUserAuthorizationService
import datadog.trace.api.Trace
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class ProjectQueryService(
    private val projectRepository: ProjectRepository,
    private val adminUserAuthorizationService: AdminUserAuthorizationService
) {

  @Trace
  @Transactional(readOnly = true)
  @AdminAuthorization
  open fun searchProjectsForFilters(
      title: String?,
      company: String?,
      creator: String?,
      pageable: Pageable
  ): Page<Project> =
      adminUserAuthorizationService.getRestrictedCountries().let {
        projectRepository.findProjectsForFilters(
            title,
            company,
            creator,
            it,
            filterAndTranslate(pageable, PROJECT_SEARCH_ALLOWED_SORTING_PROPERTIES))
      }

  @Trace
  @NoPreAuthorize(usedByController = true)
  @Transactional(readOnly = true)
  open fun findAllProjectsForCurrentUser(pageable: Pageable): Page<Project> {
    val translatedPageable = filterAndTranslate(pageable, PROJECTS_ALLOWED_SORTING_PROPERTIES)
    return projectRepository.findAllWhereCurrentUserIsActiveParticipant(translatedPageable)
  }

  @Trace
  @PreAuthorize(
      "@projectAuthorizationComponent.hasReadPermissionOnProjectIncludingAdmin(#identifier)")
  @Transactional(readOnly = true)
  open fun findOneByIdentifier(identifier: ProjectId): Project? =
      projectRepository.findOneByIdentifier(identifier)

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  open fun findOldestCsmsCompanyNameByProjectIdentifiers(
      projectIdentifiers: Collection<ProjectId>
  ): Map<ProjectId, NameByIdentifierDto> {
    val datesByProjectIdentifiers =
        projectRepository.findOldestCsmCreatedDatesByProjectIdentifiers(projectIdentifiers)
    return projectRepository.findCompanyNamesByProjectIdentifiers(datesByProjectIdentifiers)
  }

  companion object {

    val PROJECT_SEARCH_ALLOWED_SORTING_PROPERTIES =
        mapOf(
            "title" to "title",
            "createdBy.lastName" to "u.createdBy.lastName",
            "createdBy.firstName" to "u.createdBy.firstName",
            "createdDate" to "createdDate",
            "company" to "c.name")

    val PROJECTS_ALLOWED_SORTING_PROPERTIES: Map<String, String> =
        mapOf(
            "title" to "title",
            "createdBy.displayName" to "createdBy.displayName",
            "company.displayName" to "company.displayName")
  }
}

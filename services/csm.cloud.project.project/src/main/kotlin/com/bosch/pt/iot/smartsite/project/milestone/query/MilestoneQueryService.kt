/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.query

import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.common.repository.SortCriteriaFilter.filterAndTranslate
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.dto.SearchMilestonesDto
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone
import com.bosch.pt.iot.smartsite.project.milestone.shared.repository.MilestoneRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import datadog.trace.api.Trace
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class MilestoneQueryService(private val milestoneRepository: MilestoneRepository) {

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  open fun countByProjectIdentifier(projectIdentifier: ProjectId): Long =
      milestoneRepository.countByProjectIdentifier(projectIdentifier)

  @Trace
  @Transactional(readOnly = true)
  @PreAuthorize("@milestoneAuthorizationComponent.hasViewPermissionOnMilestone(#identifier)")
  open fun find(identifier: MilestoneId): Milestone =
      milestoneRepository.findWithDetailsByIdentifier(identifier)!!

  @Trace
  @Transactional(readOnly = true)
  @PreAuthorize(
      "@milestoneAuthorizationComponent.hasViewPermissionsOnMilestonesOfProject(#identifiers, #projectIdentifier)")
  open fun findBatch(identifiers: Set<MilestoneId>, projectIdentifier: ProjectId) =
      milestoneRepository.findAllWithDetailsByIdentifierInAndProjectIdentifier(
          identifiers, projectIdentifier)

  @Trace
  @Transactional(readOnly = true)
  @PreAuthorize(
      "@milestoneAuthorizationComponent.hasViewPermissionsOnMilestonesOfProject(#filters.projectIdentifier)")
  open fun findMilestonesWithDetailsForFilters(
      filters: SearchMilestonesDto,
      pageable: Pageable
  ): Page<Milestone> {

    val translatedPageable =
        filterAndTranslate(pageable, MILESTONE_SEARCH_ALLOWED_SORTING_PROPERTIES)

    val filtersDto = filters.toMilestoneFilterDto()
    val orderedIdentifiers =
        milestoneRepository.findMilestoneIdentifiersForFilters(filtersDto, translatedPageable)
    val totalCount = milestoneRepository.countAllForFilters(filtersDto)
    val milestonesWithDetails =
        milestoneRepository.findAllWithDetailsByIdentifierIn(orderedIdentifiers)

    return PageImpl(
        milestonesWithDetails.sortedWith(
            Comparator.comparingInt { orderedIdentifiers.indexOf(it.identifier) }),
        pageable,
        totalCount)
  }

  companion object {
    val MILESTONE_SEARCH_ALLOWED_SORTING_PROPERTIES =
        mapOf(
            "createdDate" to "createdDate",
            "date" to "date",
            "header" to "header",
            "position" to "position",
            "type" to "type",
            "workArea" to "workArea")
  }
}

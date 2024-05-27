/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.boundary

import com.bosch.pt.iot.smartsite.common.repository.SortCriteriaFilter.filterAndTranslate
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.relation.boundary.dto.RelationDto.RelationElementDto
import com.bosch.pt.iot.smartsite.project.relation.model.Relation
import com.bosch.pt.iot.smartsite.project.relation.model.RelationTypeEnum
import com.bosch.pt.iot.smartsite.project.relation.repository.RelationRepository
import com.bosch.pt.iot.smartsite.project.relation.repository.converter.RelationSortProperty.ID
import com.bosch.pt.iot.smartsite.project.relation.repository.dto.RelationFilterDto
import datadog.trace.api.Trace
import java.util.Comparator.comparingInt
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class RelationSearchService(private val relationRepository: RelationRepository) {

  @Trace
  @Transactional(readOnly = true)
  @PreAuthorize("@relationAuthorizationComponent.hasViewPermissionOnProject(#projectIdentifier)")
  open fun search(
      projectIdentifier: ProjectId,
      types: Collection<RelationTypeEnum>,
      sources: Collection<RelationElementDto>,
      targets: Collection<RelationElementDto>,
      pageable: Pageable
  ): Page<Relation> {
    val filters =
        RelationFilterDto(
            types = types.toSet(),
            sources = sources.map { it.toRelationElement() }.toSet(),
            targets = targets.map { it.toRelationElement() }.toSet(),
            projectIdentifier = projectIdentifier)

    val translatedPageable =
        filterAndTranslate(pageable, RELATION_SEARCH_ALLOWED_SORTING_PROPERTIES)

    val orderedIdentifiers = relationRepository.findForFilters(filters, translatedPageable)
    val totalCount = relationRepository.countForFilters(filters)
    val relations = relationRepository.findAllWithDetailsByIdentifierIn(orderedIdentifiers)

    return PageImpl(
        relations.sortedWith(comparingInt { orderedIdentifiers.indexOf(it.identifier) }),
        pageable,
        totalCount)
  }

  companion object {
    val RELATION_SEARCH_ALLOWED_SORTING_PROPERTIES = mapOf("id" to ID)
  }
}

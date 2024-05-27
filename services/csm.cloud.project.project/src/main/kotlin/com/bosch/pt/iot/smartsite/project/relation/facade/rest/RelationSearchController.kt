/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ListResponseResource
import com.bosch.pt.iot.smartsite.common.repository.PageableDefaults.DEFAULT_PAGE_SIZE
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.relation.boundary.RelationSearchService
import com.bosch.pt.iot.smartsite.project.relation.facade.rest.RelationController.Companion.PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.relation.facade.rest.resource.request.FilterRelationResource
import com.bosch.pt.iot.smartsite.project.relation.facade.rest.resource.response.RelationResource
import com.bosch.pt.iot.smartsite.project.relation.facade.rest.resource.response.factory.RelationListResourceFactory
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@ApiVersion
@RestController
open class RelationSearchController(
    private val relationSearchService: RelationSearchService,
    private val relationListResourceFactory: RelationListResourceFactory
) {

  @PostMapping(RELATION_SEARCH_ENDPOINT)
  open fun search(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId,
      @RequestBody @Valid filter: FilterRelationResource,
      @PageableDefault(size = DEFAULT_PAGE_SIZE) pageable: Pageable
  ): ResponseEntity<ListResponseResource<RelationResource>> {
    val relations =
        relationSearchService.search(
            projectIdentifier, filter.types, filter.sources, filter.targets, pageable)

    val listResource = relationListResourceFactory.build(relations, pageable, projectIdentifier)

    return ResponseEntity.ok().body(listResource)
  }

  companion object {
    const val RELATION_SEARCH_ENDPOINT = "/projects/{projectId}/relations/search"
  }
}

/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.project.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.iot.smartsite.common.repository.PageableDefaults.DEFAULT_PAGE_SIZE
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.request.SearchProjectListResource
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectListResource
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.factory.ProjectListResourceFactory
import com.bosch.pt.iot.smartsite.project.project.query.ProjectQueryService
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@ApiVersion
@RestController
open class ProjectSearchController(
    private val projectQueryService: ProjectQueryService,
    private val projectListResourceFactory: ProjectListResourceFactory
) {

  @PostMapping(PROJECTS_SEARCH_ENDPOINT)
  open fun search(
      @RequestBody search: SearchProjectListResource,
      @PageableDefault(sort = ["title"], direction = ASC, size = DEFAULT_PAGE_SIZE)
      pageable: Pageable
  ): ResponseEntity<ProjectListResource> =
      ResponseEntity.ok(
          projectListResourceFactory.build(
              projectQueryService.searchProjectsForFilters(
                  search.title, search.company, search.creator, pageable)))

  companion object {
    const val PROJECTS_SEARCH_ENDPOINT = "/projects/search"
  }
}

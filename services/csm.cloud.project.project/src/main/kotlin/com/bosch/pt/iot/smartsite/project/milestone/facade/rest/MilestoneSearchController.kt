/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ListResponseResource
import com.bosch.pt.iot.smartsite.common.repository.PageableDefaults.DEFAULT_PAGE_SIZE
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.request.FilterMilestoneListResource
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.response.MilestoneResource
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.response.factory.MilestoneListResourceFactory
import com.bosch.pt.iot.smartsite.project.milestone.query.MilestoneQueryService
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@ApiVersion
@RestController
open class MilestoneSearchController(
    private val milestoneQueryService: MilestoneQueryService,
    private val listResourceFactory: MilestoneListResourceFactory
) {

  @PostMapping(MILESTONES_SEARCH_ENDPOINT)
  open fun search(
      @PathVariable("projectId") projectId: ProjectId,
      @RequestBody filter: FilterMilestoneListResource,
      @PageableDefault(
          sort = ["date", "header", "workArea", "position"],
          direction = ASC,
          size = DEFAULT_PAGE_SIZE)
      pageable: Pageable
  ): ResponseEntity<ListResponseResource<MilestoneResource>> {
    val milestones =
        milestoneQueryService.findMilestonesWithDetailsForFilters(
            filters = filter.toSearchMilestonesDto(projectId), pageable = pageable)
    return ResponseEntity(
        listResourceFactory.build(milestones = milestones, projectRef = projectId), OK)
  }

  companion object {
    const val MILESTONES_SEARCH_ENDPOINT = "/projects/{projectId}/milestones/search"
  }
}

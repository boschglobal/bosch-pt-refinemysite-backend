/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils.getCurrentApiVersionPrefix
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.quickfilter.boundary.QuickFilterService
import com.bosch.pt.iot.smartsite.project.quickfilter.domain.QuickFilterId
import com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.resources.request.SaveQuickFilterResource
import com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.resources.response.QuickFilterListResource
import com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.resources.response.QuickFilterResource
import com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.resources.response.factory.QuickFilterListResourceFactory
import com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.resources.response.factory.QuickFilterResourceFactory
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@ApiVersion(from = 5)
@RestController
open class QuickFilterController(
    private val quickFilterListResourceFactory: QuickFilterListResourceFactory,
    private val quickFilterResourceFactory: QuickFilterResourceFactory,
    private val quickFilterService: QuickFilterService
) {

  @PostMapping(QUICK_FILTERS_ENDPOINT)
  open fun createQuickFilter(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId,
      @RequestBody @Valid saveQuickFilterResource: SaveQuickFilterResource
  ): ResponseEntity<QuickFilterResource> {

    val identifier =
        quickFilterService.save(saveQuickFilterResource.toQuickFilterDto(projectIdentifier))

    val quickFilter = quickFilterService.findOne(identifier, projectIdentifier)

    val location =
        ServletUriComponentsBuilder.fromCurrentContextPath()
            .path(getCurrentApiVersionPrefix())
            .path(QUICK_FILTERS_ENDPOINT)
            .buildAndExpand(projectIdentifier)
            .toUri()

    return ResponseEntity.created(location)
        .eTag(quickFilter.version.toString())
        .body(quickFilterResourceFactory.build(quickFilter, projectIdentifier))
  }

  @GetMapping(QUICK_FILTERS_ENDPOINT)
  open fun findQuickFilters(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId
  ): ResponseEntity<QuickFilterListResource> =
      ResponseEntity.ok(
          quickFilterListResourceFactory.build(
              quickFilterService.findAllForCurrentUser(projectIdentifier), projectIdentifier))

  @PutMapping(QUICK_FILTER_ENDPOINT)
  open fun updateQuickFilter(
      @PathVariable(PATH_VARIABLE_QUICK_FILTER_ID) identifier: QuickFilterId,
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId,
      @RequestBody @Valid saveQuickFilterResource: SaveQuickFilterResource,
      @Parameter(`in` = ParameterIn.HEADER, name = "If-Match") eTag: ETag
  ): ResponseEntity<QuickFilterResource> {

    quickFilterService.update(
        identifier, saveQuickFilterResource.toQuickFilterDto(projectIdentifier), eTag)

    val quickFilter = quickFilterService.findOne(identifier, projectIdentifier)

    return ResponseEntity.ok()
        .eTag(quickFilter.version.toString())
        .body(quickFilterResourceFactory.build(quickFilter, projectIdentifier))
  }

  @DeleteMapping(QUICK_FILTER_ENDPOINT)
  open fun deleteQuickFilter(
      @PathVariable(PATH_VARIABLE_QUICK_FILTER_ID) identifier: QuickFilterId,
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId
  ): ResponseEntity<Void> {
    quickFilterService.delete(identifier, projectIdentifier)

    return ResponseEntity.noContent().build()
  }

  companion object {
    const val PATH_VARIABLE_PROJECT_ID = "projectId"
    const val PATH_VARIABLE_QUICK_FILTER_ID = "quickFilterId"

    const val QUICK_FILTER_ENDPOINT = "/projects/{projectId}/quickfilters/{quickFilterId}"
    const val QUICK_FILTERS_ENDPOINT = "/projects/{projectId}/quickfilters"
  }
}

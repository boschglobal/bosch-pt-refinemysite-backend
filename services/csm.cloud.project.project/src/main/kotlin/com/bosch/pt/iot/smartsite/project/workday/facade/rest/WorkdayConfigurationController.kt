/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workday.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.toEtagString
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.workday.command.handler.UpdateWorkdayConfigurationCommandHandler
import com.bosch.pt.iot.smartsite.project.workday.facade.rest.resource.request.UpdateWorkdayConfigurationResource
import com.bosch.pt.iot.smartsite.project.workday.facade.rest.resource.response.WorkdayConfigurationResource
import com.bosch.pt.iot.smartsite.project.workday.facade.rest.resource.response.factory.WorkdayConfigurationResourceFactory
import com.bosch.pt.iot.smartsite.project.workday.query.WorkdayConfigurationQueryService
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@ApiVersion
@RestController
open class WorkdayConfigurationController(
    private val workdayConfigurationQueryService: WorkdayConfigurationQueryService,
    private val workdayConfigurationResourceFactory: WorkdayConfigurationResourceFactory,
    private val updateWorkdayConfigurationCommandHandler: UpdateWorkdayConfigurationCommandHandler
) {

  @GetMapping(WORKDAY_BY_PROJECT_ID_ENDPOINT)
  open fun find(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId
  ): ResponseEntity<WorkdayConfigurationResource> {
    val workdayConfiguration =
        workdayConfigurationQueryService.findOneByProjectIdentifier(projectIdentifier)

    return ResponseEntity.ok()
        .eTag(workdayConfiguration.toEtagString())
        .body(workdayConfigurationResourceFactory.build(workdayConfiguration))
  }

  @PutMapping(WORKDAY_BY_PROJECT_ID_ENDPOINT)
  open fun updateWorkdayConfiguration(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId,
      @RequestBody @Valid updateWorkdayConfigurationResource: UpdateWorkdayConfigurationResource,
      @Parameter(`in` = ParameterIn.HEADER, name = "If-Match") eTag: ETag
  ): ResponseEntity<WorkdayConfigurationResource> {

    updateWorkdayConfigurationCommandHandler.handle(
        updateWorkdayConfigurationResource.toCommand(projectIdentifier, eTag))

    val updatedWorkdayConfiguration =
        workdayConfigurationQueryService.findOneByProjectIdentifier(projectIdentifier)

    return ResponseEntity.ok()
        .eTag(updatedWorkdayConfiguration.toEtagString())
        .body(workdayConfigurationResourceFactory.build(updatedWorkdayConfiguration))
  }

  companion object {
    const val PATH_VARIABLE_PROJECT_ID = "projectId"

    const val WORKDAY_BY_PROJECT_ID_ENDPOINT = "/projects/{projectId}/workdays"
  }
}

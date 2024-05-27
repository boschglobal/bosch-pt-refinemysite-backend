/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.rfv.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.BatchResponseResource
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.rfv.boundary.RfvService
import com.bosch.pt.iot.smartsite.project.rfv.facade.rest.resource.request.UpdateRfvResource
import com.bosch.pt.iot.smartsite.project.rfv.facade.rest.resource.response.RfvResource
import com.bosch.pt.iot.smartsite.project.rfv.facade.rest.resource.response.factory.RfvListResourceFactory
import com.bosch.pt.iot.smartsite.project.rfv.facade.rest.resource.response.factory.RfvResourceFactory
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@ApiVersion
@RestController
open class RfvController(
    private val rfvService: RfvService,
    private val rfvResourceFactory: RfvResourceFactory,
    private val rfvListResourceFactory: RfvListResourceFactory
) {

  @PutMapping(RFVS_BY_PROJECT_ID)
  open fun update(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectId: ProjectId,
      @RequestBody @Valid updateRfvResource: UpdateRfvResource?,
  ): ResponseEntity<RfvResource> {
    val rfv = rfvService.update(updateRfvResource!!.toDto(projectId))
    return ResponseEntity.ok().body(rfvResourceFactory.build(projectId, rfv))
  }

  @GetMapping(RFVS_BY_PROJECT_ID)
  open fun findAll(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectId: ProjectId,
  ): ResponseEntity<BatchResponseResource<RfvResource>> {
    val rfvs = rfvService.findAll(projectId)
    return ResponseEntity.ok().body(rfvListResourceFactory.build(projectId, rfvs))
  }

  companion object {
    const val RFVS_BY_PROJECT_ID = "/projects/{projectId}/rfvs"
    const val PATH_VARIABLE_PROJECT_ID = "projectId"
  }
}

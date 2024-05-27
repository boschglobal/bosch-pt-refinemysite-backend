/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.milestone.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.BatchResponseResource
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.toEtagString
import com.bosch.pt.iot.smartsite.project.milestone.command.api.DeleteMilestoneCommand
import com.bosch.pt.iot.smartsite.project.milestone.command.handler.CreateMilestoneCommandHandler
import com.bosch.pt.iot.smartsite.project.milestone.command.handler.DeleteMilestoneCommandHandler
import com.bosch.pt.iot.smartsite.project.milestone.command.handler.UpdateMilestoneCommandHandler
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.request.CreateMilestoneResource
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.request.UpdateMilestoneResource
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.response.MilestoneResource
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.response.factory.MilestoneBatchResourceFactory
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.response.factory.MilestoneResourceFactory
import com.bosch.pt.iot.smartsite.project.milestone.query.MilestoneQueryService
import com.bosch.pt.iot.smartsite.project.project.ProjectId
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

@ApiVersion
@RestController
open class MilestoneController(
    private val createMilestoneCommandHandler: CreateMilestoneCommandHandler,
    private val updateMilestoneCommandHandler: UpdateMilestoneCommandHandler,
    private val deleteMilestoneCommandHandler: DeleteMilestoneCommandHandler,
    private val milestoneQueryService: MilestoneQueryService,
    private val milestoneResourceFactory: MilestoneResourceFactory,
    private val milestoneBatchResourceFactory: MilestoneBatchResourceFactory
) {

  @PostMapping(path = [MILESTONES_ENDPOINT, MILESTONE_BY_MILESTONE_ID_ENDPOINT])
  open fun create(
      @PathVariable(value = PATH_VARIABLE_MILESTONE_ID, required = false)
      milestoneId: MilestoneId? = null,
      @RequestBody @Valid createMilestoneResource: CreateMilestoneResource
  ): ResponseEntity<MilestoneResource> =
      createMilestoneCommandHandler
          .handle(createMilestoneResource.toCommand(milestoneId))
          .let { milestoneQueryService.find(it) }
          .let {
            val location =
                ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path(LinkUtils.getCurrentApiVersionPrefix())
                    .path(MILESTONE_BY_MILESTONE_ID_ENDPOINT)
                    .buildAndExpand(it.identifier)
                    .toUri()

            ResponseEntity.created(location)
                .eTag(it.toEtagString())
                .body(milestoneResourceFactory.build(it))
          }

  @GetMapping(MILESTONE_BY_MILESTONE_ID_ENDPOINT)
  open fun find(
      @PathVariable(PATH_VARIABLE_MILESTONE_ID) milestoneId: MilestoneId
  ): ResponseEntity<MilestoneResource> =
      milestoneQueryService.find(milestoneId).let {
        ResponseEntity.ok().eTag(it.toEtagString()).body(milestoneResourceFactory.build(it))
      }

  @PostMapping(FIND_BATCH_ENDPOINT)
  open fun findBatch(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectId: ProjectId,
      @RequestBody @Valid batchRequestResource: BatchRequestResource
  ): ResponseEntity<BatchResponseResource<MilestoneResource>> =
      milestoneQueryService
          .findBatch(batchRequestResource.ids.map { it.asMilestoneId() }.toSet(), projectId)
          .let { ResponseEntity.ok().body(milestoneBatchResourceFactory.build(it, projectId)) }

  @PutMapping(MILESTONE_BY_MILESTONE_ID_ENDPOINT)
  open fun update(
      @PathVariable(PATH_VARIABLE_MILESTONE_ID) milestoneId: MilestoneId,
      @RequestBody @Valid updateMilestoneResource: UpdateMilestoneResource,
      @Parameter(`in` = ParameterIn.HEADER, name = "If-Match") eTag: ETag
  ): ResponseEntity<MilestoneResource> =
      updateMilestoneCommandHandler
          .handle(updateMilestoneResource.toCommand(milestoneId, eTag))
          .let { milestoneQueryService.find(it) }
          .let {
            ResponseEntity.ok().eTag(it.toEtagString()).body(milestoneResourceFactory.build(it))
          }

  @DeleteMapping(MILESTONE_BY_MILESTONE_ID_ENDPOINT)
  open fun delete(
      @PathVariable(PATH_VARIABLE_MILESTONE_ID) milestoneId: MilestoneId,
      @Parameter(`in` = ParameterIn.HEADER, name = "If-Match") eTag: ETag
  ): ResponseEntity<Void> {
    deleteMilestoneCommandHandler.handle(
        DeleteMilestoneCommand(identifier = milestoneId, version = eTag.toVersion()))
    return ResponseEntity.noContent().build()
  }

  companion object {
    const val PATH_VARIABLE_PROJECT_ID = "projectId"
    const val PATH_VARIABLE_MILESTONE_ID = "milestoneId"

    const val MILESTONES_ENDPOINT = "/projects/milestones"
    const val MILESTONE_BY_MILESTONE_ID_ENDPOINT = "/projects/milestones/{milestoneId}"

    const val FIND_BATCH_ENDPOINT = "/projects/{projectId}/milestones/batch/find"
  }
}

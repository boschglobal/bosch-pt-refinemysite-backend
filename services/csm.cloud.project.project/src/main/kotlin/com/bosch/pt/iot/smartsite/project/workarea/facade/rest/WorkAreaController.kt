/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils.getCurrentApiVersionPrefix
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.toEtagString
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.workarea.command.api.DeleteWorkAreaCommand
import com.bosch.pt.iot.smartsite.project.workarea.command.handler.CreateWorkAreaCommandHandler
import com.bosch.pt.iot.smartsite.project.workarea.command.handler.DeleteWorkAreaCommandHandler
import com.bosch.pt.iot.smartsite.project.workarea.command.handler.UpdateWorkAreaCommandHandler
import com.bosch.pt.iot.smartsite.project.workarea.command.handler.list.ReorderWorkAreaListCommandHandler
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.request.CreateWorkAreaResource
import com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.request.UpdateWorkAreaListResource
import com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.request.UpdateWorkAreaResource
import com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.response.WorkAreaListResource
import com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.response.WorkAreaResource
import com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.response.factory.WorkAreaListResourceFactory
import com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.response.factory.WorkAreaResourceFactory
import com.bosch.pt.iot.smartsite.project.workarea.query.WorkAreaListQueryService
import com.bosch.pt.iot.smartsite.project.workarea.query.WorkAreaQueryService
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath

@ApiVersion
@RestController
open class WorkAreaController(
    private val createWorkAreaCommandHandler: CreateWorkAreaCommandHandler,
    private val updateWorkAreaCommandHandler: UpdateWorkAreaCommandHandler,
    private val deleteWorkAreaCommandHandler: DeleteWorkAreaCommandHandler,
    private val reorderWorkAreaListCommandHandler: ReorderWorkAreaListCommandHandler,
    private val workAreaQueryService: WorkAreaQueryService,
    private val workAreaListQueryService: WorkAreaListQueryService,
    private val workAreaResourceFactory: WorkAreaResourceFactory,
    private val workAreaListResourceFactory: WorkAreaListResourceFactory
) {

  @PostMapping(WORKAREAS_ENDPOINT, WORKAREA_BY_WORKAREA_ID_ENDPOINT)
  open fun createWorkArea(
      @PathVariable(value = PATH_VARIABLE_WORKAREA_ID, required = false)
      workAreaIdentifier: WorkAreaId?,
      @RequestBody @Valid createWorkAreaResource: CreateWorkAreaResource,
      @Parameter(`in` = ParameterIn.HEADER, name = "If-Match") eTag: ETag
  ): ResponseEntity<WorkAreaListResource> {

    createWorkAreaCommandHandler.handle(
        createWorkAreaResource.toCommand(workAreaIdentifier, eTag.toVersion()))

    val workAreaList =
        workAreaListQueryService.findOneWithDetailsByProjectIdentifier(
            createWorkAreaResource.projectId)

    val location =
        fromCurrentContextPath()
            .path(getCurrentApiVersionPrefix())
            .path(WORKAREAS_BY_PROJECT_ID_ENDPOINT)
            .buildAndExpand(createWorkAreaResource.projectId)
            .toUri()

    return ResponseEntity.created(location)
        .eTag(workAreaList.toEtagString())
        .body(workAreaListResourceFactory.build(workAreaList, workAreaList.project))
  }

  @GetMapping(WORKAREAS_BY_PROJECT_ID_ENDPOINT)
  open fun findAllByProjectIdentifier(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId
  ): ResponseEntity<WorkAreaListResource> {
    val workAreaList =
        workAreaListQueryService.findOneWithDetailsByProjectIdentifier(projectIdentifier)

    return ResponseEntity.ok()
        .eTag(workAreaList.toEtagString())
        .body(workAreaListResourceFactory.build(workAreaList, workAreaList.project))
  }

  @GetMapping(WORKAREA_BY_WORKAREA_ID_ENDPOINT)
  open fun findOneByIdentifier(
      @PathVariable(PATH_VARIABLE_WORKAREA_ID) workAreaIdentifier: WorkAreaId
  ): ResponseEntity<WorkAreaResource> {
    val workArea = workAreaQueryService.findOneWithDetailsByIdentifier(workAreaIdentifier)

    return if (workArea == null) ResponseEntity.notFound().build()
    else
        ResponseEntity.ok()
            .eTag(workArea.toEtagString())
            .body(workAreaResourceFactory.build(workArea, workArea.project))
  }

  @PutMapping(WORKAREA_BY_WORKAREA_ID_ENDPOINT)
  open fun updateWorkArea(
      @PathVariable(PATH_VARIABLE_WORKAREA_ID) workAreaIdentifier: WorkAreaId,
      @RequestBody @Valid updateWorkAreaResource: UpdateWorkAreaResource,
      @Parameter(`in` = ParameterIn.HEADER, name = "If-Match") eTag: ETag
  ): ResponseEntity<WorkAreaResource> {
    updateWorkAreaCommandHandler.handle(updateWorkAreaResource.toCommand(workAreaIdentifier, eTag))

    val workArea = workAreaQueryService.findOneWithDetailsByIdentifier(workAreaIdentifier)!!
    return ResponseEntity.ok()
        .eTag(workArea.toEtagString())
        .body(workAreaResourceFactory.build(workArea, workArea.project))
  }

  @PutMapping(WORKAREAS_ENDPOINT)
  open fun updateWorkAreaList(
      @RequestBody @Valid updateWorkAreaListResource: UpdateWorkAreaListResource,
      @Parameter(`in` = ParameterIn.HEADER, name = "If-Match") eTag: ETag
  ): ResponseEntity<WorkAreaListResource> {
    reorderWorkAreaListCommandHandler.handle(updateWorkAreaListResource.toCommand(eTag))

    val workArea =
        workAreaQueryService.findOneWithDetailsByIdentifier(updateWorkAreaListResource.workAreaId)!!

    val workAreaList =
        workAreaListQueryService.findOneWithDetailsByProjectIdentifier(workArea.project.identifier)

    return ResponseEntity.ok()
        .eTag(workAreaList.toEtagString())
        .body(workAreaListResourceFactory.build(workAreaList, workAreaList.project))
  }

  @DeleteMapping(WORKAREA_BY_WORKAREA_ID_ENDPOINT)
  open fun deleteWorkArea(
      @PathVariable(PATH_VARIABLE_WORKAREA_ID) workAreaIdentifier: WorkAreaId,
      @Parameter(`in` = ParameterIn.HEADER, name = "If-Match") eTag: ETag
  ): ResponseEntity<*> {
    val workArea = workAreaQueryService.findOneWithDetailsByIdentifier(workAreaIdentifier)!!

    deleteWorkAreaCommandHandler.handle(
        DeleteWorkAreaCommand(identifier = workAreaIdentifier, version = eTag.toVersion()))

    val workAreaList =
        workAreaListQueryService.findOneWithDetailsByProjectIdentifier(workArea.project.identifier)

    return ResponseEntity.ok()
        .eTag(workAreaList.toEtagString())
        .body(workAreaListResourceFactory.build(workAreaList, workAreaList.project))
  }

  companion object {
    const val PATH_VARIABLE_PROJECT_ID = "projectId"
    const val PATH_VARIABLE_WORKAREA_ID = "workAreaId"

    const val WORKAREAS_ENDPOINT = "/projects/workareas"
    const val WORKAREAS_BY_PROJECT_ID_ENDPOINT = "/projects/{projectId}/workareas"
    const val WORKAREA_BY_WORKAREA_ID_ENDPOINT = "/projects/workareas/{workAreaId}"
  }
}

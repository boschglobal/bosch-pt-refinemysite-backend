/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils.getCurrentApiVersionPrefix
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.toEtagString
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.command.api.DeleteProjectCraftCommand
import com.bosch.pt.iot.smartsite.project.projectcraft.command.handler.CreateProjectCraftCommandHandler
import com.bosch.pt.iot.smartsite.project.projectcraft.command.handler.DeleteProjectCraftCommandHandler
import com.bosch.pt.iot.smartsite.project.projectcraft.command.handler.ReorderProjectCraftCommandHandler
import com.bosch.pt.iot.smartsite.project.projectcraft.command.handler.UpdateProjectCraftCommandHandler
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.resource.request.ReorderProjectCraftResource
import com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.resource.request.SaveProjectCraftResource
import com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.resource.response.ProjectCraftListResource
import com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.resource.response.ProjectCraftResource
import com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.resource.response.factory.ProjectCraftListResourceFactory
import com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.resource.response.factory.ProjectCraftResourceFactory
import com.bosch.pt.iot.smartsite.project.projectcraft.query.ProjectCraftListQueryService
import com.bosch.pt.iot.smartsite.project.projectcraft.query.ProjectCraftQueryService
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.created
import org.springframework.http.ResponseEntity.ok
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
open class ProjectCraftController(
    private val createProjectCraftCommandHandler: CreateProjectCraftCommandHandler,
    private val updateProjectCraftCommandHandler: UpdateProjectCraftCommandHandler,
    private val reorderProjectCraftCommandHandler: ReorderProjectCraftCommandHandler,
    private val deleteProjectCraftCommandHandler: DeleteProjectCraftCommandHandler,
    private val projectCraftQueryService: ProjectCraftQueryService,
    private val projectCraftListQueryService: ProjectCraftListQueryService,
    private val projectCraftResourceFactory: ProjectCraftResourceFactory,
    private val projectCraftListResourceFactory: ProjectCraftListResourceFactory
) {

  @PostMapping(CRAFTS_ENDPOINT, CRAFT_BY_CRAFT_ID_ENDPOINT)
  open fun create(
      @PathVariable(value = PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId,
      @PathVariable(value = PATH_VARIABLE_PROJECT_CRAFT_ID, required = false)
      identifier: ProjectCraftId?,
      @RequestBody @Valid saveProjectCraftResource: SaveProjectCraftResource,
      @Parameter(`in` = HEADER, name = "If-Match") projectCraftListEtag: ETag
  ): ResponseEntity<ProjectCraftListResource> {

    createProjectCraftCommandHandler.handle(
        saveProjectCraftResource.toCommand(identifier, projectIdentifier, projectCraftListEtag))

    val projectCraftList = projectCraftListQueryService.findOneByProject(projectIdentifier)

    val location =
        fromCurrentContextPath()
            .path(getCurrentApiVersionPrefix())
            .path(CRAFTS_ENDPOINT)
            .buildAndExpand(projectIdentifier)
            .toUri()

    return created(location)
        .eTag(projectCraftList.toEtagString())
        .body(projectCraftListResourceFactory.build(projectCraftList, projectIdentifier))
  }

  @GetMapping(CRAFT_BY_CRAFT_ID_ENDPOINT)
  open fun find(
      @PathVariable(value = PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId,
      @PathVariable(value = PATH_VARIABLE_PROJECT_CRAFT_ID) identifier: ProjectCraftId
  ): ResponseEntity<ProjectCraftResource> {

    val projectCraft = projectCraftQueryService.findOneByIdentifier(identifier)

    return ok()
        .eTag(projectCraft.toEtagString())
        .body(projectCraftResourceFactory.build(projectCraft))
  }

  @GetMapping(CRAFTS_ENDPOINT)
  open fun findAll(
      @PathVariable(value = PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId
  ): ResponseEntity<ProjectCraftListResource> {

    val projectCraftList = projectCraftListQueryService.findOneByProject(projectIdentifier)

    return ok()
        .eTag(projectCraftList.toEtagString())
        .body(projectCraftListResourceFactory.build(projectCraftList, projectIdentifier))
  }

  @PutMapping(CRAFT_BY_CRAFT_ID_ENDPOINT)
  open fun update(
      @PathVariable(value = PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId,
      @PathVariable(value = PATH_VARIABLE_PROJECT_CRAFT_ID) identifier: ProjectCraftId,
      @RequestBody @Valid saveProjectCraftResource: SaveProjectCraftResource,
      @Parameter(`in` = HEADER, name = "If-Match") eTag: ETag
  ): ResponseEntity<ProjectCraftResource> {

    updateProjectCraftCommandHandler.handle(saveProjectCraftResource.toCommand(identifier, eTag))

    val projectCraft = projectCraftQueryService.findOneByIdentifier(identifier)

    return ok()
        .eTag(projectCraft.toEtagString())
        .body(projectCraftResourceFactory.build(projectCraft))
  }

  @PutMapping(CRAFTS_ENDPOINT)
  open fun reorder(
      @PathVariable(value = PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId,
      @RequestBody @Valid reorderProjectCraftResource: ReorderProjectCraftResource,
      @Parameter(`in` = HEADER, name = "If-Match") projectCraftListEtag: ETag
  ): ResponseEntity<ProjectCraftListResource> {

    reorderProjectCraftCommandHandler.handle(
        reorderProjectCraftResource.toCommand(projectIdentifier, projectCraftListEtag))

    val projectCraftList = projectCraftListQueryService.findOneByProject(projectIdentifier)

    return ok()
        .eTag(projectCraftList.toEtagString())
        .body(projectCraftListResourceFactory.build(projectCraftList, projectIdentifier))
  }

  @DeleteMapping(CRAFT_BY_CRAFT_ID_ENDPOINT)
  open fun delete(
      @PathVariable(value = PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId,
      @PathVariable(value = PATH_VARIABLE_PROJECT_CRAFT_ID) identifier: ProjectCraftId,
      @Parameter(`in` = HEADER, name = "If-Match") eTag: ETag
  ): ResponseEntity<ProjectCraftListResource> {

    deleteProjectCraftCommandHandler.handle(
        DeleteProjectCraftCommand(projectIdentifier, identifier, eTag.toVersion()))

    val projectCraftList = projectCraftListQueryService.findOneByProject(projectIdentifier)

    return ok()
        .eTag(projectCraftList.toEtagString())
        .body(projectCraftListResourceFactory.build(projectCraftList, projectIdentifier))
  }

  companion object {
    const val PATH_VARIABLE_PROJECT_ID = "projectId"
    const val PATH_VARIABLE_PROJECT_CRAFT_ID = "projectCraftId"

    const val CRAFTS_ENDPOINT = "/projects/{projectId}/crafts"
    const val CRAFT_BY_CRAFT_ID_ENDPOINT = "/projects/{projectId}/crafts/{projectCraftId}"
  }
}

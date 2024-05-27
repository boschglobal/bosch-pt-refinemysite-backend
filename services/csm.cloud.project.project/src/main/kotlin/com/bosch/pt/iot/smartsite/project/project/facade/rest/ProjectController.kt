/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.project.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils.getCurrentApiVersionPrefix
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.toEtagString
import com.bosch.pt.iot.smartsite.common.repository.PageableDefaults.DEFAULT_PAGE_SIZE
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.command.handler.CreateProjectCommandHandler
import com.bosch.pt.iot.smartsite.project.project.command.handler.UpdateProjectCommandHandler
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.request.DeleteProjectResource
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.request.SaveProjectResource
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectListResource
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectResource
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.factory.ProjectListResourceFactory
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.factory.ProjectResourceFactory
import com.bosch.pt.iot.smartsite.project.project.query.ProjectQueryService
import com.bosch.pt.iot.smartsite.project.project.shared.boundary.ProjectDeleteService
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.web.PageableDefault
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
open class ProjectController(
    private val projectListResourceFactory: ProjectListResourceFactory,
    private val projectResourceFactory: ProjectResourceFactory,
    private val createProjectCommandHandler: CreateProjectCommandHandler,
    private val updateProjectCommandHandler: UpdateProjectCommandHandler,
    private val projectQueryService: ProjectQueryService,
    private val projectDeleteService: ProjectDeleteService
) {

  @PostMapping(PROJECTS_ENDPOINT, PROJECT_BY_PROJECT_ID_ENDPOINT)
  open fun createProject(
      @PathVariable(value = PATH_VARIABLE_PROJECT_ID, required = false) projectId: ProjectId?,
      @RequestBody body: @Valid SaveProjectResource
  ): ResponseEntity<ProjectResource> {

    val projectIdentifier = createProjectCommandHandler.handle(body.toCommand(projectId))
    val project = projectQueryService.findOneByIdentifier(projectIdentifier)!!

    val location =
        fromCurrentContextPath()
            .path(getCurrentApiVersionPrefix())
            .path(PROJECT_BY_PROJECT_ID_ENDPOINT)
            .buildAndExpand(projectIdentifier)
            .toUri()

    return ResponseEntity.created(location)
        .eTag(project.toEtagString())
        .body(projectResourceFactory.build(project))
  }

  @PutMapping(PROJECT_BY_PROJECT_ID_ENDPOINT)
  open fun updateProject(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectId: ProjectId,
      @RequestBody body: @Valid SaveProjectResource,
      @Parameter(`in` = ParameterIn.HEADER, name = "If-Match") eTag: ETag
  ): ResponseEntity<ProjectResource> {
    updateProjectCommandHandler.handle(body.toCommand(projectId, eTag))
    val project = projectQueryService.findOneByIdentifier(projectId)!!
    return ResponseEntity.ok()
        .eTag(project.toEtagString())
        .body(projectResourceFactory.build(project))
  }

  @DeleteMapping(PROJECT_BY_PROJECT_ID_ENDPOINT)
  open fun deleteProject(
      @PathVariable(value = "projectId") projectId: ProjectId,
      @RequestBody @Valid deleteProjectResource: DeleteProjectResource
  ): ResponseEntity<Void> {
    projectDeleteService.validateProjectDeletion(projectId, deleteProjectResource.title)
    projectDeleteService.markAsDeletedAndSendEvent(projectId)
    return ResponseEntity.noContent().build()
  }

  @GetMapping(PROJECT_BY_PROJECT_ID_ENDPOINT)
  open fun findProjectById(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectId: ProjectId
  ): ResponseEntity<ProjectResource> {
    val project = projectQueryService.findOneByIdentifier(projectId)!!
    return ResponseEntity.ok()
        .eTag(project.toEtagString())
        .body(projectResourceFactory.build(project))
  }

  @GetMapping(PROJECTS_ENDPOINT)
  open fun findAllProjects(
      @PageableDefault(sort = ["title"], direction = ASC, size = DEFAULT_PAGE_SIZE)
      pageable: Pageable
  ): ResponseEntity<ProjectListResource> {
    val projectPage = projectQueryService.findAllProjectsForCurrentUser(pageable)
    return ResponseEntity.ok(projectListResourceFactory.build(projectPage))
  }

  companion object {
    const val PROJECTS_ENDPOINT = "/projects"
    const val PROJECT_BY_PROJECT_ID_ENDPOINT = "/projects/{projectId}"
    const val PATH_VARIABLE_PROJECT_ID = "projectId"
  }
}

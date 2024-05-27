/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils.getCurrentApiVersionPrefix
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.CreateBatchRequestResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.BatchResponseResource
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.toEtagString
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.relation.boundary.RelationService
import com.bosch.pt.iot.smartsite.project.relation.facade.rest.resource.request.CreateRelationResource
import com.bosch.pt.iot.smartsite.project.relation.facade.rest.resource.response.RelationResource
import com.bosch.pt.iot.smartsite.project.relation.facade.rest.resource.response.factory.RelationBatchResourceFactory
import com.bosch.pt.iot.smartsite.project.relation.facade.rest.resource.response.factory.RelationResourceFactory
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import java.util.UUID
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.lang.Nullable
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@ApiVersion
@RestController
open class RelationController(
    private val relationService: RelationService,
    private val relationResourceFactory: RelationResourceFactory,
    private val relationBatchResourceFactory: RelationBatchResourceFactory
) {

  @PostMapping(RELATIONS_ENDPOINT)
  open fun create(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId,
      @RequestBody resource: CreateRelationResource
  ): ResponseEntity<RelationResource> {
    val relationIdentifier = relationService.create(resource.toRelationDto(), projectIdentifier)
    val relation = relationService.find(relationIdentifier, projectIdentifier)
    val location = buildLocation(projectIdentifier, relation.identifier!!)

    return ResponseEntity.created(location)
        .eTag(relation.toEtagString())
        .body(relationResourceFactory.build(relation))
  }

  @PostMapping(CREATE_BATCH_ENDPOINT)
  open fun createBatch(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId,
      @RequestBody @Valid resource: CreateBatchRequestResource<CreateRelationResource>
  ): ResponseEntity<BatchResponseResource<RelationResource>> {
    val relationIdentifiers =
        relationService.createBatch(resource.items.map { it.toRelationDto() }, projectIdentifier)
    val relations = relationService.findBatch(relationIdentifiers, projectIdentifier)
    val batchResource = relationBatchResourceFactory.build(relations, projectIdentifier)

    return ResponseEntity.ok().body(batchResource)
  }

  @GetMapping(RELATION_BY_RELATION_ID_ENDPOINT)
  open fun find(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId,
      @PathVariable(PATH_VARIABLE_RELATION_ID) relationIdentifier: UUID
  ): ResponseEntity<RelationResource> {
    val relation = relationService.find(relationIdentifier, projectIdentifier)

    return ResponseEntity.ok()
        .eTag(relation.toEtagString())
        .body(relationResourceFactory.build(relation))
  }

  @PostMapping(FIND_BATCH_ENDPOINT)
  open fun findBatch(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId,
      @RequestBody @Valid resource: BatchRequestResource
  ): ResponseEntity<BatchResponseResource<RelationResource>> {
    val relations = relationService.findBatch(resource.ids, projectIdentifier)
    val batchResource = relationBatchResourceFactory.build(relations, projectIdentifier)

    return ResponseEntity.ok().body(batchResource)
  }

  @DeleteMapping(RELATION_BY_RELATION_ID_ENDPOINT)
  open fun delete(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId,
      @PathVariable(PATH_VARIABLE_RELATION_ID) relationIdentifier: UUID,
      @Parameter(`in` = ParameterIn.HEADER, name = "If-Match") @Nullable eTag: ETag? = null
  ): ResponseEntity<Void> {
    relationService.delete(relationIdentifier, projectIdentifier, eTag)

    return ResponseEntity.noContent().build()
  }

  private fun buildLocation(projectIdentifier: ProjectId, relationIdentifier: UUID) =
      ServletUriComponentsBuilder.fromCurrentContextPath()
          .path(getCurrentApiVersionPrefix())
          .path(RELATION_BY_RELATION_ID_ENDPOINT)
          .buildAndExpand(
              mapOf(
                  PATH_VARIABLE_PROJECT_ID to projectIdentifier,
                  PATH_VARIABLE_RELATION_ID to relationIdentifier))
          .toUri()

  companion object {
    const val PATH_VARIABLE_PROJECT_ID = "projectId"
    const val PATH_VARIABLE_RELATION_ID = "relationId"

    const val RELATIONS_ENDPOINT = "/projects/{projectId}/relations"
    const val RELATION_BY_RELATION_ID_ENDPOINT = "/projects/{projectId}/relations/{relationId}"

    const val CREATE_BATCH_ENDPOINT = "/projects/{projectId}/relations/batch/create"
    const val FIND_BATCH_ENDPOINT = "/projects/{projectId}/relations/batch/find"
  }
}

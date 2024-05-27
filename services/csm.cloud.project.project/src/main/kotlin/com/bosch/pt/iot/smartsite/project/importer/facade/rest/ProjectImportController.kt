/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.response.JobResponseResource
import com.bosch.pt.iot.smartsite.project.importer.boundary.ProjectImportService
import com.bosch.pt.iot.smartsite.project.importer.facade.job.submitter.ProjectImportJobSubmitter
import com.bosch.pt.iot.smartsite.project.importer.facade.rest.resource.request.ProjectImportAnalyzeResource
import com.bosch.pt.iot.smartsite.project.importer.facade.rest.resource.request.toAnalysisColumn
import com.bosch.pt.iot.smartsite.project.importer.facade.rest.resource.response.ProjectImportAnalysisResource
import com.bosch.pt.iot.smartsite.project.importer.facade.rest.resource.response.ProjectImportUploadResource
import com.bosch.pt.iot.smartsite.project.importer.facade.rest.resource.response.factory.ProjectImportResourceFactory
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@ApiVersion
@RestController
open class ProjectImportController(
    private val projectImportService: ProjectImportService,
    private val projectImportJobSubmitter: ProjectImportJobSubmitter,
    private val projectImportResourceFactory: ProjectImportResourceFactory
) {

  @PostMapping(UPLOAD_BY_PROJECT_ID_ENDPOINT)
  open fun upload(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId,
      @RequestParam("file") file: MultipartFile? = null
  ): ResponseEntity<ProjectImportUploadResource> {
    // file is nullable because of the hateoas link
    requireNotNull(file)
    val uploadResult =
        projectImportService.upload(
            projectIdentifier,
            file.bytes,
            file.originalFilename!!,
            file.contentType ?: APPLICATION_OCTET_STREAM_VALUE)

    return ResponseEntity.ok(projectImportResourceFactory.build(projectIdentifier, uploadResult))
  }

  @PostMapping(ANALYZE_BY_PROJECT_ID_ENDPOINT)
  open fun analyze(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId,
      @RequestBody(required = false) analyzeResource: ProjectImportAnalyzeResource?,
      @Parameter(`in` = ParameterIn.HEADER, name = "If-Match") eTag: ETag
  ): ResponseEntity<ProjectImportAnalysisResource> {
    val resource = analyzeResource ?: ProjectImportAnalyzeResource()
    val analysisResult =
        projectImportService.analyze(
            projectIdentifier,
            resource.readWorkAreasHierarchically ?: false,
            resource.craftColumn?.toAnalysisColumn(),
            resource.workAreaColumn?.toAnalysisColumn(),
            eTag)
    return ResponseEntity.ok(projectImportResourceFactory.build(projectIdentifier, analysisResult))
  }

  @PostMapping(IMPORT_BY_PROJECT_ID_ENDPOINT)
  open fun import(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId,
      @Parameter(`in` = ParameterIn.HEADER, name = "If-Match") eTag: ETag
  ): ResponseEntity<JobResponseResource> =
      ResponseEntity.accepted()
          .body(
              JobResponseResource(
                  projectImportJobSubmitter.enqueueImportJob(projectIdentifier, eTag).toString()))

  companion object {
    const val PATH_VARIABLE_PROJECT_ID = "projectId"

    const val IMPORT_BY_PROJECT_ID_ENDPOINT = "/projects/{projectId}/import"
    const val ANALYZE_BY_PROJECT_ID_ENDPOINT = "/projects/{projectId}/import/analyze"
    const val UPLOAD_BY_PROJECT_ID_ENDPOINT = "/projects/{projectId}/import/upload"
  }
}

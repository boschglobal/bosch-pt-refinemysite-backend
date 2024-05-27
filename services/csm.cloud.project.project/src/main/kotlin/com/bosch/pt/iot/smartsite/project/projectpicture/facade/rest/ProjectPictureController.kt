/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectpicture.facade.rest

import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.iot.smartsite.common.i18n.Key.ATTACHMENT_VALIDATION_ERROR_IOERROR
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectpicture.boundary.ProjectPictureService
import com.bosch.pt.iot.smartsite.project.projectpicture.facade.rest.resource.response.ProjectPictureResource
import com.bosch.pt.iot.smartsite.project.projectpicture.facade.rest.resource.response.factory.ProjectPictureResourceFactory
import java.io.BufferedInputStream
import java.io.IOException
import java.util.UUID
import org.apache.commons.io.IOUtils.toByteArray
import org.apache.commons.lang3.StringUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.FOUND
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentServletMapping

@ApiVersion
@RestController
open class ProjectPictureController(
    private val projectPictureService: ProjectPictureService,
    private val projectPictureResourceFactory: ProjectPictureResourceFactory
) {

  @PostMapping(PICTURE_BY_PROJECT_ID_ENDPOINT, PICTURE_BY_PROJECT_ID_AND_PICTURE_ID_ENDPOINT)
  open fun saveProjectPicture(
      @RequestParam("file") file: MultipartFile,
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectId: ProjectId,
      @PathVariable(name = PATH_VARIABLE_PICTURE_ID, required = false) projectPictureId: UUID?
  ): ResponseEntity<ProjectPictureResource> {
    var fileName = file.originalFilename
    if (StringUtils.isBlank(fileName)) {
      fileName = FILENAME_DEFAULT
    }

    val data = getRawData(file)
    projectPictureService.saveProjectPicture(data, fileName!!, projectId, projectPictureId)

    val savedProjectPicture = projectPictureService.findProjectPicture(projectId)
    val headers =
        HttpHeaders().apply {
          location =
              fromCurrentServletMapping()
                  .path(PICTURE_BY_PROJECT_ID_AND_PICTURE_ID_AND_SIZE_ENDPOINT)
                  .buildAndExpand(
                      savedProjectPicture.project!!.identifier,
                      savedProjectPicture.identifier,
                      "full")
                  .toUri()
        }

    return ResponseEntity(
        projectPictureResourceFactory.build(savedProjectPicture), headers, CREATED)
  }

  @GetMapping(PICTURE_BY_PROJECT_ID_ENDPOINT)
  open fun findProjectPictureMetadata(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectId: ProjectId
  ): ResponseEntity<ProjectPictureResource> =
      ResponseEntity(
          projectPictureResourceFactory.build(projectPictureService.findProjectPicture(projectId)),
          OK)

  @GetMapping(PICTURE_BY_PROJECT_ID_AND_PICTURE_ID_AND_SIZE_ENDPOINT)
  open fun findProjectPicture(
      @PathVariable(PATH_VARIABLE_PICTURE_ID) projectPictureId: UUID,
      @PathVariable(PATH_VARIABLE_SIZE) size: String
  ): ResponseEntity<Void> {
    val imageResolution = ImageResolution.valueOf(StringUtils.upperCase(size))
    val blobUrl = projectPictureService.generateBlobAccessUrl(projectPictureId, imageResolution)

    return ResponseEntity.status(FOUND).header("Location", blobUrl.toString()).build()
  }

  @DeleteMapping(PICTURE_BY_PROJECT_ID_ENDPOINT, PICTURE_BY_PROJECT_ID_AND_PICTURE_ID_ENDPOINT)
  open fun deleteProjectPicture(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectId: ProjectId,
      @PathVariable(name = PATH_VARIABLE_PICTURE_ID, required = false) projectPictureId: UUID?
  ): ResponseEntity<Void> {
    if (projectPictureId != null) {
      projectPictureService.deleteProjectPicture(projectPictureId)
    } else {
      projectPictureService.deleteProjectPictureByProject(projectId)
    }

    return ResponseEntity.noContent().build()
  }

  private fun getRawData(file: MultipartFile): ByteArray =
      try {
        BufferedInputStream(file.inputStream).use { inputStream -> toByteArray(inputStream) }
      } catch (ex: IOException) {
        throw PreconditionViolationException(ATTACHMENT_VALIDATION_ERROR_IOERROR, cause = ex)
      }

  companion object {
    const val FILENAME_DEFAULT = "project-picture.jpg"
    const val PICTURE_BY_PROJECT_ID_ENDPOINT = "/projects/{projectId}/picture"
    const val PICTURE_BY_PROJECT_ID_AND_PICTURE_ID_ENDPOINT =
        "/projects/{projectId}/picture/{projectPictureId}"
    const val PICTURE_BY_PROJECT_ID_AND_PICTURE_ID_AND_SIZE_ENDPOINT =
        "/projects/{projectId}/picture/{projectPictureId}/{size}"
    const val PATH_VARIABLE_PICTURE_ID = "projectPictureId"
    const val PATH_VARIABLE_PROJECT_ID = "projectId"
    const val PATH_VARIABLE_SIZE = "size"
  }
}

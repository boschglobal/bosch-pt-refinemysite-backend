/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.projectpicture.boundary

import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.FULL
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.ORIGINAL
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.SMALL
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.attachment.boundary.BlobStoreService
import com.bosch.pt.iot.smartsite.attachment.dto.ImageMetadataDto
import com.bosch.pt.iot.smartsite.common.i18n.Key.PROJECT_VALIDATION_ERROR_PROJECT_PICTURE_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.projectpicture.model.ProjectPicture
import com.bosch.pt.iot.smartsite.project.projectpicture.repository.ProjectPictureRepository
import datadog.trace.api.Trace
import java.net.URL
import java.util.TimeZone
import java.util.UUID
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.IdGenerator

@Service
open class ProjectPictureService(
    private val idGenerator: IdGenerator,
    private val blobStoreService: BlobStoreService,
    private val projectPictureRepository: ProjectPictureRepository,
    private val projectRepository: ProjectRepository,
) {

  @Trace
  @Transactional
  @PreAuthorize(
      "@projectPictureAuthorizationComponent.hasDeletePermissionOnPicture(#projectPictureIdentifier)")
  open fun deleteProjectPicture(projectPictureIdentifier: UUID) {
    val projectPicture =
        projectPictureRepository.findOneByIdentifier(projectPictureIdentifier)
            ?: throw AggregateNotFoundException(
                PROJECT_VALIDATION_ERROR_PROJECT_PICTURE_NOT_FOUND,
                projectPictureIdentifier.toString())
    deleteProjectPicture(projectPicture)
  }

  @Trace
  @Transactional
  @NoPreAuthorize
  open fun deleteProjectPictureWithoutAuthorization(projectPictureIdentifier: UUID) {
    projectPictureRepository.findOneByIdentifier(projectPictureIdentifier)?.let {
      deleteProjectPicture(it)
    }
  }

  private fun deleteProjectPicture(projectPicture: ProjectPicture) {
    projectPictureRepository.delete(projectPicture, DELETED)
    for (imageResolution in ImageResolution.values()) {
      blobStoreService.deleteImageIfExists(projectPicture, imageResolution)
    }
  }

  @Trace
  @Transactional
  @PreAuthorize("@projectAuthorizationComponent.hasUpdatePermissionOnProject(#projectIdentifier)")
  open fun deleteProjectPictureByProject(projectIdentifier: ProjectId) {
    val projectPicture =
        projectPictureRepository.findOneByProjectIdentifier(projectIdentifier)
            ?: throw AggregateNotFoundException(
                PROJECT_VALIDATION_ERROR_PROJECT_PICTURE_NOT_FOUND, projectIdentifier.toString())
    deleteProjectPicture(projectPicture)
  }

  @Trace
  @NoPreAuthorize
  @Transactional(propagation = MANDATORY)
  open fun deleteByProjectIdentifier(projectIdentifier: ProjectId) =
      projectPictureRepository.findOneByProjectIdentifier(projectIdentifier)?.let { picture ->
        projectPictureRepository.deleteAllInBatch(listOf(picture))
        for (imageResolution in ImageResolution.values()) {
          blobStoreService.deleteImageIfExists(picture, imageResolution)
        }
      }

  @Trace
  @PreAuthorize(
      "@projectPictureAuthorizationComponent.hasReadPermissionOnPicture(#projectPictureIdentifier)")
  @Transactional(readOnly = true)
  open fun generateBlobAccessUrl(projectPictureIdentifier: UUID, size: ImageResolution): URL {
    val projectPicture =
        projectPictureRepository.findOneByIdentifier(projectPictureIdentifier)
            ?: throw AggregateNotFoundException(
                PROJECT_VALIDATION_ERROR_PROJECT_PICTURE_NOT_FOUND,
                projectPictureIdentifier.toString())

    return blobStoreService.generateSignedUrlForImage(
        projectPicture, projectPicture.getResolutionOrOriginal(size))
        ?: throw AggregateNotFoundException(
            PROJECT_VALIDATION_ERROR_PROJECT_PICTURE_NOT_FOUND, projectPictureIdentifier.toString())
  }

  @Trace
  @PreAuthorize("@projectAuthorizationComponent.hasReadPermissionOnProject(#projectIdentifier)")
  @Transactional(readOnly = true)
  open fun findProjectPicture(projectIdentifier: ProjectId): ProjectPicture =
      projectPictureRepository.findOneByProjectIdentifier(projectIdentifier)
          ?: throw AggregateNotFoundException(
              PROJECT_VALIDATION_ERROR_PROJECT_PICTURE_NOT_FOUND, projectIdentifier.toString())

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  open fun findProjectPictureByIdentifier(projectIdentifier: UUID): ProjectPicture? =
      projectPictureRepository.findOneByIdentifier(projectIdentifier)

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  open fun findProjectPictures(projectIdentifiers: Set<ProjectId>): Map<ProjectId, ProjectPicture> =
      if (projectIdentifiers.isEmpty()) {
        emptyMap()
      } else {
        projectPictureRepository.findAllByProjectIdentifierIn(projectIdentifiers).associateBy {
          it.project!!.identifier
        }
      }

  @Trace
  @Transactional
  @PreAuthorize("@projectAuthorizationComponent.hasUpdatePermissionOnProject(#projectIdentifier)")
  open fun saveProjectPicture(
      binaryData: ByteArray,
      fileName: String,
      projectIdentifier: ProjectId,
      projectPictureIdentifier: UUID?,
  ): UUID {
    val existingPicture = projectPictureRepository.findOneByProjectIdentifier(projectIdentifier)
    if (existingPicture != null) {
      deleteProjectPicture(existingPicture)
      projectPictureRepository.flush()
    }

    val project = requireNotNull(projectRepository.findOneByIdentifier(projectIdentifier))
    val projectPicture = createProjectPicture(project, binaryData.size.toLong())
    projectPicture.identifier = projectPictureIdentifier ?: idGenerator.generateId()

    // Save original binary image in the blob store
    blobStoreService.saveImage(
        binaryData,
        projectPicture,
        BlobMetadata.from(fileName, TimeZone.getDefault(), projectPicture))

    return projectPictureRepository.save(projectPicture, CREATED).identifier!!
  }

  @Trace
  @Transactional
  @NoPreAuthorize
  open fun updateResolutionAvailable(projectPictureIdentifier: UUID, resolution: ImageResolution) {
    val projectPicture =
        projectPictureRepository.findOneByIdentifier(projectPictureIdentifier)
            ?: throw AggregateNotFoundException(
                PROJECT_VALIDATION_ERROR_PROJECT_PICTURE_NOT_FOUND,
                projectPictureIdentifier.toString())

    projectPicture.setResolutionAvailable(resolution)
    projectPictureRepository.save(projectPicture, UPDATED)
  }

  @Trace
  @Transactional
  @NoPreAuthorize
  open fun updateImageMetadata(
      projectPictureIdentifier: UUID,
      fileSize: Long,
      imageMetadata: ImageMetadataDto?
  ) {
    val projectPicture =
        projectPictureRepository.findOneByIdentifier(projectPictureIdentifier)
            ?: throw AggregateNotFoundException(
                PROJECT_VALIDATION_ERROR_PROJECT_PICTURE_NOT_FOUND,
                projectPictureIdentifier.toString())
    projectPicture.setResolutionAvailable(SMALL)
    projectPicture.setResolutionAvailable(FULL)
    projectPicture.setResolutionAvailable(ORIGINAL)
    projectPicture.fileSize = fileSize
    projectPicture.height = imageMetadata?.imageHeight
    projectPicture.width = imageMetadata?.imageWidth

    projectPictureRepository.save(projectPicture, UPDATED)
  }

  private fun createProjectPicture(
      project: Project,
      fileSize: Long,
  ): ProjectPicture = ProjectPicture(project, fileSize)
}

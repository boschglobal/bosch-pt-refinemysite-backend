/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.attachment.service

import com.bosch.pt.csm.cloud.common.blob.model.BoundedContext
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution
import com.bosch.pt.csm.cloud.projectmanagement.common.exceptions.ResourceNotFoundException
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key
import java.net.URL
import java.util.UUID
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service

@Service
class AttachmentService(private val blobStoreService: BlobStoreService) {

  @PreAuthorize("@taskActivityAuthorizationComponent.hasReadPermissionOnTask(#taskIdentifier)")
  fun generateBlobAccessUrl(
      boundedContext: BoundedContext,
      taskIdentifier: UUID,
      attachmentIdentifier: UUID,
      imageResolution: ImageResolution
  ): URL {
    return blobStoreService.generateSignedUrlForImage(
        boundedContext, taskIdentifier, attachmentIdentifier, imageResolution)
        ?: throw ResourceNotFoundException(Key.ATTACHMENT_VALIDATION_ERROR_NOT_FOUND)
  }
}

/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.download.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.iot.smartsite.project.attachment.facade.rest.AttachmentHelper.getFoundResponseEntity
import com.bosch.pt.iot.smartsite.project.download.repository.DownloadBlobStorageRepository
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@ApiVersion
@RestController
open class DownloadController(
    private val downloadBlobStorageRepository: DownloadBlobStorageRepository
) {

  @GetMapping(EXPORT_DOWNLOAD_BY_ID_ENDPOINT)
  fun download(@PathVariable(PATH_VARIABLE_DOCUMENT_ID) documentId: UUID): ResponseEntity<Void> =
      getFoundResponseEntity(downloadBlobStorageRepository.generateSignedUrl(documentId))

  companion object {
    const val PATH_VARIABLE_DOCUMENT_ID = "documentId"
    const val EXPORT_DOWNLOAD_BY_ID_ENDPOINT = "/projects/downloads/{documentId}"
  }
}

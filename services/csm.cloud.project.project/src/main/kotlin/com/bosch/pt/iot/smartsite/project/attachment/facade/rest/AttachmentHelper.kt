/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.attachment.facade.rest

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.iot.smartsite.common.i18n.Key.ATTACHMENT_VALIDATION_ERROR_IOERROR
import com.bosch.pt.iot.smartsite.project.attachment.boundary.AttachmentService
import java.io.BufferedInputStream
import java.io.IOException
import java.net.URL
import org.apache.commons.io.IOUtils.toByteArray
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.http.HttpStatus.FOUND
import org.springframework.http.InvalidMediaTypeException
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM
import org.springframework.http.ResponseEntity
import org.springframework.web.multipart.MultipartFile

object AttachmentHelper {

  private val LOGGER = LoggerFactory.getLogger(AttachmentService::class.java)

  fun getRawData(file: MultipartFile): ByteArray =
      try {
        BufferedInputStream(file.inputStream).use { inputStream -> toByteArray(inputStream) }
      } catch (ex: IOException) {
        throw PreconditionViolationException(ATTACHMENT_VALIDATION_ERROR_IOERROR, cause = ex)
      }

  fun getMediaType(mimeType: String?): MediaType =
      if (mimeType == null) {
        APPLICATION_OCTET_STREAM
      } else
          try {
            MediaType.parseMediaType(mimeType)
          } catch (e: InvalidMediaTypeException) {
            LOGGER.info("Couldn't parse mimetype", e)
            APPLICATION_OCTET_STREAM
          }

  fun getFoundResponseEntity(location: URL): ResponseEntity<Void> =
      ResponseEntity.status(FOUND).header(LOCATION, location.toString()).build()
}

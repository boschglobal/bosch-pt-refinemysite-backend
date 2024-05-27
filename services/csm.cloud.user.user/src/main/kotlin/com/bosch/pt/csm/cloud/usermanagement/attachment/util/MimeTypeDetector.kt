/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.attachment.util

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.IMAGE_VALIDATION_ERROR_INVALID_IMAGE
import java.io.IOException
import org.apache.tika.Tika
import org.apache.tika.io.TikaInputStream
import org.springframework.stereotype.Component

/** Mime type detector based on magic bytes detection. */
@Component
class MimeTypeDetector {

  private val tika = Tika()

  fun detect(data: ByteArray?): String =
      when (data == null) {
        true -> throw PreconditionViolationException(IMAGE_VALIDATION_ERROR_INVALID_IMAGE)
        else ->
            try {
              tika.detect(TikaInputStream.get(data))
            } catch (e: IOException) {
              throw IllegalStateException("Error detecting mime type", e)
            }
      }
}

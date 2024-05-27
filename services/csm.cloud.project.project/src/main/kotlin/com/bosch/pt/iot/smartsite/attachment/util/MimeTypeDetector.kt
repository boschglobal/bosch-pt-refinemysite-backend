/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.attachment.util

import java.io.IOException
import org.apache.tika.Tika
import org.apache.tika.io.TikaInputStream
import org.springframework.stereotype.Component

/** Mime type detector based on magic bytes detection. */
@Component
open class MimeTypeDetector {

  private val tika = Tika()

  open fun detect(data: ByteArray?): String =
      try {
        tika.detect(TikaInputStream.get(data))
      } catch (e: IOException) {
        throw IllegalStateException("Error detecting mime type", e)
      }
}

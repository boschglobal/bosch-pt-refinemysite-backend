/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.security

import java.util.Base64

object Base64Utils {

  fun encodeUrlSafe(src: ByteArray): ByteArray =
      if (src.isEmpty()) {
        src
      } else {
        Base64.getUrlEncoder().encode(src)
      }

  fun encodeToUrlSafeString(src: ByteArray): String =
    if (src.isEmpty()) {
      ""
    } else {
      Base64.getUrlEncoder().encodeToString(src)
    }
}

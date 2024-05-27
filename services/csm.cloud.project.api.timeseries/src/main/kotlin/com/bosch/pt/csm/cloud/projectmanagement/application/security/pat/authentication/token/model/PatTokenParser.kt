/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.model

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.model.PatToken.Companion.ID_SEPARATOR
import java.text.ParseException

object PatTokenParser {

  @Suppress("ThrowsCount")
  fun parse(token: String): PatToken? {
    if (token.isBlank()) {
      return null
    }
    if (!token.contains(".")) {
      throw ParseException("Missing dot delimiter(s)", 0)
    }

    val parts = token.split(".")
    require(parts.size == 3) { "Invalid number of pat components" }

    val type = parts[0]
    val patId = parts[1]
    val secret = parts[2]

    val patIdAsUuid =
        try {
          val expandedPatId =
              patId.let {
                check(it.length == 32) { "Invalid pat id" }
                it.slice(0..7) +
                    ID_SEPARATOR +
                    it.slice(8..11) +
                    ID_SEPARATOR +
                    it.slice(12..15) +
                    ID_SEPARATOR +
                    it.slice(16..19) +
                    ID_SEPARATOR +
                    it.slice(20..31)
              }
          expandedPatId.toUUID()
        } catch (e: IllegalArgumentException) {
          throw ParseException("Invalid pat identifier", 0)
        } catch (e: IllegalStateException) {
          throw ParseException("Invalid pat format", 0)
        }

    return PatToken(type, patIdAsUuid, secret)
  }
}

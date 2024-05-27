/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token

import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.model.PatTokenParser
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.InvalidPatException
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatTypeEnum
import org.springframework.core.convert.converter.Converter

/** The type converter could be used to handle different versions of the PATs. */
class PatTypeConverter : Converter<PatAuthenticationToken, PatTypeEnum> {

  override fun convert(authenticationToken: PatAuthenticationToken): PatTypeEnum =
      try {
        val token = PatTokenParser.parse(authenticationToken.token)
        PatTypeEnum.valueOf(checkNotNull(token).type)
      } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        throw InvalidPatException(e.message ?: "Unsupported pat type", e)
      }
}

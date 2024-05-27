/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security

import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.PatAuthenticationToken
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.PatTypeConverter
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.InvalidPatException
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatTypeEnum.RMSPAT1
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test

class PatTypeConverterTest {

  private val cut = PatTypeConverter()

  @Test
  fun `convert valid pat type`() {
    val token = "RMSPAT1.7281b943e25b4822bd92c330ebcc9a59.xbmb7t9pQ8z8veA2MlwlOXvBxMvDDcpd"

    val authenticationToken = PatAuthenticationToken(token)
    assertThat(cut.convert(authenticationToken)).isEqualTo(RMSPAT1)
  }

  @Test
  fun `unsupported type`() {
    val invalid = "RMSUNS1.7281b943e25b4822bd92c330ebcc9a59.xbmb7t9pQ8z8veA2MlwlOXvBxMvDDcpd"

    val authenticationToken = PatAuthenticationToken(invalid)
    assertThatExceptionOfType(InvalidPatException::class.java)
        .isThrownBy { cut.convert(authenticationToken) }
        .withMessage(
            "No enum constant com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatTypeEnum.RMSUNS1")
  }

  @Test
  fun `invalid pat`() {
    val invalid = "a.b.c"

    val authenticationToken = PatAuthenticationToken(invalid)
    assertThatExceptionOfType(InvalidPatException::class.java)
        .isThrownBy { cut.convert(authenticationToken) }
        .withMessage("Invalid pat format")
  }
}

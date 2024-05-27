/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security

import com.bosch.pt.csm.cloud.common.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.PatAuthenticationManagerResolver
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.PatAuthenticationToken
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.details.PatUserDetailsAuthenticationToken
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.model.RmsPat1AuthenticationTokenConverter
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.domain.PatId
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.domain.asPatId
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatProjection
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatScopeEnum.TIMELINE_API_READ
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatTypeEnum.RMSPAT1
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.service.PatQueryService
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.asUserId
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import java.time.LocalDateTime
import java.util.Locale
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest

@SmartSiteMockKTest
class PatAuthenticationManagerResolverTest {

  @RelaxedMockK private lateinit var patQueryService: PatQueryService

  private val cut by lazy {
    PatAuthenticationManagerResolver(
        mapOf(RMSPAT1 to RmsPat1AuthenticationTokenConverter(patQueryService)))
  }

  @Test
  fun `resolve and authenticate`() {
    val patId = "7281b943-e25b-4822-bd92-c330ebcc9a59".toUUID().asPatId()
    val impersonatedUserIdentifier = "6fab69f3-9c00-4835-c9bf-9ac564100d94".toUUID().asUserId()

    // expect the authentication manager above to be returned
    val authenticationManager = cut.resolve(MockHttpServletRequest())
    for (patType in PatTypeEnum.values()) {
      val patProjection = patProjection(patId, impersonatedUserIdentifier, patType)
      every { patQueryService.findByIdentifier(patId) } returns patProjection

      val pat =
          PatAuthenticationToken(
              "$patType.7281b943e25b4822bd92c330ebcc9a59.xbmb7t9pQ8z8veA2MlwlOXvBxMvDDcpd")

      val authentication = authenticationManager.authenticate(pat)
      assertThat(authentication).isInstanceOf(PatUserDetailsAuthenticationToken::class.java)
      assertThat(authentication.principal).isEqualTo(patProjection)
    }
  }

  private fun patProjection(
      patId: PatId,
      impersonatedUserIdentifier: UserId,
      patTypeEnum: PatTypeEnum
  ) =
      PatProjection(
          patId,
          0L,
          "description",
          impersonatedUserIdentifier,
          "\$2a\$10\$.alDG9I8Q2l1YR265lFxDeJ88iTecGPCRSSOA9b4HHKFfPk1k/zcC",
          patTypeEnum,
          listOf(TIMELINE_API_READ),
          LocalDateTime.now(),
          LocalDateTime.now().plusMinutes(5),
          false,
          LocalDateTime.now(),
          Locale.GERMANY)
}

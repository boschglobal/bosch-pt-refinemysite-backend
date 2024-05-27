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
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.model.PatToken
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.model.RmsPat1AuthenticationTokenConverter
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.domain.PatId
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.domain.asPatId
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatProjection
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatScopeEnum.TIMELINE_API_READ
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatTypeEnum.RMSPAT1
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.service.PatQueryService
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.asUserId
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import java.time.LocalDateTime
import java.util.Locale.GERMANY
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.userdetails.UsernameNotFoundException

@SmartSiteMockKTest
class RmsPat1AuthenticationTokenConverterTest {

  @RelaxedMockK private lateinit var patQueryService: PatQueryService

  private val cut by lazy { RmsPat1AuthenticationTokenConverter(patQueryService) }

  @Test
  fun `empty secret`() {
    val token = PatToken(RMSPAT1.name, randomUUID(), "")
    assertThatExceptionOfType(UsernameNotFoundException::class.java)
        .isThrownBy { cut.convert(token) }
        .withMessage("Empty secret given")
  }

  @Test
  fun `pat projection not found for pat id`() {
    val token = PatToken(RMSPAT1.name, randomUUID(), "secret")

    every { patQueryService.findByIdentifier(any()) } returns null

    assertThatExceptionOfType(UsernameNotFoundException::class.java)
        .isThrownBy { cut.convert(token) }
        .withMessage("No user found")
  }

  @Test
  fun `invalid token`() {
    val patId = randomUUID()
    val token = PatToken(RMSPAT1.name, patId, "wU2oMEreh0UqSQ2ySGjLftWAxM7Mfsu9")

    every { patQueryService.findByIdentifier(any()) } returns
        patProjection(patId.asPatId(), randomUUID().asUserId())

    assertThatExceptionOfType(UsernameNotFoundException::class.java)
        .isThrownBy { cut.convert(token) }
        .withMessage("No pat found")
  }

  @Test
  fun `pat locked`() {
    val patId = "7281b943-e25b-4822-bd92-c330ebcc9a59".toUUID()
    val impersonatedUserIdentifier = "6fab69f3-9c00-4835-c9bf-9ac564100d94".toUUID().asUserId()
    val token = PatToken(RMSPAT1.name, patId, "xbmb7t9pQ8z8veA2MlwlOXvBxMvDDcpd")

    every { patQueryService.findByIdentifier(any()) } returns
        patProjection(patId.asPatId(), impersonatedUserIdentifier, true)

    assertThatExceptionOfType(LockedException::class.java)
        .isThrownBy { cut.convert(token) }
        .withMessage("The user account with userId $impersonatedUserIdentifier is locked")
  }

  @Test
  fun `pat expired`() {
    val patId = "7281b943-e25b-4822-bd92-c330ebcc9a59".toUUID()
    val impersonatedUserIdentifier = "6fab69f3-9c00-4835-c9bf-9ac564100d94".toUUID().asUserId()
    val token = PatToken(RMSPAT1.name, patId, "xbmb7t9pQ8z8veA2MlwlOXvBxMvDDcpd")

    every { patQueryService.findByIdentifier(any()) } returns
        patProjection(patId.asPatId(), impersonatedUserIdentifier)
            .copy(expiresAt = LocalDateTime.now().minusDays(1))

    assertThatExceptionOfType(CredentialsExpiredException::class.java)
        .isThrownBy { cut.convert(token) }
        .withMessage("Pat expired")
  }

  @Test
  fun `conversion successful`() {
    val patId = "7281b943-e25b-4822-bd92-c330ebcc9a59".toUUID()
    val impersonatedUserIdentifier = "6fab69f3-9c00-4835-c9bf-9ac564100d94".toUUID().asUserId()
    val token = PatToken(RMSPAT1.name, patId, "xbmb7t9pQ8z8veA2MlwlOXvBxMvDDcpd")

    val patProjection = patProjection(patId.asPatId(), impersonatedUserIdentifier)
    every { patQueryService.findByIdentifier(any()) } returns patProjection

    assertThat(cut.convert(token).principal).isEqualTo(patProjection)
  }

  private fun patProjection(
      patId: PatId,
      impersonatedUserIdentifier: UserId,
      locked: Boolean? = false
  ) =
      PatProjection(
          patId,
          0L,
          "description",
          impersonatedUserIdentifier,
          "\$2a\$10\$.alDG9I8Q2l1YR265lFxDeJ88iTecGPCRSSOA9b4HHKFfPk1k/zcC",
          RMSPAT1,
          listOf(TIMELINE_API_READ),
          LocalDateTime.now(),
          LocalDateTime.now().plusMinutes(5),
          checkNotNull(locked),
          LocalDateTime.now(),
          GERMANY)
}

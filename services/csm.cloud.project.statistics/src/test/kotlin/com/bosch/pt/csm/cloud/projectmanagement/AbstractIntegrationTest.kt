/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.registerStaticContext
import com.bosch.pt.csm.cloud.common.util.HttpTestUtils.setFakeUrlWithApiVersion
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.security.authorizeWithUser
import java.util.Locale
import java.util.Locale.UK
import java.util.TimeZone
import java.util.UUID
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.security.core.context.SecurityContextHolder

@SmartSiteSpringBootTest
@Suppress("UnnecessaryAbstractClass")
abstract class AbstractIntegrationTest {

  @Autowired protected lateinit var messageSource: MessageSource

  @Autowired protected lateinit var repositories: Repositories

  @Autowired protected lateinit var eventStreamGenerator: EventStreamGenerator

  @Value("\${system.user.identifier}") protected lateinit var systemUserIdentifier: String

  @BeforeEach
  fun initAbstractIntegrationTest() {
    eventStreamGenerator.registerStaticContext()
    setFakeUrlWithApiVersion()
    Locale.setDefault(UK)
  }

  @AfterEach
  fun cleanup() {
    eventStreamGenerator.reset()
    SecurityContextHolder.clearContext()
    repositories.truncateDatabase()
  }

  protected fun setAuthentication(userIdentifier: UUID) =
      authorizeWithUser(repositories.userRepository.findOneByIdentifier(userIdentifier)!!, false)

  companion object {
    init {
      TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }
  }
}

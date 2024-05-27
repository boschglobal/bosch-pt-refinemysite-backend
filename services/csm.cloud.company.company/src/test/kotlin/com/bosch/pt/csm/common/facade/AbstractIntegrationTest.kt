/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.common.facade

import com.bosch.pt.csm.application.security.AuthorizationTestUtils.authorizeWithUser
import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.eventstore.EventStoreUtils
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.registerStaticContext
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitTestAdminUserAndActivate
import com.bosch.pt.csm.common.Repositories
import com.bosch.pt.csm.common.event.CompanyServiceEventStreamContext
import com.bosch.pt.csm.common.util.HttpTestUtils.setFakeUrlWithApiVersion
import com.bosch.pt.csm.company.eventstore.CompanyContextKafkaEvent
import java.util.TimeZone
import java.util.UUID
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.transaction.support.TransactionTemplate

@Suppress("UnnecessaryAbstractClass")
abstract class AbstractIntegrationTest {

  @Autowired protected lateinit var messageSource: MessageSource

  @Autowired
  protected lateinit var companyEventStoreUtils: EventStoreUtils<CompanyContextKafkaEvent>

  @Autowired protected lateinit var repositories: Repositories

  @Autowired protected lateinit var transactionTemplate: TransactionTemplate

  @Autowired protected lateinit var eventStreamGenerator: EventStreamGenerator

  @Value("\${system.user.identifier}") protected lateinit var systemUserIdentifier: String

  @BeforeEach
  protected fun initAbstractIntegrationTest() {
    eventStreamGenerator.registerStaticContext()
    getContext().useRestoreListener()
    eventStreamGenerator.submitSystemUserAndActivate().submitTestAdminUserAndActivate()

    setFakeUrlWithApiVersion()
  }

  @AfterEach
  protected fun cleanup() {
    eventStreamGenerator.reset()
    companyEventStoreUtils.reset()
    SecurityContextHolder.clearContext()
    repositories.truncateDatabase()
  }

  protected fun getContext() = eventStreamGenerator.getContext() as CompanyServiceEventStreamContext

  protected fun setAuthentication(userReference: String) =
      setAuthentication(eventStreamGenerator.getIdentifier(userReference))

  private fun setAuthentication(userIdentifier: UUID) =
      repositories.userProjectionRepository.findOneById(userIdentifier.asUserId())!!.also {
        authorizeWithUser(it, it.admin)
      }

  companion object {
    init {
      TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }
  }
}

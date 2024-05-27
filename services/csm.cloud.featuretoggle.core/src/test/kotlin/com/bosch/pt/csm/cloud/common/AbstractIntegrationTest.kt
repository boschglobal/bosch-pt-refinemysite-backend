/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common

import com.bosch.pt.csm.cloud.application.event.FeaturetoggleServiceEventStreamContext
import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.eventstore.EventStoreUtils
import com.bosch.pt.csm.cloud.common.security.AuthorizationTestUtils.authorizeWithUser
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.registerStaticContext
import com.bosch.pt.csm.cloud.common.util.HttpTestUtils.setFakeUrlWithApiVersion
import com.bosch.pt.csm.cloud.featuretoggle.eventstore.FeaturetoggleContextKafkaEvent
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitTestAdminUserAndActivate
import java.util.TimeZone
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
  protected lateinit var companyEventStoreUtils: EventStoreUtils<FeaturetoggleContextKafkaEvent>

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

  protected fun getContext() =
      eventStreamGenerator.getContext() as FeaturetoggleServiceEventStreamContext

  protected fun setAuthentication(userReference: String) =
      setAuthentication(UserId(eventStreamGenerator.getIdentifier(userReference)))

  protected fun setAuthentication(userIdentifier: UserId) =
      repositories.userProjectionRepository.findOneById(userIdentifier)!!.also {
        authorizeWithUser(it, it.admin)
      }

  companion object {
    init {
      TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }
  }
}

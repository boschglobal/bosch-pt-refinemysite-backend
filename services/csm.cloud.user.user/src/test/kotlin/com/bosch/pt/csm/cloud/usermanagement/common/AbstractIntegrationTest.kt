/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.common

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.eventstore.EventStoreUtils
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.registerStaticContext
import com.bosch.pt.csm.cloud.usermanagement.application.security.AuthorizationTestUtils.authorizeWithUnregisteredUser
import com.bosch.pt.csm.cloud.usermanagement.application.security.AuthorizationTestUtils.authorizeWithUser
import com.bosch.pt.csm.cloud.usermanagement.common.event.UserServiceEventStreamContext
import com.bosch.pt.csm.cloud.usermanagement.common.util.HttpTestUtils.setFakeUrlWithApiVersion
import com.bosch.pt.csm.cloud.usermanagement.pat.eventstore.PatKafkaEvent
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitTestAdminUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.eventstore.UserContextKafkaEvent
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.UnregisteredUser
import java.util.TimeZone
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.transaction.support.TransactionTemplate

@Suppress("UnnecessaryAbstractClass")
abstract class AbstractIntegrationTest {

  @Autowired protected lateinit var messageSource: MessageSource

  @Autowired protected lateinit var userEventStoreUtils: EventStoreUtils<UserContextKafkaEvent>

  @Autowired protected lateinit var patEventStreamUtils: EventStoreUtils<PatKafkaEvent>

  @Autowired protected lateinit var repositories: Repositories

  @Autowired protected lateinit var transactionTemplate: TransactionTemplate

  @Autowired protected lateinit var eventStreamGenerator: EventStreamGenerator

  @Value("\${system.user.identifier}") protected lateinit var systemUserIdentifier: String

  @BeforeEach
  protected fun initAbstractIntegrationTest() {
    eventStreamGenerator
        .registerStaticContext()
        .submitSystemUserAndActivate()
        .submitTestAdminUserAndActivate()

    setFakeUrlWithApiVersion()
  }

  @AfterEach
  protected fun cleanup() {
    eventStreamGenerator.reset()
    userEventStoreUtils.reset()
    TestSecurityContextHolder.clearContext()
    repositories.truncateDatabase()
  }

  protected fun useOnlineListener() {
    (eventStreamGenerator.getContext() as UserServiceEventStreamContext).useOnlineListener()
  }

  protected fun useRestoreListener() {
    (eventStreamGenerator.getContext() as UserServiceEventStreamContext).useRestoreListener()
  }

  protected fun setAuthentication(userReference: String) =
      setAuthentication(UserId(eventStreamGenerator.getIdentifier(userReference)))

  protected fun setAuthentication(userIdentifier: UserId) =
      repositories.userRepository.findOneByIdentifier(userIdentifier)!!.also {
        authorizeWithUser(it, it.admin)
      }

  protected fun setSecurityContextAsUnregisteredUser(): UnregisteredUser {
    TestSecurityContextHolder.clearContext()
    val unregisteredUser = UnregisteredUser("UNREGISTERED", "test@example.com")
    authorizeWithUnregisteredUser(unregisteredUser)
    return unregisteredUser
  }

  companion object {
    init {
      TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }
  }
}

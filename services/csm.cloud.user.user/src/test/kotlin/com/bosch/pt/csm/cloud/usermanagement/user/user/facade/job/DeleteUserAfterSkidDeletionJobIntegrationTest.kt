package com.bosch.pt.csm.cloud.usermanagement.user.user.facade.job

import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.usermanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.usermanagement.application.config.EnableAllKafkaListeners
import com.bosch.pt.csm.cloud.usermanagement.application.security.AuthorizationUtils.doWithAuthenticatedUser
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractIntegrationTest
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.USER_VALIDATION_ERROR_SYSTEM_USER_MUST_NOT_BE_MODIFIED
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.OK

@EnableAllKafkaListeners
@SmartSiteSpringBootTest
class DeleteUserAfterSkidDeletionJobIntegrationTest : AbstractIntegrationTest() {

  private lateinit var mockServer: MockWebServer

  @Autowired private lateinit var cut: DeleteUserAfterSkidDeletionJob

  @BeforeEach
  fun setup() {
    mockServer = MockWebServer().apply { start(5678) }
  }

  @AfterEach fun tearDown() = mockServer.shutdown()

  @Test
  fun `verify that the correct users are deleted`() {
    eventStreamGenerator
        .submitUser("user1") { it.userId = USER_ID_1 }
        .submitUser("user2") { it.userId = USER_ID_2 }
        .submitUser("user3") { it.userId = USER_ID_3 }

    val usersBeforeDeletion = repositories.userRepository.findAll()

    // make sure all users have been created
    assertThat(usersBeforeDeletion)
        .extracting<String> { it.externalUserId }
        .containsAnyOf(USER_ID_1, USER_ID_2, USER_ID_3)

    mockServer.enqueue(
        MockResponse()
            .setHeader("Content-Type", "application/json")
            .setResponseCode(OK.value())
            .setBody(SKID_DELETED_USERS_RESPONSE))

    doWithAuthenticatedUser(findSystemUser()) { cut.deleteUsersDeletedInSkid() }

    userEventStoreUtils.verifyContainsTombstoneMessages(2, USER.value)

    val remainingUsersAfterDeletion = repositories.userRepository.findAll()

    // user1 and user2 must have been deleted
    assertThat(remainingUsersAfterDeletion)
        .extracting<String> { it.externalUserId }
        .doesNotContain(USER_ID_1, USER_ID_2)

    // user3 and system user must not have been deleted
    assertThat(remainingUsersAfterDeletion)
        .extracting<String> { it.externalUserId }
        .containsAnyOf(USER_ID_3, "SYSTEM")
  }

  @Test
  fun `verify that deleting the system user fails`() {
    mockServer.enqueue(
        MockResponse()
            .setHeader("Content-Type", "application/json")
            .setResponseCode(OK.value())
            .setBody(
                """
                [
                  {"id":"SYSTEM","deleteDate":"2023-12-01T00:00:00"}
                ]
                """))

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy { doWithAuthenticatedUser(findSystemUser()) { cut.deleteUsersDeletedInSkid() } }
        .withMessage(USER_VALIDATION_ERROR_SYSTEM_USER_MUST_NOT_BE_MODIFIED)
  }

  private fun findSystemUser() =
      (repositories.userRepository.findOneByIdentifier(systemUserIdentifier.asUserId())
          ?: error("Couldn't load system user by identifier $systemUserIdentifier"))

  companion object {
    private const val USER_ID_1 = "75ca4fbe-3671-4e07-bb72-f1a1b0f39b60"
    private const val USER_ID_2 = "3928f841-9915-42a6-8f26-c4110f6929e5"
    private const val USER_ID_3 = "86db51cf-4782-5f18-cc83-g2b2c1140b71"

    private const val SKID_DELETED_USERS_RESPONSE =
        """
        [
          {"id":"$USER_ID_1","deleteDate":"2023-11-09T00:18:41"},
          {"id":"$USER_ID_2","deleteDate":"2023-11-09T00:19:01"}
        ]
        """
  }
}

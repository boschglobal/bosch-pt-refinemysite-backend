package com.bosch.pt.csm.cloud.usermanagement.user.user.integration

import com.bosch.pt.csm.cloud.usermanagement.application.SmartSiteSpringBootTest
import java.time.LocalDateTime
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.OK

@SmartSiteSpringBootTest
class SkidIntegrationServiceIntegrationTest {

  private lateinit var mockServer: MockWebServer

  @Autowired private lateinit var cut: SkidIntegrationService

  @BeforeEach
  fun setup() {
    mockServer = MockWebServer().apply { start(5678) }
  }

  @AfterEach fun tearDown() = mockServer.shutdown()

  @Test
  fun `verify that the SKID response is parsed properly`() {
    mockServer.enqueue(
        MockResponse()
            .setHeader("Content-Type", "application/json")
            .setResponseCode(OK.value())
            .setBody(SKID_DELETED_USERS_RESPONSE))

    val foundUserIds =
        cut.findUsersDeletedInDateRangeInBatches(
            fromInclusive = LocalDateTime.parse("2023-12-01T02:03:04.567"),
            toExclusive = LocalDateTime.parse("2023-12-02T02:03:04.567"))

    assertThat(foundUserIds.first()).containsExactlyInAnyOrder(USER_ID_1, USER_ID_2)
  }

  @Test
  fun `verify that the SKID request is sent properly`() {
    mockServer.enqueue(
        MockResponse()
            .setHeader("Content-Type", "application/json")
            .setResponseCode(OK.value())
            .setBody(SKID_DELETED_USERS_RESPONSE))

    cut.findUsersDeletedInDateRangeInBatches(
            fromInclusive = LocalDateTime.parse("2023-12-01T02:03:04.567"),
            toExclusive = LocalDateTime.parse("2023-12-02T02:03:04.567"))
        .first()

    mockServer.takeRequest().also {
      assertThat(it.method).isEqualTo("GET")
      assertThat(it.path)
          .isEqualTo(
              "/auth/api/v1/UserDeletion/deleted?" +
                  "from=2023-12-01T02:03:04Z&" +
                  "to=2023-12-02T02:03:04Z")
    }
  }

  @Test
  fun `verify that HTTP 400 bad request is handled`() {
    mockServer.enqueue(
        MockResponse()
            .setHeader("Content-Type", "application/json")
            .setResponseCode(BAD_REQUEST.value())
            .setBody(SKID_DELETED_USERS_RESPONSE))

    assertThatExceptionOfType(SkidBadRequestException::class.java).isThrownBy {
      cut.findUsersDeletedInDateRangeInBatches(
              fromInclusive = LocalDateTime.parse("2023-12-01T02:03:04.567"),
              toExclusive = LocalDateTime.parse("2023-12-02T02:03:04.567"))
          .first()
    }
  }

  companion object {
    private const val USER_ID_1 = "75ca4fbe-3671-4e07-bb72-f1a1b0f39b60"
    private const val USER_ID_2 = "3928f841-9915-42a6-8f26-c4110f6929e5"

    private const val SKID_DELETED_USERS_RESPONSE =
        """
        [
          {"id":"$USER_ID_1","deleteDate":"2023-11-09T00:18:41"},
          {"id":"$USER_ID_2","deleteDate":"2023-11-09T00:19:01"}
        ]
        """
  }
}

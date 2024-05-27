/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.mail.integration

import com.bosch.pt.iot.smartsite.application.SmartSiteSpringBootTest
import com.bosch.pt.iot.smartsite.application.config.MailjetPort
import com.bosch.pt.iot.smartsite.application.config.MailjetProperties
import com.bosch.pt.iot.smartsite.application.config.MailjetProperties.ApiCredentials
import com.bosch.pt.iot.smartsite.mail.template.MailTemplate
import com.bosch.pt.iot.smartsite.mail.template.TemplateResolver
import com.bosch.pt.iot.smartsite.util.bcc
import com.bosch.pt.iot.smartsite.util.bccs
import com.bosch.pt.iot.smartsite.util.cc
import com.bosch.pt.iot.smartsite.util.ccs
import com.bosch.pt.iot.smartsite.util.messages
import com.bosch.pt.iot.smartsite.util.recipient
import com.bosch.pt.iot.smartsite.util.recipients
import com.bosch.pt.iot.smartsite.util.respondWithError
import com.bosch.pt.iot.smartsite.util.respondWithSuccess
import com.bosch.pt.iot.smartsite.util.templateId
import com.bosch.pt.iot.smartsite.util.variables
import com.mailjet.client.MailjetClient
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.json.JSONObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("Verify mail")
@SmartSiteSpringBootTest
internal class MailIntegrationServiceIntegrationTest {

  @Autowired private lateinit var mailjetPort: MailjetPort
  @Autowired private lateinit var mailjetClient: MailjetClient
  @Autowired private lateinit var templateResolver: TemplateResolver
  @Autowired private lateinit var cut: MailIntegrationService

  private lateinit var mockServer: MockWebServer

  @BeforeEach
  fun setup() {
    mockServer = MockWebServer().apply { start(mailjetPort.value) }
  }

  @AfterEach fun tearDown() = mockServer.shutdown()

  @Test
  fun `is sent only once`() {
    mockServer.respondWithSuccess()

    cut.sendMail(TestMailTemplate("var1", "var2"), US, MailContact("test@example.com"))
    val request = mockServer.takeRequest()

    assertThat(mockServer.requestCount).isEqualTo(1)
    assertThat(request.messages().length()).isEqualTo(1)
  }

  @Test
  fun `has single recipient`() {
    mockServer.respondWithSuccess()

    cut.sendMail(TestMailTemplate("var1", "var2"), US, MailContact("test@example.com"))
    val request = mockServer.takeRequest()

    assertThat(request.recipients().length()).isEqualTo(1)
  }

  @Test
  fun `has correct recipient when no name is set`() {
    mockServer.respondWithSuccess()

    cut.sendMail(TestMailTemplate("var1", "var2"), US, MailContact("test@example.com"))
    val recipient = mockServer.takeRequest().recipient()

    assertThat(recipient.getString("Email")).isEqualTo("test@example.com")
    assertThat(recipient.has("Name")).isFalse
  }

  @Test
  fun `has correct recipient when both email and name are set`() {
    mockServer.respondWithSuccess()

    cut.sendMail(TestMailTemplate("var1", "var2"), US, MailContact("test@example.com", "test"))
    val recipient = mockServer.takeRequest().recipient()

    assertThat(recipient.getString("Email")).isEqualTo("test@example.com")
    assertThat(recipient.getString("Name")).isEqualTo("test")
  }

  @Test
  fun `has correct cc when set without name`() {
    mockServer.respondWithSuccess()

    cut.sendMail(
        TestMailTemplate("var1", "var2"),
        US,
        MailContact("test@example.com"),
        MailContact("cc@example.com"))
    val cc = mockServer.takeRequest().cc()

    assertThat(cc.getString("Email")).isEqualTo("cc@example.com")
    assertThat(cc.has("Name")).isFalse
  }

  @Test
  fun `has correct cc when set with name`() {
    mockServer.respondWithSuccess()

    cut.sendMail(
        TestMailTemplate("var1", "var2"),
        US,
        MailContact("test@example.com"),
        MailContact("cc@example.com", "cc"))
    val cc = mockServer.takeRequest().cc()

    assertThat(cc.getString("Email")).isEqualTo("cc@example.com")
    assertThat(cc.getString("Name")).isEqualTo("cc")
  }

  @Test
  fun `has no cc when not set`() {
    mockServer.respondWithSuccess()
    val mailjetPropertiesNoBcc = MailjetProperties(ApiCredentials("", ""))
    val cut = MailIntegrationService(mailjetClient, templateResolver, mailjetPropertiesNoBcc)

    cut.sendMail(TestMailTemplate("var1", "var2"), US, MailContact("test@example.com"))
    val request = mockServer.takeRequest()

    assertThat(request.ccs().length()).isEqualTo(0)
  }

  @Test
  fun `has correct bcc when set`() {
    mockServer.respondWithSuccess()

    cut.sendMail(
        TestMailTemplate("var1", "var2"), US, MailContact("test@example.com"), sendBcc = true)
    val bcc = mockServer.takeRequest().bcc()

    assertThat(bcc.getString("Email")).isEqualTo("bcc@example.com")
    assertThat(bcc.has("Name")).isFalse
  }

  @Test
  fun `has no bcc when not set`() {
    mockServer.respondWithSuccess()
    val mailjetPropertiesNoBcc = MailjetProperties(ApiCredentials("", ""))
    val cut = MailIntegrationService(mailjetClient, templateResolver, mailjetPropertiesNoBcc)

    cut.sendMail(TestMailTemplate("var1", "var2"), US, MailContact("test@example.com"))
    val request = mockServer.takeRequest()

    assertThat(request.bccs().length()).isEqualTo(0)
  }

  @Test
  fun `has correct variables`() {
    mockServer.respondWithSuccess()

    cut.sendMail(TestMailTemplate("var1", "var2"), US, MailContact("test@example.com"))
    val variables = mockServer.takeRequest().variables()

    assertThat(variables.getString("variable1")).isEqualTo("var1")
    assertThat(variables.getString("variable2")).isEqualTo("var2")
  }

  @Test
  fun `has correct JSON variables`() {
    mockServer.respondWithSuccess()

    cut.sendMail(
        JsonMailTemplate("var1", mapOf("var2" to "val2")), US, MailContact("test@example.com"))
    val variables = mockServer.takeRequest().variables()

    assertThat(variables.getString("variable1")).isEqualTo("var1")
    assertThat(variables.getJSONObject("variable2").toString())
        .isEqualTo(JSONObject(mapOf("var2" to "val2")).toString())
  }

  @Test
  fun `is redirected when the recipient is a test user`() {
    mockServer.respondWithSuccess()

    cut.sendMail(TestMailTemplate("var1", "var2"), US, MailContact("smartsiteapp+daniel@gmail.com"))
    val recipient = mockServer.takeRequest().recipient()

    assertThat(recipient.getString("Email")).isEqualTo("redirect@example.com")
  }

  @Test
  fun `uses correct template for supported country code`() {
    mockServer.respondWithSuccess()

    cut.sendMail(TestMailTemplate("var1", "var2"), US, MailContact("test@example.com"))
    val request = mockServer.takeRequest()

    assertThat(request.templateId()).isEqualTo(200)
  }

  @Test
  fun `falls back to default template for unsupported country code`() {
    mockServer.respondWithSuccess()

    cut.sendMail(TestMailTemplate("var1", "var2"), GL, MailContact("test@example.com"))
    val request = mockServer.takeRequest()

    assertThat(request.templateId()).isEqualTo(300)
  }

  @Test
  fun `falls back to default template for unspecified country code`() {
    mockServer.respondWithSuccess()

    cut.sendMail(TestMailTemplate("var1", "var2"), null, MailContact("test@example.com"))
    val request = mockServer.takeRequest()

    assertThat(request.templateId()).isEqualTo(300)
  }

  @Test
  fun `sending fails for unknown template name`() {
    mockServer.respondWithSuccess()

    assertThatThrownBy { cut.sendMail(UnknownMailTemplate(), US, MailContact("test@example.com")) }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("Could not find configuration")
  }

  @Test
  fun `sending fails for Mailjet reporting errors`() {
    mockServer.respondWithError()

    assertThatThrownBy {
          cut.sendMail(TestMailTemplate("var1", "var2"), US, MailContact("test@example.com"))
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContainingAll("Error(s) sending mail", "errorCode=mj-0004", "errorCode=mj-0005")
  }

  class TestMailTemplate(variable1: String, variable2: String) : MailTemplate("test-template") {

    override val variables = mapOf("variable1" to variable1, "variable2" to variable2)
  }

  class UnknownMailTemplate : MailTemplate("unknown-template") {

    override val variables: Map<String, String> = mapOf()
  }

  class JsonMailTemplate(variable1: String, variable2: Map<String, Any>) :
      MailTemplate("test-template") {

    override val variables = mapOf("variable1" to variable1, "variable2" to variable2)
  }

  companion object {
    const val US = "US"
    const val GL = "GL"
  }
}

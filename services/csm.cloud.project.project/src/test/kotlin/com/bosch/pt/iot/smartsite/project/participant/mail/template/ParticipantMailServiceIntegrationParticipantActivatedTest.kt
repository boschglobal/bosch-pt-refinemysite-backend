/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.mail.template

import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompanyWithBothAddresses
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompanyWithPostboxAddress
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.application.config.MailProperties
import com.bosch.pt.iot.smartsite.application.config.MailjetPort
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.simulateKafkaListener
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.participant.command.sideeffects.ParticipantMailService
import com.bosch.pt.iot.smartsite.project.participant.mail.template.ParticipantActivatedTemplate.Companion.PROJECT_URL
import com.bosch.pt.iot.smartsite.project.participant.mail.template.ParticipantActivatedTemplate.Companion.RECIPIENT_NAME
import com.bosch.pt.iot.smartsite.project.participant.mail.template.ParticipantActivatedTemplate.Companion.TEMPLATE_NAME
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.util.cc
import com.bosch.pt.iot.smartsite.util.recipient
import com.bosch.pt.iot.smartsite.util.respondWithSuccess
import com.bosch.pt.iot.smartsite.util.templateId
import com.bosch.pt.iot.smartsite.util.variables
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("Verify participant invitation mail B2 (account activated)")
@EnableAllKafkaListeners
open class ParticipantMailServiceIntegrationParticipantActivatedTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var mailjetPort: MailjetPort
  @Autowired private lateinit var cut: ParticipantMailService
  @Autowired private lateinit var mailProperties: MailProperties

  private val templateInvitedProperties by lazy { mailProperties.templates[TEMPLATE_NAME]!! }

  private val mockServer by lazy { MockWebServer().apply { start(mailjetPort.value) } }

  private val invitedParticipant by lazy {
    repositories.findParticipant(getIdentifier("invitedParticipant"))!!
  }
  private val invitingUser by lazy { repositories.findUser(getIdentifier("userCsm1"))!! }
  private val invitingParticipant by lazy {
    repositories.findParticipant(invitingParticipantIdentifier)!!
  }
  private val invitingParticipantIdentifier by lazy { getIdentifier("participantCsm1") }
  private val project by lazy { repositories.findProject(getIdentifier("project").asProjectId())!! }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .setUserContext("userCsm1")
        .submitUser("invitedUser")
        .submitParticipantG3("invitedParticipant")

    setAuthentication("userCsm1")
    projectEventStoreUtils.reset()
    mockServer.respondWithSuccess()
  }

  @AfterEach fun tearDown() = mockServer.shutdown()

  @Test
  fun `is sent to invited participant`() {
    simulateKafkaListener {
      transactionTemplate.execute {
        cut.sendParticipantActivated(project.identifier, invitedParticipant, invitingParticipant)
      }
    }
    val recipient = mockServer.takeRequest().recipient()

    assertThat(recipient.getString("Email")).isEqualTo(invitedParticipant.user!!.email)
  }

  @Test
  fun `is sent in cc to inviting CSM`() {
    simulateKafkaListener {
      transactionTemplate.execute {
        cut.sendParticipantActivated(project.identifier, invitedParticipant, invitingParticipant)
      }
    }
    val cc = mockServer.takeRequest().cc()

    assertThat(cc.getString("Email")).isEqualTo(invitingUser.email)
    assertThat(cc.getString("Name")).isEqualTo(invitingUser.getDisplayName())
  }

  @Test
  fun `has correct variables`() {
    simulateKafkaListener {
      transactionTemplate.execute {
        cut.sendParticipantActivated(project.identifier, invitedParticipant, invitingParticipant)
      }
    }
    val variables = mockServer.takeRequest().variables()

    assertThat(variables.getString(PROJECT_URL))
        .isEqualTo("https://localhost/projects/${project.identifier}")
    assertThat(variables.getString(RECIPIENT_NAME)).isEqualTo(invitedParticipant.getDisplayName())
  }

  @TestFactory
  fun `uses the correct template id for country code`() =
      templateInvitedProperties.countries.map {
        val countryCode = it.key
        val expectedTemplateId = it.value

        // create one test for each supported country
        dynamicTest(countryCode) {
          mockServer.respondWithSuccess()
          val countryName = IsoCountryCodeEnum.fromCountryCode(countryCode)!!.countryName

          // because the mail language is determined by the country of the originator (who is
          // sending the invitation), we build a company located in the country to be tested
          eventStreamGenerator.submitCompanyWithPostboxAddress {
            it.postBoxAddressBuilder.country = countryName
          }

          val invitingParticipant = repositories.findParticipant(invitingParticipantIdentifier)!!

          simulateKafkaListener {
            transactionTemplate.execute {
              cut.sendParticipantActivated(
                  project.identifier, invitedParticipant, invitingParticipant)
            }
          }
          val request = mockServer.takeRequest()

          assertThat(request.templateId()).isEqualTo(expectedTemplateId)
        }
      }

  @Test
  fun `uses default template for unsupported countries`() {
    val defaultTemplateId = templateInvitedProperties.default
    eventStreamGenerator.submitCompanyWithPostboxAddress {
      it.postBoxAddressBuilder.country = "Greenland"
    }

    simulateKafkaListener {
      transactionTemplate.execute {
        cut.sendParticipantActivated(project.identifier, invitedParticipant, invitingParticipant)
      }
    }
    val request = mockServer.takeRequest()

    assertThat(request.templateId()).isEqualTo(defaultTemplateId)
  }

  @Test
  fun `uses template based on country of street address if given`() {
    val expectedTemplateId = templateInvitedProperties.countries["DE"]!!
    eventStreamGenerator.submitCompanyWithBothAddresses {
      it.streetAddressBuilder.country = "Germany"
      it.postBoxAddressBuilder.country = "United States of America (the)"
    }

    simulateKafkaListener {
      transactionTemplate.execute {
        cut.sendParticipantActivated(project.identifier, invitedParticipant, invitingParticipant)
      }
    }
    val request = mockServer.takeRequest()

    assertThat(request.templateId()).isEqualTo(expectedTemplateId)
  }

  @Test
  fun `uses template based on country of post box address if no street address is given`() {
    val expectedTemplateId = templateInvitedProperties.countries["US"]!!
    eventStreamGenerator.submitCompanyWithPostboxAddress {
      it.postBoxAddressBuilder.country = "United States of America (the)"
    }

    simulateKafkaListener {
      transactionTemplate.execute {
        cut.sendParticipantActivated(project.identifier, invitedParticipant, invitingParticipant)
      }
    }
    val request = mockServer.takeRequest()

    assertThat(request.templateId()).isEqualTo(expectedTemplateId)
  }
}

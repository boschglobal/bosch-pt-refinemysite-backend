/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.command.sideeffects

import com.bosch.pt.csm.cloud.common.command.DenyWebRequests
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.mail.integration.MailContact
import com.bosch.pt.iot.smartsite.mail.integration.MailIntegrationService
import com.bosch.pt.iot.smartsite.project.participant.mail.template.ParticipantActivatedTemplate
import com.bosch.pt.iot.smartsite.project.participant.mail.template.ParticipantAddedTemplate
import com.bosch.pt.iot.smartsite.project.participant.mail.template.ParticipantInvitedTemplate
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import java.text.DateFormat.MEDIUM
import java.text.DateFormat.getDateInstance
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.util.Date
import java.util.Locale
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional

@Service
open class ParticipantMailService(
    @Value("\${domain.name}") private val domainName: String,
    @Value("\${custom.mail.locale.supported}") private val supportedLocales: List<Locale>,
    private val mailIntegrationService: MailIntegrationService,
    private val projectRepository: ProjectRepository
) {

  /** Scenario A */
  @Transactional(propagation = MANDATORY)
  @NoPreAuthorize
  open fun sendParticipantAdded(
      projectIdentifier: ProjectId,
      participant: Participant,
      invitingParticipant: Participant
  ) {
    val project = projectRepository.findOneByIdentifier(projectIdentifier)!!

    mailIntegrationService.sendMail(
        template =
            ParticipantAddedTemplate(
                projectUrl = buildProjectUrl(projectIdentifier),
                projectName = project.getDisplayName(),
                originatorName = invitingParticipant.getDisplayName()!!,
                recipientName = participant.getDisplayName()!!),
        countryCode = invitingParticipant.getCountryCode(),
        to = participant.buildMailContact())
  }

  /** Scenario B1 */
  @Transactional(propagation = MANDATORY)
  @NoPreAuthorize
  open fun sendParticipantInvited(
      projectIdentifier: ProjectId,
      email: String,
      expirationDate: LocalDateTime,
      invitingParticipant: Participant
  ) {
    val project = projectRepository.findOneByIdentifier(projectIdentifier)!!

    mailIntegrationService.sendMail(
        template =
            ParticipantInvitedTemplate(
                projectName = project.getDisplayName(),
                originatorName = invitingParticipant.getDisplayName()!!,
                registerUrl = buildRegisterUrl(),
                expirationDate = supportedLocales.toLocalizedDates(expirationDate)),
        countryCode = invitingParticipant.getCountryCode(),
        to = MailContact(email = email),
        sendBcc = true)
  }

  /** Scenario B2 */
  @Transactional(propagation = MANDATORY)
  @DenyWebRequests
  @NoPreAuthorize
  open fun sendParticipantActivated(
      projectIdentifier: ProjectId,
      participant: Participant,
      invitingParticipant: Participant?
  ) {

    mailIntegrationService.sendMail(
        template =
            ParticipantActivatedTemplate(
                projectUrl = buildProjectUrl(projectIdentifier),
                recipientName = requireNotNull(participant.getDisplayName())),
        countryCode = invitingParticipant?.getCountryCode() ?: participant.getCountryCode(),
        to = MailContact(email = requireNotNull(participant.email)),
        cc = invitingParticipant?.buildMailContact())
  }

  private fun buildProjectUrl(projectIdentifier: ProjectId) =
      String.format(PROJECT_URL_TEMPLATE, domainName, projectIdentifier)

  private fun buildRegisterUrl() = String.format(REGISTER_URL_TEMPLATE, domainName)

  private fun Participant.buildMailContact() = MailContact(user!!.email!!, getDisplayName())

  private fun Participant.getCountryCode(): String? {
    // get country name from company's streetAddress, or fall back to postBoxAddress
    val countryName =
        company!!.streetAddress?.country ?: company!!.postBoxAddress?.country ?: return null

    val countryCode =
        IsoCountryCodeEnum.fromCountryName(countryName)
            ?: IsoCountryCodeEnum.fromAlternativeCountryName(countryName)
    if (countryCode == null) {
      LOGGER.warn("Unable to determine country code from country name $countryName.")
    }
    return countryCode.toString()
  }

  private fun List<Locale>.toLocalizedDates(expirationDate: LocalDateTime) = associate {
    it.language to expirationDate.format(it)
  }

  private fun LocalDateTime.format(locale: Locale) =
      getDateInstance(MEDIUM, locale).format(Date.from(toInstant(UTC)))

  companion object {

    private const val PROJECT_URL_TEMPLATE = "https://%s/projects/%s"

    private const val REGISTER_URL_TEMPLATE = "https://%s/auth/register"

    private val LOGGER = LoggerFactory.getLogger(ParticipantMailService::class.java)
  }
}

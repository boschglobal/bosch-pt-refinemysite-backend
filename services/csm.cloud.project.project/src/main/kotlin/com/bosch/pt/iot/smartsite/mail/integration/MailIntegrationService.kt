/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.mail.integration

import com.bosch.pt.iot.smartsite.application.config.MailjetProperties
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.mail.template.MailTemplate
import com.bosch.pt.iot.smartsite.mail.template.TemplateResolver
import com.bosch.pt.iot.smartsite.mail.util.asString
import com.bosch.pt.iot.smartsite.mail.util.failOnError
import com.mailjet.client.MailjetClient
import com.mailjet.client.transactional.SendContact
import com.mailjet.client.transactional.SendEmailsRequest
import com.mailjet.client.transactional.TransactionalEmail
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation.SUPPORTS
import org.springframework.transaction.annotation.Transactional

@Service
open class MailIntegrationService(
    private val mailjetClient: MailjetClient,
    private val templateResolver: TemplateResolver,
    private val mailjetProperties: MailjetProperties,
) {

  // TODO: [SMAR-12221] make mail sending transactional
  @NoPreAuthorize
  @Transactional(propagation = SUPPORTS)
  open fun sendMail(
      template: MailTemplate,
      countryCode: String?,
      to: MailContact,
      cc: MailContact? = null,
      sendBcc: Boolean = false
  ) {

    val toContact = to.buildSendContact()
    val ccContact = cc?.buildSendContact()
    val bccContact =
        if (sendBcc) {
          mailjetProperties.bcc?.let { buildSendContact(it.email) }
        } else null

    val message =
        TransactionalEmail.builder()
            .templateID(templateResolver.getMailjetTemplateId(template, countryCode))
            .templateLanguage(true)
            .templateErrorReporting(getTemplateErrorReportingEmail())
            .to(toContact)
            .variables(template.variables)
            // invoke cc() or bcc() only with non-null arguments
            .apply { ccContact?.let { cc(it) } }
            .apply { bccContact?.let { bcc(it) } }
            .build()

    val request =
        SendEmailsRequest.builder()
            .message(message)
            .sandboxMode(mailjetProperties.sandboxMode)
            .build()

    LOGGER.debug("Sending mail: ${message.asString()}")
    if (mailjetProperties.enabled) {
      request.sendWith(mailjetClient).failOnError()
    } else {
      LOGGER.warn("Mailjet is disabled. No mail was sent.")
    }
  }

  private fun MailContact.buildSendContact() =
      if (shouldRedirect(email)) {
        buildSendContact(mailjetProperties.redirectMails!!.redirectToEmail, name)
      } else {
        buildSendContact(email, name)
      }

  private fun shouldRedirect(email: String) =
      mailjetProperties.redirectMails?.let { email.matches(Regex(it.recipientEmailPattern)) }
          ?: false

  private fun buildSendContact(email: String, name: String? = null) =
      name?.let { SendContact(email, name) } ?: SendContact(email)

  private fun getTemplateErrorReportingEmail() =
      mailjetProperties.templateErrorReporting?.let { SendContact(it.email) }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(MailIntegrationService::class.java)
  }
}

data class MailContact(val email: String, val name: String? = null)

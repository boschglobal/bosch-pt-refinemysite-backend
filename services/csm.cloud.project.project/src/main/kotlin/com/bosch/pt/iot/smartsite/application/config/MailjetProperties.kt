/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.application.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "custom.mailjet")
data class MailjetProperties(
    val api: ApiCredentials?,

    /**
     * When set, enables sending a mail copy (BCC) to the specified email address. Additionally to
     * this setting, sending a BCC needs to be explicitly requested when sending the mail.
     */
    val bcc: Bcc? = null,

    /** When false, no connection to Mailjet will be established. Mails will not be sent. */
    val enabled: Boolean = true,

    /**
     * When set, mails that match a specified recipient email address pattern will be redirected to
     * another email address. This is helpful on staging environments to deliver all emails
     * addressed to test users to a central environment-specific mailbox.
     */
    val redirectMails: RedirectMails? = null,

    /**
     * When true, the API call will be run in Sandbox mode. This will disable the delivery of the
     * message, but the API will still perform all necessary validations. You will still receive
     * success or error messages related to the processing of the message. If the message is
     * processed successfully, you will receive the standard response payload but without the
     * Message ID and UUID. (source: see below)
     *
     * @see <a href="https://dev.mailjet.com/email/reference/send-emails/">Send API</>
     */
    val sandboxMode: Boolean = false,

    /**
     * When set, Mailjet will send error reports to this address in case of template errors. When
     * sending mails, template language must be set to true.
     *
     * @see <a href="https://dev.mailjet.com/email/reference/send-emails/">Send API</>
     */
    val templateErrorReporting: TemplateErrorReporting? = null
) {

  data class ApiCredentials(val key: String, val secret: String)

  data class Bcc(val email: String)

  data class TemplateErrorReporting(val email: String)

  data class RedirectMails(val recipientEmailPattern: String, val redirectToEmail: String)
}

/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.mail.util

import com.mailjet.client.transactional.TransactionalEmail
import com.mailjet.client.transactional.response.SendEmailError
import com.mailjet.client.transactional.response.SendEmailsResponse
import com.mailjet.client.transactional.response.SentMessageStatus
import org.apache.commons.lang3.builder.RecursiveToStringStyle
import org.apache.commons.lang3.builder.ToStringBuilder

fun SendEmailsResponse.failOnError() {
  this.messages.forEach {
    if (it.status == SentMessageStatus.ERROR) {
      throw IllegalArgumentException("Error(s) sending mail: ${it.errors.asString()}")
    }
  }
}

/*
 * Mailjet's SendEmailError class does not implement toString() properly.
 * This workaround can be removed once that is fixed in the Mailjet library.
 */
fun Array<SendEmailError>.asString() =
    this.map { ToStringBuilder.reflectionToString(it) }.toString()

/*
 * Mailjet's TransactionalEmail class does not implement toString() properly.
 * This workaround can be removed once that is fixed in the Mailjet library.
 */
fun TransactionalEmail.asString(): String =
    ToStringBuilder.reflectionToString(this, RecursiveToStringStyle())

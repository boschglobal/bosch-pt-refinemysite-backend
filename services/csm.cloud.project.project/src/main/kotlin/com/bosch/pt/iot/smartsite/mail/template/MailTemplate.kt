/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.mail.template

abstract class MailTemplate(val templateName: String) {

  abstract val variables: Map<String, Any>
}

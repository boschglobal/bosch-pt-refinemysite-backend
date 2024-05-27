/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.common.exceptions

class ResourceNotFoundException : RuntimeException {
  val messageKey: String

  constructor(messageKey: String) {
    this.messageKey = messageKey
  }

  constructor(messageKey: String, cause: Throwable?) : super(cause) {
    this.messageKey = messageKey
  }
}

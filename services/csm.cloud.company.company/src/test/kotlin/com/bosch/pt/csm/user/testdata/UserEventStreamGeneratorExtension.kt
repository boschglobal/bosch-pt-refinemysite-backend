/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.user.testdata

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser

fun EventStreamGenerator.submitUserCreatedUnregistered(asReference: String = "user") =
    this.submitUser(asReference) {
      it.firstName = null
      it.lastName = null
      it.email = "max@mustermann.de"
      it.gender = null
      it.admin = false
      it.locked = true
      it.registered = false
    }

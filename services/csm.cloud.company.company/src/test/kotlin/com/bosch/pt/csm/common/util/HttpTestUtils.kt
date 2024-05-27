/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.common.util

import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.util.Assert
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

object HttpTestUtils {

  fun setFakeUrlWithApiVersion() {
    val attrs = RequestContextHolder.getRequestAttributes()
    Assert.state(attrs is ServletRequestAttributes, "No current ServletRequestAttributes")

    val request = (attrs as ServletRequestAttributes).request as MockHttpServletRequest
    request.requestURI = "/v2/fake-url"
  }
}

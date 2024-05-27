/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.common.util;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class HttpTestUtils {

  public static final int API_VERSION_DEFAULT = 1;

  private HttpTestUtils() {}

  public static void setFakeUrlWithApiVersion() {
    setFakeUrlWithApiVersion(API_VERSION_DEFAULT);
  }

  public static void setFakeUrlWithApiVersion(int version) {
    RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
    if (attrs == null) {
      attrs = new ServletRequestAttributes(new MockHttpServletRequest());
      RequestContextHolder.setRequestAttributes(attrs);
    }
    Assert.state(attrs instanceof ServletRequestAttributes, "No current ServletRequestAttributes");
    MockHttpServletRequest request =
        (MockHttpServletRequest) ((ServletRequestAttributes) attrs).getRequest();
    request.setRequestURI("/v" + version + "/fake-url");
  }
}

/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest

import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationStrategyTest
import java.text.SimpleDateFormat
import java.util.TimeZone

open class AbstractNotificationApiTest : BaseNotificationStrategyTest() {

  protected val df =
      SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").apply {
        timeZone = TimeZone.getTimeZone("UTC")
      }
}

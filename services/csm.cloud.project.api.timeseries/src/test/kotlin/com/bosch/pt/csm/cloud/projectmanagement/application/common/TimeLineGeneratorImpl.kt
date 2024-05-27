/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.common

import com.bosch.pt.csm.cloud.common.test.DefaultTimeLineGenerator
import org.springframework.stereotype.Component

@Component
class TimeLineGeneratorImpl : DefaultTimeLineGenerator() {

  fun next(millis: Long = 5000) = time.apply { plusMillis(millis) }
}

/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.test

import com.bosch.pt.csm.cloud.common.test.DefaultTimeLineGenerator
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.strategies.notifications.TaskNotificationMerger
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class TimeLineGeneratorImpl : DefaultTimeLineGenerator() {

    fun next(millis: Long = 5000): Instant {
        this.time = this.time.plusMillis(millis)
        return this.time
    }

    fun relativeToPrevious(millis: Long = 50): Instant = time.plusMillis(millis)

    companion object {
        const val LESS_THAN_LOOK_BACK_SECONDS = TaskNotificationMerger.LOOK_BACK_SECONDS * 1000 - 10
    }
}

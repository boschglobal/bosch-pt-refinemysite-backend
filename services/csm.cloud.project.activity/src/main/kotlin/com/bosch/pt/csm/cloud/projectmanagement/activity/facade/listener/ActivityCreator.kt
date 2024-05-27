/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.activity.facade.listener

import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.activity.service.ActivityService
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.activity.ActivityStrategy
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component

@Component
class ActivityCreator(
    private val activityStrategies: Set<ActivityStrategy>,
    private val activityService: ActivityService
) {

  fun createActivities(key: EventMessageKey, value: SpecificRecordBase?) {
    if (key !is AggregateEventMessageKey) return

    if (!activityService.activityExists(key.aggregateIdentifier)) {
      val activities =
          activityStrategies.filter { it.handles(key, value) }.map { it.apply(key, value) }.toSet()
      if (activities.isNotEmpty()) {
        activityService.saveAll(activities)
      }
    }
  }
}

/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2021
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.common.extensions.toLocalDate
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.activity.AggregateComparator
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.activity.AttributeChangeEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.activity.AttributeChangeEnum.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.activity.AttributeChangeEnum.REMOVED
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.activity.AttributeChangeEnum.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.model.TaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.model.TaskScheduleSlot
import java.util.Date
import java.util.UUID
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.stereotype.Component

@Component
class TaskScheduleAggregateComparator(mongoOperations: MongoOperations) :
    AggregateComparator(mongoOperations) {

  fun compare(
      currentVersion: TaskSchedule,
      previousVersion: TaskSchedule
  ): Collection<TaskScheduleAttributeChange> {

    val changedAttributes =
        super.compare(currentVersion, previousVersion)
            .apply { remove("slots") }
            .map {
              TaskScheduleAttributeChange(
                  attribute = it.value.attribute,
                  attributeIdentifier = null,
                  oldValue =
                      with(it.value.oldValue) {
                        when (this) {
                          is Date -> this.toLocalDate()
                          else -> this
                        }
                      },
                  newValue =
                      with(it.value.newValue) {
                        when (this) {
                          is Date -> this.toLocalDate()
                          else -> this
                        }
                      },
                  changeType = it.value.changeType)
            }
            .toSet()

    val slotsChangedAttributes = compareSlots(currentVersion.slots, previousVersion.slots)

    return changedAttributes + slotsChangedAttributes
  }

  private fun compareSlots(
      currentSlots: Collection<TaskScheduleSlot>,
      previousSlots: Collection<TaskScheduleSlot>
  ): Collection<TaskScheduleAttributeChange> {

    val currentSlotsMap = currentSlots.map { it.dayCardIdentifier to it.date }.toMap()
    val previousSlotsMap = previousSlots.map { it.dayCardIdentifier to it.date }.toMap()

    // Insert
    val insertAttributes =
        currentSlotsMap.filterNot { previousSlotsMap.contains(it.key) }.map {
          TaskScheduleAttributeChange(
              attribute = "daycard",
              attributeIdentifier = it.key,
              oldValue = null,
              newValue = it.value,
              changeType = CREATED)
        }

    // Update
    val updateAttributes =
        currentSlotsMap
            .filter {
              previousSlotsMap.contains(it.key) && previousSlotsMap.getValue(it.key) != it.value
            }
            .map {
              TaskScheduleAttributeChange(
                  attribute = "daycard",
                  attributeIdentifier = it.key,
                  oldValue = previousSlotsMap.getValue(it.key),
                  newValue = it.value,
                  changeType = UPDATED)
            }

    // Delete
    val deleteAttributes =
        previousSlotsMap.filterNot { currentSlotsMap.contains(it.key) }.map {
          TaskScheduleAttributeChange(
              attribute = "daycard",
              attributeIdentifier = it.key,
              oldValue = it.value,
              newValue = null,
              changeType = REMOVED)
        }

    return insertAttributes + updateAttributes + deleteAttributes
  }
}

data class TaskScheduleAttributeChange(
    val attribute: String,
    val attributeIdentifier: UUID?,
    val oldValue: Any?,
    val newValue: Any?,
    val changeType: AttributeChangeEnum
)

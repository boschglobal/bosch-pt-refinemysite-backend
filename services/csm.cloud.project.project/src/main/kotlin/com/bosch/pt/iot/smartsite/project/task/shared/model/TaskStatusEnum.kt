/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.shared.model

import com.bosch.pt.iot.smartsite.common.model.Sortable

/**
 * Do not reorder enum items. Their position (not the value of the variable position ) is important
 * for comparisons in [TaskPrecondition].
 */
enum class TaskStatusEnum(private val position: Int) : Sortable {
  DRAFT(50),
  OPEN(100),
  STARTED(200),
  CLOSED(300),
  ACCEPTED(400);

  override fun getPosition(): Int = position

  companion object {

    /** Valid enum values for documentation. */
    const val ENUM_VALUES = "DRAFT,OPEN,STARTED,CLOSED,ACCEPTED"
  }
}

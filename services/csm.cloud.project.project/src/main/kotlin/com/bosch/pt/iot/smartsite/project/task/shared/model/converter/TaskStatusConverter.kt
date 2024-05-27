/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.shared.model.converter

import com.bosch.pt.iot.smartsite.common.model.Sortable
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/** Converter for [task status][TaskStatusEnum] used in [entities][Entity]. */
@Converter(autoApply = true)
class TaskStatusConverter : AttributeConverter<TaskStatusEnum, Int> {

  override fun convertToDatabaseColumn(attribute: TaskStatusEnum): Int = attribute.getPosition()

  override fun convertToEntityAttribute(dbData: Int): TaskStatusEnum =
      Sortable.get(TaskStatusEnum::class.java, dbData)
}

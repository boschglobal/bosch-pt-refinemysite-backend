/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workday.query.model

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.domain.WorkDayConfigurationId
import com.bosch.pt.csm.cloud.projectmanagement.translation.shared.model.TranslatableEnum
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import java.time.LocalDate
import java.time.LocalDateTime
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document

const val WORK_DAY_CONFIGURATION_PROJECTION = "WorkDayConfigurationProjection"

@Document(WORK_DAY_CONFIGURATION_PROJECTION)
@TypeAlias(WORK_DAY_CONFIGURATION_PROJECTION)
data class WorkDayConfiguration(
    @Id val identifier: WorkDayConfigurationId,
    val project: ProjectId,
    val version: Long,
    val startOfWeek: DayEnum,
    val workingDays: List<DayEnum>,
    val holidays: List<Holiday>,
    val allowWorkOnNonWorkingDays: Boolean,
    val deleted: Boolean = false,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime,
    val history: List<WorkDayConfigurationVersion>
)

data class WorkDayConfigurationVersion(
    val version: Long,
    val startOfWeek: DayEnum,
    val workingDays: List<DayEnum>,
    val holidays: List<Holiday>,
    val allowWorkOnNonWorkingDays: Boolean,
    val deleted: Boolean = false,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime,
)

data class Holiday(val name: String, val date: LocalDate)

enum class DayEnum(private val day: String) : TranslatableEnum {
  MONDAY("MONDAY"),
  TUESDAY("TUESDAY"),
  WEDNESDAY("WEDNESDAY"),
  THURSDAY("THURSDAY"),
  FRIDAY("FRIDAY"),
  SATURDAY("SATURDAY"),
  SUNDAY("SUNDAY");

  companion object {
    const val KEY_PREFIX: String = "DAY_"
  }

  val shortKey: String
    get() = this.day

  override val key: String
    get() = "${KEY_PREFIX}${this.day}"

  override val messageKey: String
    get() = "${DayEnum::class.simpleName}_$this"
}

/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workday.command.handler

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.common.i18n.Key.WORKDAY_CONFIGURATION_VALIDATION_ERROR_DUPLICATED_HOLIDAY
import com.bosch.pt.iot.smartsite.common.i18n.Key.WORKDAY_CONFIGURATION_VALIDATION_ERROR_DUPLICATED_WORKDAY
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.workday.command.api.UpdateWorkdayConfigurationCommand
import com.bosch.pt.iot.smartsite.project.workday.command.snapshotshore.WorkdayConfigurationSnapshotStore
import com.bosch.pt.iot.smartsite.project.workday.domain.WorkdayConfigurationId
import com.bosch.pt.iot.smartsite.project.workday.shared.model.Holiday
import com.bosch.pt.iot.smartsite.project.workday.shared.model.Holiday.Companion.MAX_HOLIDAY_AMOUNT
import com.bosch.pt.iot.smartsite.project.workday.shared.repository.WorkdayConfigurationRepository
import datadog.trace.api.Trace
import java.time.DayOfWeek
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class UpdateWorkdayConfigurationCommandHandler(
    private val snapshotStore: WorkdayConfigurationSnapshotStore,
    private val eventBus: ProjectContextLocalEventBus,
    private val workdayConfigurationRepository: WorkdayConfigurationRepository
) {

  @Trace
  @Transactional
  @PreAuthorize("@projectAuthorizationComponent.hasUpdatePermissionOnProject(#command.projectRef)")
  open fun handle(command: UpdateWorkdayConfigurationCommand): WorkdayConfigurationId {
    assertDuplicatedWorkdays(command.workingDays)
    assertDuplicatedHolidays(command.holidays)
    assertMaxNumberOfHolidays(command.holidays)

    val workdayConfigurationIdentifier =
        requireNotNull(
            workdayConfigurationRepository.findIdentifierByProjectIdentifier(command.projectRef)) {
              "Could not find Project ${command.projectRef}"
            }

    return snapshotStore
        .findOrFail(workdayConfigurationIdentifier)
        .toCommandHandler()
        .assertVersionMatches(command.version)
        .update {
          it.copy(
              startOfWeek = command.startOfWeek,
              workingDays = command.workingDays.toSet(),
              holidays = command.holidays.toSet(),
              allowWorkOnNonWorkingDays = command.allowWorkOnNonWorkingDays)
        }
        .emitEvent(UPDATED)
        .ifSnapshotWasChanged()
        .to(eventBus)
        .andReturnSnapshot()
        .identifier
  }
}

// Validates if multiple work days exists.
private fun assertDuplicatedWorkdays(workdays: List<DayOfWeek>) {
  if (workdays.size != workdays.distinct().size) {
    throw PreconditionViolationException(WORKDAY_CONFIGURATION_VALIDATION_ERROR_DUPLICATED_WORKDAY)
  }
}

// Validates if multiple holidays exists.
private fun assertDuplicatedHolidays(holidays: List<Holiday>) {
  val holidayMap = holidays.groupBy(Holiday::date, Holiday::name)

  if (holidayMap.values.any { it.size != it.distinctBy(String::lowercase).size }) {
    throw PreconditionViolationException(WORKDAY_CONFIGURATION_VALIDATION_ERROR_DUPLICATED_HOLIDAY)
  }
}

// Validates if number of holidays does not exceed the max value.
private fun assertMaxNumberOfHolidays(holidays: List<Holiday>) {
  require(holidays.size <= MAX_HOLIDAY_AMOUNT)
}

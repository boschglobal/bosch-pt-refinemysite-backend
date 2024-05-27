/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workday.command.handler

import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot.Companion.INITIAL_SNAPSHOT_VERSION
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventEnumAvro.CREATED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.workday.command.api.CreateWorkdayConfigurationCommand
import com.bosch.pt.iot.smartsite.project.workday.command.snapshotshore.WorkdayConfigurationSnapshot
import com.bosch.pt.iot.smartsite.project.workday.domain.WorkdayConfigurationId
import datadog.trace.api.Trace
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class CreateWorkdayConfigurationCommandHandler(
    private val eventBus: ProjectContextLocalEventBus
) {

  @Trace
  @Transactional
  @NoPreAuthorize
  open fun handle(command: CreateWorkdayConfigurationCommand) =
      WorkdayConfigurationSnapshot(
              identifier = WorkdayConfigurationId(),
              version = INITIAL_SNAPSHOT_VERSION,
              projectRef = command.projectRef,
              startOfWeek = MONDAY,
              workingDays = setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY),
              holidays = emptySet(),
              allowWorkOnNonWorkingDays = true)
          .toCommandHandler()
          .emitEvent(CREATED)
          .to(eventBus)
          .andReturnSnapshot()
          .identifier
}

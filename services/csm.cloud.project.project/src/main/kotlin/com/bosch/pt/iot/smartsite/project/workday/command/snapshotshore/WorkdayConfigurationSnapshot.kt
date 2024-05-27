/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workday.command.snapshotshore

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.handler.CommandHandler
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditableSnapshot
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.workday.command.mapper.WorkdayConfigurationAvroSnapshotMapper
import com.bosch.pt.iot.smartsite.project.workday.domain.WorkdayConfigurationId
import com.bosch.pt.iot.smartsite.project.workday.shared.model.Holiday
import com.bosch.pt.iot.smartsite.project.workday.shared.model.WorkdayConfiguration
import java.time.DayOfWeek
import java.time.LocalDateTime

data class WorkdayConfigurationSnapshot(
    override val identifier: WorkdayConfigurationId,
    override val version: Long,
    override val createdBy: UserId? = null,
    override val createdDate: LocalDateTime? = null,
    override val lastModifiedBy: UserId? = null,
    override val lastModifiedDate: LocalDateTime? = null,
    val projectRef: ProjectId,
    val startOfWeek: DayOfWeek,
    val workingDays: Set<DayOfWeek>,
    val holidays: Set<Holiday>,
    val allowWorkOnNonWorkingDays: Boolean
) : VersionedSnapshot, AuditableSnapshot {

  constructor(
      workdayConfiguration: WorkdayConfiguration
  ) : this(
      workdayConfiguration.identifier,
      workdayConfiguration.version,
      workdayConfiguration.createdBy.get(),
      workdayConfiguration.createdDate.get(),
      workdayConfiguration.lastModifiedBy.get(),
      workdayConfiguration.lastModifiedDate.get(),
      workdayConfiguration.project.identifier,
      workdayConfiguration.startOfWeek,
      workdayConfiguration.workingDays,
      workdayConfiguration.holidays,
      workdayConfiguration.allowWorkOnNonWorkingDays)

  fun toCommandHandler() = CommandHandler.of(this, WorkdayConfigurationAvroSnapshotMapper)
}

fun WorkdayConfiguration.asSnapshot() = WorkdayConfigurationSnapshot(this)

/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.project.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.handler.CommandHandler
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditableSnapshot
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.iot.smartsite.common.i18n.Key.PROJECT_VALIDATION_ERROR_START_DATE_AFTER_END_DATE
import com.bosch.pt.iot.smartsite.project.project.ProjectAddressVo
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.command.mapper.ProjectAvroSnapshotMapper
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectAddress
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectCategoryEnum
import java.time.LocalDate
import java.time.LocalDateTime

data class ProjectSnapshot(
    override val identifier: ProjectId,
    override val version: Long = VersionedSnapshot.INITIAL_SNAPSHOT_VERSION,
    override val createdBy: UserId? = null,
    override val createdDate: LocalDateTime? = null,
    override val lastModifiedBy: UserId? = null,
    override val lastModifiedDate: LocalDateTime? = null,
    val client: String? = null,
    val description: String? = null,
    val start: LocalDate,
    val end: LocalDate,
    val projectNumber: String,
    val title: String,
    val category: ProjectCategoryEnum? = null,
    val address: ProjectAddressVo? = null,
    val deleted: Boolean = false
) : VersionedSnapshot, AuditableSnapshot {

  constructor(
      project: Project
  ) : this(
      project.identifier,
      project.version,
      project.createdBy.get(),
      project.createdDate.get(),
      project.lastModifiedBy.get(),
      project.lastModifiedDate.get(),
      project.client,
      project.description,
      project.start,
      project.end,
      project.projectNumber,
      project.title,
      project.category,
      project.projectAddress?.toValueObject(),
      project.deleted)

  init {
    invariably(
        startDateBeforeOrEqualToEndDate(), PROJECT_VALIDATION_ERROR_START_DATE_AFTER_END_DATE)
  }

  private fun invariably(requiredCondition: Boolean, failureMessageKey: String) {
    if (!requiredCondition) throw PreconditionViolationException(failureMessageKey)
  }

  private fun startDateBeforeOrEqualToEndDate() = start.isBefore(end) || start.isEqual(end)

  fun toCommandHandler() = CommandHandler.of(this, ProjectAvroSnapshotMapper)
}

fun Project.asSnapshot() = ProjectSnapshot(this)

fun ProjectAddress.toValueObject() =
    ProjectAddressVo(
        street = this.street,
        houseNumber = this.houseNumber,
        city = this.city,
        zipCode = this.zipCode)

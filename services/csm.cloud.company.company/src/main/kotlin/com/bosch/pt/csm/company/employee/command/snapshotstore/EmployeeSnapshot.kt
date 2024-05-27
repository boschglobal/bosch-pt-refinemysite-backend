/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.employee.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.handler.CommandHandler
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditableSnapshot
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot
import com.bosch.pt.csm.company.company.CompanyId
import com.bosch.pt.csm.company.company.asCompanyId
import com.bosch.pt.csm.company.employee.EmployeeId
import com.bosch.pt.csm.company.employee.command.mapper.EmployeeAvroSnapshotMapper
import com.bosch.pt.csm.company.employee.shared.model.Employee
import com.bosch.pt.csm.company.employee.shared.model.EmployeeRoleEnum
import java.time.LocalDateTime

data class EmployeeSnapshot(
    override val identifier: EmployeeId,
    override val version: Long = VersionedSnapshot.INITIAL_SNAPSHOT_VERSION,
    override val createdDate: LocalDateTime? = null,
    override val createdBy: UserId? = null,
    override val lastModifiedDate: LocalDateTime? = null,
    override val lastModifiedBy: UserId? = null,
    val userRef: UserId,
    val companyRef: CompanyId,
    val roles: List<EmployeeRoleEnum>
) : VersionedSnapshot, AuditableSnapshot {

  constructor(
      employee: Employee
  ) : this(
      employee.identifier,
      employee.version,
      employee.createdDate.get(),
      employee.createdBy.get(),
      employee.lastModifiedDate.get(),
      employee.lastModifiedBy.get(),
      employee.userRef,
      employee.company.getIdentifierUuid().asCompanyId(),
      employee.roles!!)

  fun toCommandHandler() = CommandHandler.of(this, EmployeeAvroSnapshotMapper)
}

fun Employee.asSnapshot() = EmployeeSnapshot(this)

/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.employee.command.mapper

import com.bosch.pt.csm.cloud.common.command.mapper.AbstractAvroSnapshotMapper
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.companymanagement.common.CompanymanagementAggregateTypeEnum.COMPANY
import com.bosch.pt.csm.cloud.companymanagement.common.CompanymanagementAggregateTypeEnum.EMPLOYEE
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventEnumAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro.valueOf
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.company.employee.command.snapshotstore.EmployeeSnapshot

object EmployeeAvroSnapshotMapper : AbstractAvroSnapshotMapper<EmployeeSnapshot>() {

  override fun <E : Enum<*>> toAvroMessageWithNewVersion(
      snapshot: EmployeeSnapshot,
      eventType: E
  ): EmployeeEventAvro =
      with(snapshot) {
        EmployeeEventAvro.newBuilder()
            .setName(eventType as EmployeeEventEnumAvro)
            .setAggregateBuilder(
                EmployeeAggregateAvro.newBuilder()
                    .setAggregateIdentifier(toAggregateIdentifierAvroWithNextVersion(snapshot))
                    .setAuditingInformation(toUpdatedAuditingInformationAvro(snapshot))
                    .setUser(
                        AggregateIdentifierAvro.newBuilder()
                            .setIdentifier(this.userRef.toString())
                            .setVersion(0)
                            .setType(USER.value)
                            .build())
                    .setCompany(
                        AggregateIdentifierAvro.newBuilder()
                            .setIdentifier(this.companyRef.toString())
                            .setVersion(0)
                            .setType(COMPANY.value)
                            .build())
                    .setRoles(this.roles.map { role -> valueOf(role.name) }))
            .build()
      }

  override fun getAggregateType() = EMPLOYEE.name

  override fun getRootContextIdentifier(snapshot: EmployeeSnapshot) = snapshot.companyRef.toUuid()
}

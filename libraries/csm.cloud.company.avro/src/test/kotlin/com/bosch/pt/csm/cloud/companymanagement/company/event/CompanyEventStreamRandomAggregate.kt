/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

@file:JvmName("CompanyEventStreamRandomAggregate")

package com.bosch.pt.csm.cloud.companymanagement.company.event

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.AuditingInformationAvro
import com.bosch.pt.csm.cloud.common.test.randomLong
import com.bosch.pt.csm.cloud.common.test.randomString
import com.bosch.pt.csm.cloud.companymanagement.common.CompanymanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.companymanagement.company.CompanyReferencedAggregateTypesEnum
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventEnumAvro

fun randomCompany(
    block: ((CompanyAggregateAvro) -> Unit)? = null,
    event: CompanyEventEnumAvro = CompanyEventEnumAvro.CREATED
): CompanyEventAvro.Builder {
  val company =
      CompanyAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              randomIdentifier(CompanymanagementAggregateTypeEnum.COMPANY.value))
          .setAuditingInformation(randomAuditing())
          .setName(randomString())
          .build()
          .also { block?.invoke(it) }

  return CompanyEventAvro.newBuilder().setAggregate(company).setName(event)
}

fun randomEmployee(
    block: ((EmployeeAggregateAvro) -> Unit)? = null,
    event: EmployeeEventEnumAvro = EmployeeEventEnumAvro.CREATED
): EmployeeEventAvro.Builder {
  val employee =
      EmployeeAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              randomIdentifier(CompanymanagementAggregateTypeEnum.EMPLOYEE.value))
          .setAuditingInformation(randomAuditing())
          .setCompanyBuilder(randomIdentifier(CompanymanagementAggregateTypeEnum.COMPANY.value))
          .setRoles(emptyList())
          .setUserBuilder(randomIdentifier(CompanyReferencedAggregateTypesEnum.USER.value))
          .build()
          .also { block?.invoke(it) }

  return EmployeeEventAvro.newBuilder().setAggregate(employee).setName(event)
}

fun randomAuditing(block: ((AuditingInformationAvro) -> Unit)? = null): AuditingInformationAvro =
    AuditingInformationAvro.newBuilder()
        .setCreatedByBuilder(randomIdentifier())
        .setCreatedDate(randomLong())
        .setLastModifiedByBuilder(randomIdentifier())
        .setLastModifiedDate(randomLong())
        .build()
        .also { block?.invoke(it) }

private fun randomIdentifier(type: String = randomString()) =
    AggregateIdentifierAvro.newBuilder().setIdentifier(randomString()).setType(type).setVersion(0)

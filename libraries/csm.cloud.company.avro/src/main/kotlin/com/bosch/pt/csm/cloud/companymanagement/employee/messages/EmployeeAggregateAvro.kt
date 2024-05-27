/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.companymanagement.employee.messages

import com.bosch.pt.csm.cloud.common.extensions.toInstantByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier

fun EmployeeAggregateAvro.getCompanyIdentifier() = getCompany().getIdentifier().toUUID()

fun EmployeeAggregateAvro.getUserIdentifier() = getUser().getIdentifier().toUUID()

fun EmployeeAggregateAvro.getIdentifier() = getAggregateIdentifier().getIdentifier().toUUID()

fun EmployeeAggregateAvro.getVersion() = getAggregateIdentifier().getVersion()

fun EmployeeAggregateAvro.getCreatedDate() =
    getAuditingInformation().getCreatedDate().toInstantByMillis()

fun EmployeeAggregateAvro.getCreatedByUserIdentifier() =
    getAuditingInformation().getCreatedBy().getIdentifier().toUUID()

fun EmployeeAggregateAvro.getLastModifiedDate() =
    getAuditingInformation().getLastModifiedDate().toInstantByMillis()

fun EmployeeAggregateAvro.getLastModifiedByUserIdentifier() =
    getAuditingInformation().getLastModifiedBy().getIdentifier().toUUID()

fun EmployeeAggregateAvro.buildAggregateIdentifier() =
    getAggregateIdentifier().buildAggregateIdentifier()

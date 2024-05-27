/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.companymanagement.company.messages

import com.bosch.pt.csm.cloud.common.extensions.toInstantByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier

fun CompanyAggregateAvro.getIdentifier() = getAggregateIdentifier().getIdentifier().toUUID()

fun CompanyAggregateAvro.getVersion() = getAggregateIdentifier().getVersion()

fun CompanyAggregateAvro.getCreatedDate() =
    getAuditingInformation().getCreatedDate().toInstantByMillis()

fun CompanyAggregateAvro.getCreatedByUserIdentifier() =
    getAuditingInformation().getCreatedBy().getIdentifier().toUUID()

fun CompanyAggregateAvro.getLastModifiedDate() =
    getAuditingInformation().getLastModifiedDate().toInstantByMillis()

fun CompanyAggregateAvro.getLastModifiedByUserIdentifier() =
    getAuditingInformation().getLastModifiedBy().getIdentifier().toUUID()

fun CompanyAggregateAvro.buildAggregateIdentifier() =
    getAggregateIdentifier().buildAggregateIdentifier()

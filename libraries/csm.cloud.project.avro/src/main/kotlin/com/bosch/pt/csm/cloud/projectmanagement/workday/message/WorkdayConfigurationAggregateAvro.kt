/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.workday.message

import com.bosch.pt.csm.cloud.common.extensions.toInstantByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationAggregateAvro

fun WorkdayConfigurationAggregateAvro.getIdentifier() = aggregateIdentifier.identifier.toUUID()

fun WorkdayConfigurationAggregateAvro.getProjectIdentifier() = project.identifier.toUUID()

fun WorkdayConfigurationAggregateAvro.getCreatedDate() =
    auditingInformation.createdDate.toInstantByMillis()

fun WorkdayConfigurationAggregateAvro.getCreatedByUserIdentifier() =
    auditingInformation.createdBy.identifier.toUUID()

fun WorkdayConfigurationAggregateAvro.getLastModifiedDate() =
    auditingInformation.lastModifiedDate.toInstantByMillis()

fun WorkdayConfigurationAggregateAvro.getLastModifiedByUserIdentifier() =
    auditingInformation.lastModifiedBy.identifier.toUUID()

fun WorkdayConfigurationAggregateAvro.buildAggregateIdentifier() =
    aggregateIdentifier.buildAggregateIdentifier()

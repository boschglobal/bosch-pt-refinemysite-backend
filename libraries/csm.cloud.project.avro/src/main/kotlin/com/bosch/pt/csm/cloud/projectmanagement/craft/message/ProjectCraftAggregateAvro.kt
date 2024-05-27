/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.craft.message

import com.bosch.pt.csm.cloud.common.extensions.toInstantByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftAggregateG2Avro

fun ProjectCraftAggregateG2Avro.getIdentifier() = aggregateIdentifier.identifier.toUUID()

fun ProjectCraftAggregateG2Avro.getProjectIdentifier() = project.identifier.toUUID()

fun ProjectCraftAggregateG2Avro.getCreatedDate() =
    auditingInformation.createdDate.toInstantByMillis()

fun ProjectCraftAggregateG2Avro.getCreatedByUserIdentifier() =
    auditingInformation.createdBy.identifier.toUUID()

fun ProjectCraftAggregateG2Avro.getLastModifiedDate() =
    auditingInformation.lastModifiedDate.toInstantByMillis()

fun ProjectCraftAggregateG2Avro.getLastModifiedByUserIdentifier() =
    auditingInformation.lastModifiedBy.identifier.toUUID()

fun ProjectCraftAggregateG2Avro.buildAggregateIdentifier() =
    aggregateIdentifier.buildAggregateIdentifier()

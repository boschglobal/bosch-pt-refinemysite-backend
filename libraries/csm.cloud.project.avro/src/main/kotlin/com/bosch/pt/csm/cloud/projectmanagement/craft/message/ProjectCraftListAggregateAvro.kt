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
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListAggregateAvro

fun ProjectCraftListAggregateAvro.getIdentifier() = aggregateIdentifier.identifier.toUUID()

fun ProjectCraftListAggregateAvro.getProjectIdentifier() = project.identifier.toUUID()

fun ProjectCraftListAggregateAvro.getCreatedDate() =
    auditingInformation.createdDate.toInstantByMillis()

fun ProjectCraftListAggregateAvro.getCreatedByUserIdentifier() =
    auditingInformation.createdBy.identifier.toUUID()

fun ProjectCraftListAggregateAvro.getLastModifiedDate() =
    auditingInformation.lastModifiedDate.toInstantByMillis()

fun ProjectCraftListAggregateAvro.getLastModifiedByUserIdentifier() =
    auditingInformation.lastModifiedBy.identifier.toUUID()

fun ProjectCraftListAggregateAvro.buildAggregateIdentifier() =
    aggregateIdentifier.buildAggregateIdentifier()

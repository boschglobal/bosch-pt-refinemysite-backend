/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.external.message

import com.bosch.pt.csm.cloud.common.extensions.toInstantByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.external.messages.ExternalIdAggregateAvro

fun ExternalIdAggregateAvro.getIdentifier() = this.aggregateIdentifier.identifier.toUUID()

fun ExternalIdAggregateAvro.getVersion() = this.aggregateIdentifier.version

fun ExternalIdAggregateAvro.getProjectIdentifier() = this.project.identifier.toUUID()

fun ExternalIdAggregateAvro.getCreatedBy() = this.auditingInformation.createdBy.identifier.toUUID()

fun ExternalIdAggregateAvro.getCreatedDate() =
    this.auditingInformation.createdDate.toInstantByMillis()

fun ExternalIdAggregateAvro.getLastModifiedBy() =
    this.auditingInformation.lastModifiedBy.identifier.toUUID()

fun ExternalIdAggregateAvro.getLastModifiedDate() =
    this.auditingInformation.lastModifiedDate.toInstantByMillis()

fun ExternalIdAggregateAvro.buildAggregateIdentifier() =
    this.aggregateIdentifier.buildAggregateIdentifier()

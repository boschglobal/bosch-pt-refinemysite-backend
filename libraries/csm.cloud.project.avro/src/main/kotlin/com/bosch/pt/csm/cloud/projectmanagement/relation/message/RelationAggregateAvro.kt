/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.relation.message

import com.bosch.pt.csm.cloud.common.extensions.toInstantByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationAggregateAvro

fun RelationAggregateAvro.getIdentifier() = getAggregateIdentifier().getIdentifier().toUUID()

fun RelationAggregateAvro.getVersion() = getAggregateIdentifier().getVersion()

fun RelationAggregateAvro.buildAggregateIdentifier() =
    getAggregateIdentifier().buildAggregateIdentifier()

fun RelationAggregateAvro.getProjectIdentifier() = getProject().getIdentifier().toUUID()

fun RelationAggregateAvro.getSourceIdentifier() = getSource().getIdentifier().toUUID()

fun RelationAggregateAvro.getTargetIdentifier() = getTarget().getIdentifier().toUUID()

fun RelationAggregateAvro.getCreatedBy() =
    getAuditingInformation().getCreatedBy().getIdentifier().toUUID()

fun RelationAggregateAvro.getCreatedDate() =
    getAuditingInformation().getCreatedDate().toInstantByMillis()

fun RelationAggregateAvro.getLastModifiedBy() =
    getAuditingInformation().getLastModifiedBy().getIdentifier().toUUID()

fun RelationAggregateAvro.getLastModifiedDate() =
    getAuditingInformation().getLastModifiedDate().toInstantByMillis()

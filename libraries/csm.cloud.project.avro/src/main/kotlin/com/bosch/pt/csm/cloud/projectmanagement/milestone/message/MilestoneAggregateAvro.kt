/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.milestone.message

import com.bosch.pt.csm.cloud.common.extensions.toInstantByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneAggregateAvro

fun MilestoneAggregateAvro.getIdentifier() = getAggregateIdentifier().getIdentifier().toUUID()

fun MilestoneAggregateAvro.getVersion() = getAggregateIdentifier().getVersion()

fun MilestoneAggregateAvro.buildAggregateIdentifier() =
    getAggregateIdentifier().buildAggregateIdentifier()

fun MilestoneAggregateAvro.getProjectIdentifier() = getProject().getIdentifier().toUUID()

fun MilestoneAggregateAvro.getCraftIdentifier() = getCraft()?.getIdentifier()?.toUUID()

fun MilestoneAggregateAvro.getWorkAreaIdentifier() = getWorkarea()?.getIdentifier()?.toUUID()

fun MilestoneAggregateAvro.getCreatedBy() =
    getAuditingInformation().getCreatedBy().getIdentifier().toUUID()

fun MilestoneAggregateAvro.getCreatedDate() =
    getAuditingInformation().getCreatedDate().toInstantByMillis()

fun MilestoneAggregateAvro.getLastModifiedBy() =
    getAuditingInformation().getLastModifiedBy().getIdentifier().toUUID()

fun MilestoneAggregateAvro.getLastModifiedDate() =
    getAuditingInformation().getLastModifiedDate().toInstantByMillis()

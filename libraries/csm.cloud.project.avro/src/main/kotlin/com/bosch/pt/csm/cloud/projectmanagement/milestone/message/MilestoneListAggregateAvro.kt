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
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListAggregateAvro

fun MilestoneListAggregateAvro.getIdentifier() = getAggregateIdentifier().getIdentifier().toUUID()

fun MilestoneListAggregateAvro.getVersion() = getAggregateIdentifier().getVersion()

fun MilestoneListAggregateAvro.buildAggregateIdentifier() =
    getAggregateIdentifier().buildAggregateIdentifier()

fun MilestoneListAggregateAvro.getProjectIdentifier() = getProject().getIdentifier().toUUID()

fun MilestoneListAggregateAvro.getWorkAreaIdentifier() = getWorkarea()?.getIdentifier()?.toUUID()

fun MilestoneListAggregateAvro.getCreatedBy() =
    getAuditingInformation().getCreatedBy().getIdentifier().toUUID()

fun MilestoneListAggregateAvro.getCreatedDate() =
    getAuditingInformation().getCreatedDate().toInstantByMillis()

fun MilestoneListAggregateAvro.getLastModifiedBy() =
    getAuditingInformation().getLastModifiedBy().getIdentifier().toUUID()

fun MilestoneListAggregateAvro.getLastModifiedDate() =
    getAuditingInformation().getLastModifiedDate().toInstantByMillis()

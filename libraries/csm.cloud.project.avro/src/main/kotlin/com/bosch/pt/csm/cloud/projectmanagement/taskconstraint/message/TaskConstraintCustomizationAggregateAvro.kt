/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.message

import com.bosch.pt.csm.cloud.common.extensions.toInstantByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationAggregateAvro

fun TaskConstraintCustomizationAggregateAvro.getIdentifier() =
    getAggregateIdentifier().getIdentifier().toUUID()

fun TaskConstraintCustomizationAggregateAvro.getVersion() = getAggregateIdentifier().getVersion()

fun TaskConstraintCustomizationAggregateAvro.buildAggregateIdentifier() =
    getAggregateIdentifier().buildAggregateIdentifier()

fun TaskConstraintCustomizationAggregateAvro.getProjectIdentifier() =
    getProject().getIdentifier().toUUID()

fun TaskConstraintCustomizationAggregateAvro.getCreatedBy() =
    getAuditingInformation().getCreatedBy().getIdentifier().toUUID()

fun TaskConstraintCustomizationAggregateAvro.getCreatedDate() =
    getAuditingInformation().getCreatedDate().toInstantByMillis()

fun TaskConstraintCustomizationAggregateAvro.getLastModifiedBy() =
    getAuditingInformation().getLastModifiedBy().getIdentifier().toUUID()

fun TaskConstraintCustomizationAggregateAvro.getLastModifiedDate() =
    getAuditingInformation().getLastModifiedDate().toInstantByMillis()

/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.listener.message

import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventAvro as TaskConstraintSelectionEventAvro

fun TaskConstraintSelectionEventAvro.getLastModifiedDate() = getAggregate().getLastModifiedDate()

fun TaskConstraintSelectionEventAvro.getLastModifiedByUserIdentifier() =
    getAggregate().getLastModifiedByUserIdentifier()

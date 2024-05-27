/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.message

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectAggregateAvro

fun ProjectAggregateAvro.getIdentifier() = getAggregateIdentifier().getIdentifier().toUUID()

fun ProjectAggregateAvro.buildAggregateIdentifier() =
    getAggregateIdentifier().buildAggregateIdentifier()

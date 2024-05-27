/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.event.listener

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.MessageKeyAvro

fun AggregateIdentifierAvro.buildMessageKeyAvro(aggregateIdentifier: AggregateIdentifierAvro) =
    MessageKeyAvro(getIdentifier(), aggregateIdentifier)

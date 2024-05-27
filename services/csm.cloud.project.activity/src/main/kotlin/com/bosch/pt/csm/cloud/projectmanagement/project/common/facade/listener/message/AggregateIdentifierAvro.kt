/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.message

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier

fun AggregateIdentifierAvro.toAggregateIdentifier() =
    AggregateIdentifier(this.getType(), this.getIdentifier().toUUID(), this.getVersion())

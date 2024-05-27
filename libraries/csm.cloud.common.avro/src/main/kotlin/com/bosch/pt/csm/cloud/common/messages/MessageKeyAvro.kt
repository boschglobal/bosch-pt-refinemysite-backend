/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.messages

import com.bosch.pt.csm.cloud.common.extensions.toUUID

fun MessageKeyAvro.getContextIdentifier() = getRootContextIdentifier().toUUID()

fun MessageKeyAvro.getIdentifier() = getAggregateIdentifier().getIdentifier().toUUID()

fun MessageKeyAvro.getType(): String = getAggregateIdentifier().getType()

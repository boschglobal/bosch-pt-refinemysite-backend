/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.workarea.message

import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventAvro

fun WorkAreaEventAvro.getIdentifier() = getAggregate().getIdentifier()

/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.job.facade.listener

import com.bosch.pt.csm.cloud.job.job.api.JsonSerializedObject
import com.bosch.pt.csm.cloud.job.messages.JsonSerializedObjectAvro

fun JsonSerializedObjectAvro.toJsonSerializedObject() =
    JsonSerializedObject(this.getType(), this.getJson())

/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.job.api

data class JobIdentifier(val value: String)

data class UserIdentifier(val value: String)

data class JsonSerializedObject(val type: String, val json: String)

/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.common.util

import java.util.UUID

fun randomUUID(): UUID = UUID.randomUUID()

fun String.toUUID(): UUID = UUID.fromString(this)

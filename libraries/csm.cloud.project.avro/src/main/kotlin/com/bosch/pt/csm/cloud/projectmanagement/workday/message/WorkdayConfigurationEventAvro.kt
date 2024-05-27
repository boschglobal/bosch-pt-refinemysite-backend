/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.workday.message

import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventAvro

fun WorkdayConfigurationEventAvro.getIdentifier() = aggregate.getIdentifier()

fun WorkdayConfigurationEventAvro.getCreatedDate() = aggregate.getCreatedDate()

fun WorkdayConfigurationEventAvro.getCreatedByUserIdentifier() =
    aggregate.getCreatedByUserIdentifier()

fun WorkdayConfigurationEventAvro.getLastModifiedDate() = aggregate.getLastModifiedDate()

fun WorkdayConfigurationEventAvro.getLastModifiedByUserIdentifier() =
    aggregate.getLastModifiedByUserIdentifier()

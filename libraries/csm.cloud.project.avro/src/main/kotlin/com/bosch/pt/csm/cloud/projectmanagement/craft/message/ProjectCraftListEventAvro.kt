/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.craft.message

import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventAvro

fun ProjectCraftListEventAvro.getIdentifier() = aggregate.getIdentifier()

fun ProjectCraftListEventAvro.getCreatedDate() = aggregate.getCreatedDate()

fun ProjectCraftListEventAvro.getCreatedByUserIdentifier() = aggregate.getCreatedByUserIdentifier()

fun ProjectCraftListEventAvro.getLastModifiedDate() = aggregate.getLastModifiedDate()

fun ProjectCraftListEventAvro.getLastModifiedByUserIdentifier() =
    aggregate.getLastModifiedByUserIdentifier()

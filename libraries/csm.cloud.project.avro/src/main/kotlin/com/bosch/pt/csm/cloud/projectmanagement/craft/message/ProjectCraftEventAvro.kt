/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.craft.message

import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventG2Avro

fun ProjectCraftEventG2Avro.getIdentifier() = aggregate.getIdentifier()

fun ProjectCraftEventG2Avro.getCreatedDate() = aggregate.getCreatedDate()

fun ProjectCraftEventG2Avro.getCreatedByUserIdentifier() = aggregate.getCreatedByUserIdentifier()

fun ProjectCraftEventG2Avro.getLastModifiedDate() = aggregate.getLastModifiedDate()

fun ProjectCraftEventG2Avro.getLastModifiedByUserIdentifier() =
    aggregate.getLastModifiedByUserIdentifier()

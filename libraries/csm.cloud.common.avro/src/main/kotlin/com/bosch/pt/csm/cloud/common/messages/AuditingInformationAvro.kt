/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.messages

import com.bosch.pt.csm.cloud.common.extensions.toUUID

fun AuditingInformationAvro.getCreatedByIdentifier() = getCreatedBy().getIdentifier().toUUID()

fun AuditingInformationAvro.getLastModifiedByIdentifier() =
    getLastModifiedBy().getIdentifier().toUUID()

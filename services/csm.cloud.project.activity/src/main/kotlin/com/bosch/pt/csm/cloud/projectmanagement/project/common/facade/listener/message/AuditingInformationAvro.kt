/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.message

import com.bosch.pt.csm.cloud.common.messages.AuditingInformationAvro
import com.bosch.pt.csm.cloud.common.messages.getCreatedByIdentifier
import com.bosch.pt.csm.cloud.common.messages.getLastModifiedByIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.AuditingInformation
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.UnresolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType.USER
import java.util.Date
import java.util.UUID

fun AuditingInformationAvro.buildAuditingInformation(projectIdentifier: UUID) =
    AuditingInformation(
        UnresolvedObjectReference(USER.type, getCreatedByIdentifier(), projectIdentifier),
        Date(this.getCreatedDate()),
        UnresolvedObjectReference(USER.type, getLastModifiedByIdentifier(), projectIdentifier),
        Date(this.getLastModifiedDate()))

/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReferenceWithPicture
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.response.AbstractAuditableResource
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum
import com.bosch.pt.iot.smartsite.user.facade.rest.datastructure.PhoneNumberDto
import com.bosch.pt.iot.smartsite.user.model.GenderEnum
import java.util.Date
import java.util.UUID

class ParticipantResource(
    override val identifier: UUID,
    override val version: Long,
    override val createdDate: Date,
    override val createdBy: ResourceReference,
    override val lastModifiedDate: Date,
    override val lastModifiedBy: ResourceReference,
    val project: ResourceReference,
    val projectRole: ParticipantRoleEnum?,
    val company: ResourceReference?,
    val user: ResourceReferenceWithPicture? = null,
    var gender: GenderEnum? = null,
    var phoneNumbers: List<PhoneNumberDto>? = null,
    val email: String?,
    val crafts: List<ResourceReference>? = null,
    val status: ParticipantStatusEnum?
) :
    AbstractAuditableResource(
        identifier, version, createdDate, createdBy, lastModifiedDate, lastModifiedBy) {

  companion object {
    const val LINK_DELETE = "delete"
    const val LINK_UPDATE = "update"
    const val LINK_RESEND = "resend"
  }
}

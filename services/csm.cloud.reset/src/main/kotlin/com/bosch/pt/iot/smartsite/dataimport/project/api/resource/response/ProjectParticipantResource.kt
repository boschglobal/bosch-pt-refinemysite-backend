/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.project.api.resource.response

import com.bosch.pt.iot.smartsite.dataimport.common.api.resource.AuditableResource
import com.bosch.pt.iot.smartsite.dataimport.common.api.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.dataimport.common.api.resource.response.ResourceReferenceWithPicture
import com.bosch.pt.iot.smartsite.dataimport.project.model.ProjectRoleEnum
import com.bosch.pt.iot.smartsite.dataimport.user.api.resource.GenderEnum
import com.bosch.pt.iot.smartsite.dataimport.user.model.PhoneNumber
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.UUID

@JsonIgnoreProperties("_links")
class ProjectParticipantResource(
    val id: UUID,
    val crafts: List<ResourceReference>? = null,
    val project: ResourceReference? = null,
    val projectRole: ProjectRoleEnum? = null,
    val company: ResourceReference? = null,
    val user: ResourceReferenceWithPicture? = null,
    val phoneNumbers: Set<PhoneNumber>? = null,
    val email: String? = null,
    val gender: GenderEnum? = null,
    val status: ParticipantStatusEnum? = null
) : AuditableResource()

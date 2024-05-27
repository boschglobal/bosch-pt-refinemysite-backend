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
import com.bosch.pt.iot.smartsite.dataimport.project.model.MilestoneTypeEnum
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import java.util.UUID

@JsonIgnoreProperties("_links")
class MilestoneResource(
    var id: UUID,
    var name: String? = null,
    var type: MilestoneTypeEnum? = null,
    var date: LocalDate? = null,
    var header: Boolean = false,
    var project: ResourceReference? = null,
    var description: String? = null,
    var craft: ProjectCraftReference? = null,
    var workArea: ResourceReference? = null,
    var creator: ResourceReferenceWithPicture? = null,
    var position: Int? = null
) : AuditableResource()

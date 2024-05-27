/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.task.api.resource.response

import com.bosch.pt.iot.smartsite.dataimport.common.api.resource.AuditableResource
import com.bosch.pt.iot.smartsite.dataimport.common.api.resource.response.ResourceReference
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal
import java.util.UUID

@JsonIgnoreProperties("_embedded", "_links")
class DayCardResource(
    var id: UUID? = null,
    var title: String? = null,
    var manpower: BigDecimal? = null,
    var notes: String? = null,
    var task: ResourceReference? = null,
    var status: String? = null,
    var reason: ReasonDayCardResource? = null
) : AuditableResource()

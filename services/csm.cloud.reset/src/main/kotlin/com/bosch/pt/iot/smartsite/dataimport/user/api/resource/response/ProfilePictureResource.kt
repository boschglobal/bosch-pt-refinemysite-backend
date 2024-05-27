/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.user.api.resource.response

import com.bosch.pt.iot.smartsite.dataimport.common.api.resource.AuditableResource
import com.bosch.pt.iot.smartsite.dataimport.common.api.resource.response.ResourceReference
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties("_links")
class ProfilePictureResource(
    val width: Long? = null,
    val height: Long? = null,
    val fileSize: Int? = null,
    val userReference: ResourceReference? = null
) : AuditableResource()

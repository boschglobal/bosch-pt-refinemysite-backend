/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.common.api.resource

import com.bosch.pt.iot.smartsite.dataimport.common.api.resource.response.ResourceReference
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.util.Date

open class AuditableResource(
    val createdBy: ResourceReference? = null,
    val lastModifiedBy: ResourceReference? = null,
    val createdDate: Date? = null,
    val lastModifiedDate: Date? = null,
    val version: Long? = null,
    @JsonProperty("_links")
    @JsonDeserialize(keyAs = String::class, contentAs = MutableMap::class)
    val links: Map<String, Map<String, String>>? = null
)

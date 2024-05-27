/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.project.api.resource.response

import com.bosch.pt.iot.smartsite.dataimport.common.api.resource.response.ResourceReference
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties("_links")
class ProjectCraftReference(val color: String? = null) : ResourceReference()

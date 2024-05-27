/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.user.api.resource.response

import com.bosch.pt.iot.smartsite.dataimport.common.api.resource.response.AbstractListResource
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties("_links")
class CraftListResource(val crafts: Collection<CraftResource> = emptyList()) :
    AbstractListResource()

/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.employee.api.resource.response

import com.bosch.pt.iot.smartsite.dataimport.common.api.resource.response.AbstractListResource
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties("_links")
class EmployeeListResource(val items: Collection<EmployeeResource> = emptyList()) :
    AbstractListResource()

/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.common.api.resource.response

import java.net.URI

data class ResourceReferenceWithPicture(val picture: URI) : ResourceReference()

/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.util

import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum

data class TypedId(val type: ResourceTypeEnum?, val id: String?) {

  companion object {
    fun typedId(type: ResourceTypeEnum?, id: String?): TypedId = TypedId(type, id)
  }
}

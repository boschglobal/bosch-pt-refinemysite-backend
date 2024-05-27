/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.facade.rest.resource.request

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

open class UpdateBatchRequestResource<T : IdentifiableResource>
@JvmOverloads
constructor(@field:NotEmpty @field:Size(max = 100) open val items: Collection<T> = emptyList()) {
  @JsonIgnore fun getIdentifiers() = items.map { requireNotNull(it.id) }.toSet()
}

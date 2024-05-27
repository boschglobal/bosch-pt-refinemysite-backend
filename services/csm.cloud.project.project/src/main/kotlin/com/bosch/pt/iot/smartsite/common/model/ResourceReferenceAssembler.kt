/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.model

import com.bosch.pt.csm.cloud.common.LibraryCandidate
import com.bosch.pt.csm.cloud.common.Referable
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import java.util.UUID
import java.util.function.Supplier

@LibraryCandidate("Same implementation in company service", "csm.cloud.common.web")
object ResourceReferenceAssembler {

  @JvmStatic
  fun referTo(
      identifier: UUID,
      displayName: String?,
      deletedReference: Supplier<ResourceReference>,
      isDeleted: Boolean
  ): ResourceReference =
      if (isDeleted) deletedReference.get()
      else
          ResourceReference.from(
              object : Referable {
                override fun getDisplayName(): String? = displayName
                override fun getIdentifierUuid(): UUID = identifier
              })

  @JvmStatic
  fun referTo(
      referable: Referable,
      deletedReference: Supplier<ResourceReference>,
      isDeleted: Boolean
  ): ResourceReference =
      if (isDeleted) deletedReference.get() else ResourceReference.from(referable)
}

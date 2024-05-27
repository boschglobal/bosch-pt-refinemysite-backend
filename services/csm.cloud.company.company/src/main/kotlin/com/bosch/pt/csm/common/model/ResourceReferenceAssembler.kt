/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.common.model

import com.bosch.pt.csm.cloud.common.LibraryCandidate
import com.bosch.pt.csm.cloud.common.Referable
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import java.util.function.Supplier

@LibraryCandidate(
    "Same implementation in project service - better null handling here", "csm.cloud.common.web")
object ResourceReferenceAssembler {

  @JvmStatic
  fun referTo(
      referable: Referable?,
      deletedReference: Supplier<ResourceReference>
  ): ResourceReference =
      if (null == referable) {
        deletedReference.get()
      } else {
        ResourceReference.from(referable)
      }
}

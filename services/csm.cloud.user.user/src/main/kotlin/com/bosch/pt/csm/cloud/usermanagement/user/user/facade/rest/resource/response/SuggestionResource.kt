/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.response

import jakarta.validation.constraints.NotNull

class SuggestionResource() {
  var term: @NotNull String? = null

  constructor(term: String?) : this() {
    this.term = term
  }
}

/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.common.facade.rest.resource.request

import jakarta.validation.constraints.Size

class SuggestionResource(@field:Size(max = 200) val term: String?)

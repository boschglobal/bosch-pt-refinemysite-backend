/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.company.facade.rest.resource.request

import com.bosch.pt.csm.company.company.shared.model.Company.Companion.MAX_NAME_LENGTH
import jakarta.validation.constraints.Size

class FilterCompanyListResource(@field:Size(max = MAX_NAME_LENGTH) val name: String?)

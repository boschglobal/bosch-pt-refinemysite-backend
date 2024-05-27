/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.employee.facade.rest.resource.request

class SearchEmployeesFilterResource(
    /** if set, first name or last name has to start with this term */
    var name: String? = null,

    /** if set, the email address has to start with this term */
    var email: String? = null,

    /** if set, the name of the company the user is assigned to needs to start with this term */
    var companyName: String? = null
)

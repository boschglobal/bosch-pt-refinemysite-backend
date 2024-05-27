/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.repository

import java.util.UUID

interface CompanyRepositoryExtension {
    fun deleteCompany(companyIdentifier: UUID)
}

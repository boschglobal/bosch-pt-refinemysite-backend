/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.repository.impl

import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections
import com.bosch.pt.csm.cloud.projectmanagement.company.repository.CompanyRepositoryExtension
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query.query
import java.util.UUID

open class CompanyRepositoryExtensionImpl(
    private val mongoOperations: MongoOperations
) : CompanyRepositoryExtension {

    override fun deleteCompany(companyIdentifier: UUID) {
        mongoOperations.remove(
            query(isAnyVersionOfCompany(companyIdentifier)),
            Collections.COMPANY_STATE
        )
    }

    private fun isAnyVersionOfCompany(companyIdentifier: UUID): Criteria =
        Criteria.where("companyIdentifier").`is`(companyIdentifier)
}
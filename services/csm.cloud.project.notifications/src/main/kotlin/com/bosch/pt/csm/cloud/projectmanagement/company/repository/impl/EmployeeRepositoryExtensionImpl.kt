/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.repository.impl

import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CommonAttributeNames.ID_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.company.repository.EmployeeRepositoryExtension
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query.query
import java.util.UUID

open class EmployeeRepositoryExtensionImpl(
    private val mongoOperations: MongoOperations
) : EmployeeRepositoryExtension {

    override fun deleteEmployee(companyIdentifier: UUID, identifier: UUID) {
        mongoOperations.remove(
            query(isAnyVersionOfEmployee(companyIdentifier, identifier)),
            Collections.COMPANY_STATE
        )
    }

    private fun isAnyVersionOfEmployee(companyIdentifier: UUID, identifier: UUID): Criteria =
        Criteria.where(ID_IDENTIFIER).`is`(identifier).and("companyIdentifier").`is`(companyIdentifier)
}
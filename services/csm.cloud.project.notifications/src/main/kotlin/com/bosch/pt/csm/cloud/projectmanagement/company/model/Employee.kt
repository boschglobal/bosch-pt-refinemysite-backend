/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.model

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections.COMPANY_STATE
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document
import java.util.UUID

@Document(collection = COMPANY_STATE)
@TypeAlias("Employee")
data class Employee(
    @Id val identifier: AggregateIdentifier,
    override val companyIdentifier: UUID,
    val userIdentifier: UUID,
    val deleted: Boolean = false
) : ShardedByCompanyIdentifier

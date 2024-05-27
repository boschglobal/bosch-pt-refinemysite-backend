/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.model

import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Sharded
import java.util.UUID

interface ShardedByCompanyIdentifier : Sharded {
    val companyIdentifier: UUID

    override val shardKeyName: String
        get() = "companyIdentifier"

    override val shardKeyValue: UUID
        get() = companyIdentifier
}

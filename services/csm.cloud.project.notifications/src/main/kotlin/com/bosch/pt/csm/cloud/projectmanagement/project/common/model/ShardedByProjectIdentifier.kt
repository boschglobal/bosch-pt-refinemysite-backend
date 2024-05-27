/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.common.model

import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Sharded
import java.util.UUID

interface ShardedByProjectIdentifier : Sharded {
    val projectIdentifier: UUID

    override val shardKeyName: String
        get() = "projectIdentifier"

    override val shardKeyValue: UUID
        get() = projectIdentifier
}

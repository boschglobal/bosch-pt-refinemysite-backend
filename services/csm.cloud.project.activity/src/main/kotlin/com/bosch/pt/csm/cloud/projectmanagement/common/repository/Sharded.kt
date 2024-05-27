/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.common.repository

import java.io.Serializable
import java.util.UUID

interface Sharded : Serializable {
  val shardKeyName: String
  val shardKeyValue: UUID
}

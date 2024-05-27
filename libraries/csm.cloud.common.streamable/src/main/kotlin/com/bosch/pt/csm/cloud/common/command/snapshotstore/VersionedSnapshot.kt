/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UuidIdentifiable

interface VersionedSnapshot {
  val identifier: UuidIdentifiable
  val version: Long

  companion object {
    const val INITIAL_SNAPSHOT_VERSION = -1L
  }
}

/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.common.model

import java.util.UUID

interface VersionedEntity {
  val identifier: UUID?
  val version: Long?
}

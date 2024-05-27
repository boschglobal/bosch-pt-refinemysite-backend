/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.api

import java.util.UUID

interface UuidIdentifiable {
  val identifier: UUID
  fun toUuid(): UUID = identifier
}

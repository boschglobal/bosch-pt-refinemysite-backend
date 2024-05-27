/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.common.repository

import com.bosch.pt.iot.smartsite.common.model.VersionedEntity
import java.util.Optional
import java.util.UUID

interface FindOneByIdentifierRepository {

  fun findOne(identifier: UUID): Optional<VersionedEntity>
}

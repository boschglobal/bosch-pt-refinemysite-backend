/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.statistics.repository

import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.NamedObject
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.ObjectIdentifier
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface NamedObjectRepository : JpaRepository<NamedObject, Long> {

  fun findOneByObjectIdentifier(objectIdentifier: ObjectIdentifier): NamedObject?

  fun findAllByObjectIdentifierIn(
      objectIdentifiers: Iterable<ObjectIdentifier>
  ): Collection<NamedObject>

  fun deleteAllByObjectIdentifierIn(identifiers: Collection<ObjectIdentifier>)
}

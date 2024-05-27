/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.statistics.repository

import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.ObjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.ObjectRelation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ObjectRelationRepository :
    JpaRepository<ObjectRelation, Long>, ObjectRelationRepositoryExtension {

  fun findOneByLeftAndRightType(objectIdentifier: ObjectIdentifier, type: String): ObjectRelation?

  fun findAllByLeftInAndRightType(
      objectIdentifiers: Collection<ObjectIdentifier>,
      type: String
  ): List<ObjectRelation>

  fun findAllByLeftTypeAndRight(
      type: String,
      objectIdentifier: ObjectIdentifier
  ): List<ObjectRelation>
}

/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.common.repository

import com.bosch.pt.csm.cloud.projectmanagement.news.model.ObjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.news.model.ObjectRelation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ObjectRelationRepository :
    JpaRepository<ObjectRelation, Long>, ObjectRelationRepositoryExtension {

  fun findOneByLeftAndRightType(objectIdentifier: ObjectIdentifier, type: String): ObjectRelation?

  fun findAllByLeftTypeAndRight(
      type: String,
      objectIdentifier: ObjectIdentifier
  ): List<ObjectRelation>

  fun findAllByLeftTypeAndRightIn(
      type: String,
      objectIdentifiers: List<ObjectIdentifier>
  ): List<ObjectRelation>

  fun findAllByLeftInAndRightType(
      objectIdentifiers: List<ObjectIdentifier>,
      type: String
  ): List<ObjectRelation>
}

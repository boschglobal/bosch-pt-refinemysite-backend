/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.user.repository.impl

import com.bosch.pt.iot.smartsite.craft.model.Craft
import com.bosch.pt.iot.smartsite.user.model.PhoneNumber
import com.bosch.pt.iot.smartsite.user.model.User
import com.bosch.pt.iot.smartsite.user.model.User_
import com.bosch.pt.iot.smartsite.user.repository.UserRepositoryExtension
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.JoinType
import java.util.UUID

class UserRepositoryImpl(@PersistenceContext val entityManager: EntityManager) :
    UserRepositoryExtension {

  override fun findWithDetailsByIdentifier(identifier: UUID): User? {
    val cb = entityManager.criteriaBuilder
    val cq = cb.createQuery(User::class.java)

    val root = cq.from(User::class.java)
    root.fetch<User, Craft>(User_.CRAFTS, JoinType.LEFT)
    root.fetch<User, PhoneNumber>(User_.PHONENUMBERS, JoinType.LEFT)
    root.fetch<User, User>(User_.CREATED_BY, JoinType.LEFT)
    root.fetch<User, User>(User_.LAST_MODIFIED_BY, JoinType.LEFT)

    cq.where(cb.equal(root.get(User_.identifier), identifier))

    return entityManager.createQuery(cq).resultList.firstOrNull()
  }
}

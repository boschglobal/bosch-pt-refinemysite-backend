/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.model

import java.io.Serializable
import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PostLoad
import jakarta.persistence.PostPersist
import org.springframework.data.domain.Persistable

@MappedSuperclass
abstract class AbstractPersistableEntity<PK : Serializable>(givenId: PK) : Persistable<PK> {

  @EmbeddedId
  @Column(unique = true, nullable = false, name = "identifier")
  var id: PK = givenId
    protected set

  @Transient private var persisted: Boolean = false

  override fun isNew(): Boolean = !persisted

  override fun hashCode(): Int = id.hashCode()

  override fun equals(other: Any?): Boolean =
      when {
        this === other -> true
        other == null -> false
        other !is AbstractPersistableEntity<*> -> false
        else -> id == other.id
      }

  @PostPersist
  @PostLoad
  @Suppress("UnusedPrivateMember", "kotlin:S1144")
  private fun setPersisted() {
    persisted = true
  }
}

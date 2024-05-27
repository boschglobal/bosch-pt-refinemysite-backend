/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.api

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.IDENTITY
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Transient
import java.io.Serializable
import org.springframework.data.domain.Persistable
import org.springframework.data.util.ProxyUtils

/**
 * Copy of [org.springframework.data.jpa.domain.AbstractPersistable]. Converted to Kotlin and
 * specified explicit strategy for [GeneratedValue]
 */
@MappedSuperclass
abstract class AbstractPersistable<PK : Serializable> : Persistable<PK> {

  @Id @GeneratedValue(strategy = IDENTITY) private var id: PK? = null

  override fun getId(): PK? = id

  // Overriding setId is a hack we currently use in the project service to
  // replace replicated entities.
  protected open fun setId(id: PK?) {
    this.id = id
  }

  @Transient override fun isNew(): Boolean = null == getId()

  @Suppress("ImplicitDefaultLocale")
  override fun toString() =
      String.format("Entity of type %s with id: %s", this.javaClass.name, getId())

  override fun equals(other: Any?): Boolean {
    return if (null == other) {
      false
    } else if (this === other) {
      true
    } else if (this.javaClass != ProxyUtils.getUserClass(other)) {
      false
    } else {
      val that = other as AbstractPersistable<*>
      if (null == getId()) false else getId() == that.getId()
    }
  }

  override fun hashCode(): Int {
    var hashCode = 17
    hashCode += if (null == getId()) 0 else getId().hashCode() * 31
    return hashCode
  }
}

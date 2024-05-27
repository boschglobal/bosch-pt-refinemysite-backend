/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.command.snapshotstore

import com.bosch.pt.csm.cloud.common.Referable
import com.bosch.pt.csm.cloud.common.api.AbstractPersistable
import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.UuidIdentifiable
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType.TIMESTAMP
import jakarta.persistence.Version
import java.io.Serializable
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Objects.hash
import java.util.Optional
import java.util.UUID
import org.apache.commons.lang3.builder.ToStringBuilder
import org.springframework.data.domain.Auditable

/**
 * Abstract base class for JPA entities that represent an aggregate snapshot
 *
 * @param IID The type of the internal ID used in the database (Usually this is Long)
 * @param EID the type of the external ID used to reference the aggregate. This is a wrapped UUID to
 *   make it type-safe.
 */
@MappedSuperclass
@AttributeOverrides(
    AttributeOverride(name = "createdDate", column = Column(nullable = false)),
    AttributeOverride(name = "lastModifiedDate", column = Column(nullable = false)))
@Suppress("SerialVersionUIDInSerializableClass")
abstract class AbstractSnapshotEntity<IID : Serializable, EID : UuidIdentifiable> :
    AbstractPersistable<IID>(), Auditable<UserId, IID, LocalDateTime>, Referable, Serializable {

  @Embedded
  @AttributeOverride(name = "identifier", column = Column(nullable = false, length = 36))
  lateinit var identifier: EID

  @Version @Column(nullable = false) var version: Long = -1L

  @Embedded
  @AttributeOverride(
      name = "identifier", column = Column(name = "createdBy", nullable = false, length = 36))
  private lateinit var createdBy: UserId

  @Temporal(TIMESTAMP) private lateinit var createdDate: Date

  @Embedded
  @AttributeOverride(
      name = "identifier", column = Column(name = "lastModifiedBy", nullable = false, length = 36))
  private lateinit var lastModifiedBy: UserId

  @Temporal(TIMESTAMP) private lateinit var lastModifiedDate: Date

  override fun getCreatedBy(): Optional<UserId> = Optional.ofNullable(createdBy)

  override fun setCreatedBy(createdBy: UserId) {
    this.createdBy = createdBy
  }

  override fun getCreatedDate(): Optional<LocalDateTime> =
      Optional.of(LocalDateTime.ofInstant(createdDate.toInstant(), ZoneId.systemDefault()))

  override fun setCreatedDate(createdDate: LocalDateTime) {
    this.createdDate = Date.from(createdDate.atZone(ZoneId.systemDefault()).toInstant())
  }

  override fun getLastModifiedBy(): Optional<UserId> = Optional.ofNullable(lastModifiedBy)

  override fun setLastModifiedBy(lastModifiedBy: UserId) {
    this.lastModifiedBy = lastModifiedBy
  }

  override fun getLastModifiedDate(): Optional<LocalDateTime> =
      Optional.of(LocalDateTime.ofInstant(lastModifiedDate.toInstant(), ZoneId.systemDefault()))

  override fun setLastModifiedDate(lastModifiedDate: LocalDateTime) {
    this.lastModifiedDate = Date.from(lastModifiedDate.atZone(ZoneId.systemDefault()).toInstant())
  }

  override fun getIdentifierUuid(): UUID = this.identifier.toUuid()

  override fun toString(): String =
      ToStringBuilder(this)
          .append("id", id)
          .append("identifier", getIdentifierUuid())
          .append("version", version)
          .toString()

  /**
   * @see [Explanation]
   *   (https://thorben-janssen.com/ultimate-guide-to-implementing-equals-and-hashcode-with-hibernate/)
   *   for further information how to implement equals and hashcode to work properly with hibernate
   */
  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    // do not compare via Class#equals because o is typically a Hibernate proxy, using a class
    // generated a runtime.
    if (other !is AbstractSnapshotEntity<*, *>) {
      return false
    }
    return identifier == other.identifier
  }

  override fun hashCode(): Int = hash(super.hashCode(), identifier)
}

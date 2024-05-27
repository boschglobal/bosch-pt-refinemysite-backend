/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.common.model

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverageGenerated
import com.bosch.pt.csm.cloud.common.Referable
import com.bosch.pt.csm.cloud.common.api.AbstractPersistable
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.iot.smartsite.user.model.User
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.ManyToOne
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType.TIMESTAMP
import jakarta.validation.constraints.NotNull
import java.io.Serializable
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Objects
import java.util.Optional
import java.util.UUID
import org.apache.commons.lang3.builder.ToStringBuilder
import org.springframework.data.domain.Auditable

/**
 * A replicated entity is a derived local view of data owned by another service. Hence, the data is
 * received via kafka topics and stored here. No auditing listener is required for those entities
 * and the auditing columns are also optional to support a soft delete of these entities.
 *
 * @param <T> Type of the primary key </T>
 */
@MappedSuperclass
@AttributeOverrides(
    AttributeOverride(name = "createdDate", column = Column(nullable = false)),
    AttributeOverride(name = "lastModifiedDate", column = Column(nullable = false)))
abstract class AbstractReplicatedEntity<T : Serializable> :
    AbstractPersistable<T>(),
    Auditable<User, T, LocalDateTime>,
    Referable,
    Serializable,
    VersionedEntity {

  /** Identifier of the entity. */
  @field:NotNull @Column(nullable = false) override var identifier: UUID? = null

  @ManyToOne(fetch = LAZY) private var createdBy: User? = null

  @Temporal(TIMESTAMP) private var createdDate: Date? = null

  @ManyToOne(fetch = LAZY) private var lastModifiedBy: User? = null

  @Temporal(TIMESTAMP) private var lastModifiedDate: Date? = null

  /** Version for optimistic locking. */
  @field:NotNull @Column(nullable = false) override var version: Long? = null

  override fun getIdentifierUuid(): UUID = requireNotNull(this.identifier)
  override fun getCreatedBy(): Optional<User> = Optional.ofNullable(createdBy)

  override fun setCreatedBy(createdBy: User?) {
    this.createdBy = createdBy
  }

  @ExcludeFromCodeCoverageGenerated
  override fun getCreatedDate(): Optional<LocalDateTime> =
      if (null == createdDate) Optional.empty()
      else Optional.of(LocalDateTime.ofInstant(createdDate!!.toInstant(), ZoneId.systemDefault()))

  override fun setCreatedDate(createdDate: LocalDateTime?) {
    this.createdDate =
        if (createdDate == null) null
        else Date.from(createdDate.atZone(ZoneId.systemDefault()).toInstant())
  }

  override fun getLastModifiedBy(): Optional<User> = Optional.ofNullable(lastModifiedBy)

  override fun setLastModifiedBy(lastModifiedBy: User?) {
    this.lastModifiedBy = lastModifiedBy
  }

  @ExcludeFromCodeCoverageGenerated
  override fun getLastModifiedDate(): Optional<LocalDateTime> =
      if (null == lastModifiedDate) Optional.empty()
      else
          Optional.of(
              LocalDateTime.ofInstant(lastModifiedDate!!.toInstant(), ZoneId.systemDefault()))

  override fun setLastModifiedDate(lastModifiedDate: LocalDateTime?) {
    this.lastModifiedDate =
        if (lastModifiedDate == null) null
        else Date.from(lastModifiedDate.atZone(ZoneId.systemDefault()).toInstant())
  }

  @ExcludeFromCodeCoverage
  override fun toString(): String =
      ToStringBuilder(this)
          .append("id", id)
          .append("identifier", identifier)
          .append("version", version)
          .toString()

  /**
   * Reusable operation to map information of this class to the corresponding AVRO class.
   *
   * @return [AuditingInformationAvro]
   */
  fun toAggregateIdentifier(): AggregateIdentifierAvro =
      toAggregateIdentifier(this, getAggregateType())

  protected fun toAggregateIdentifier(
      entity: AbstractReplicatedEntity<*>,
      type: String
  ): AggregateIdentifierAvro =
      AggregateIdentifierAvro.newBuilder()
          .setIdentifier(entity.identifier.toString())
          .setVersion(requireNotNull(entity.version))
          .setType(type)
          .build()

  /**
   * This operation needs to be overwritten by the entity class and return the correct aggregate
   * type
   *
   * @return the aggregate type as a string
   */
  protected abstract fun getAggregateType(): String

  @ExcludeFromCodeCoverage
  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    // do not compare via Class#equals because o is typically a Hibernate proxy, using a class
    // generated a runtime.
    if (other !is AbstractReplicatedEntity<*>) {
      return false
    }

    return null != identifier && identifier == other.identifier
  }

  @ExcludeFromCodeCoverage override fun hashCode(): Int = Objects.hash(super.hashCode(), identifier)

  companion object {
    private const val serialVersionUID: Long = -2041686578917972885
  }
}

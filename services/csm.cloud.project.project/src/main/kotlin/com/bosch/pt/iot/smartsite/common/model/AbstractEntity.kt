/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.common.model

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.Referable
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.AuditingInformationAvro
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.iot.smartsite.application.security.AuthorizationUtils.getCurrentUser
import com.bosch.pt.iot.smartsite.user.model.User
import com.google.common.collect.Sets
import java.io.Serializable
import java.time.Instant.now
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset.UTC
import java.util.Date
import java.util.Objects
import java.util.Optional
import java.util.UUID
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.ManyToOne
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType.TIMESTAMP
import jakarta.persistence.Version
import jakarta.validation.constraints.NotNull
import org.apache.commons.lang3.builder.ToStringBuilder
import org.springframework.data.domain.Auditable
import org.springframework.data.jpa.domain.support.AuditingEntityListener

/**
 * Abstract base class for all entities.
 *
 * Adds optimistic locking and partly auditing support. Note: Due to existing hibernate bug
 * HHH-10387 (https://hibernate.atlassian.net/browse/HHH-10387) user associations cannot be declared
 * in this superclass!
 *
 * @param <T> type of primary key extending [Serializable].
 * @param <U> concrete type of this entity </U></T>
 */
@MappedSuperclass
@AttributeOverrides(
    AttributeOverride(name = "createdDate", column = Column(nullable = false)),
    AttributeOverride(name = "lastModifiedDate", column = Column(nullable = false)))
@EntityListeners(AuditingEntityListener::class, DispatchingEntityListener::class)
abstract class AbstractEntity<T : Serializable, U : AbstractEntity<T, U>> :
    AbstractCallbackAwarePersistable<T, U>(),
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
  @field:NotNull @Version @Column(nullable = false) override var version: Long? = null

  override fun getIdentifierUuid(): UUID = requireNotNull(identifier)

  override fun getCreatedBy(): Optional<User> = Optional.ofNullable(createdBy)

  override fun setCreatedBy(createdBy: User?) {
    this.createdBy = createdBy
  }

  override fun getCreatedDate(): Optional<LocalDateTime> =
      if (null == createdDate) Optional.empty()
      else Optional.of(LocalDateTime.ofInstant(createdDate!!.toInstant(), ZoneId.systemDefault()))

  override fun setCreatedDate(createdDate: LocalDateTime) {
    this.createdDate = Date.from(createdDate.atZone(ZoneId.systemDefault()).toInstant())
  }

  override fun getLastModifiedBy(): Optional<User> = Optional.ofNullable(lastModifiedBy)

  override fun setLastModifiedBy(lastModifiedBy: User?) {
    this.lastModifiedBy = lastModifiedBy
  }

  override fun getLastModifiedDate(): Optional<LocalDateTime> =
      if (null == lastModifiedDate) Optional.empty()
      else
          Optional.of(
              LocalDateTime.ofInstant(lastModifiedDate!!.toInstant(), ZoneId.systemDefault()))

  override fun setLastModifiedDate(lastModifiedDate: LocalDateTime) {
    this.lastModifiedDate = Date.from(lastModifiedDate.atZone(ZoneId.systemDefault()).toInstant())
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
   * @param deleted flag to indicate that entity was deleted and last modified information must be
   * overwritten
   * @return [AuditingInformationAvro]
   */
  protected fun toAuditingInformationAvro(deleted: Boolean): AuditingInformationAvro =
      if (getCreatedDate().isPresent) {
        AuditingInformationAvro.newBuilder()
            .setCreatedBy(toAggregateIdentifier(getCreatedBy().get(), USER.value))
            .setCreatedDate(getCreatedDate().get().toInstant(UTC).toEpochMilli())
            .setLastModifiedBy(
                toAggregateIdentifier(
                    if (deleted) getCurrentUser() else getLastModifiedBy().get(), USER.value))
            .setLastModifiedDate(
                if (deleted) now().toEpochMilli()
                else
                    getLastModifiedDate()
                        .map { d: LocalDateTime -> d.toInstant(UTC).toEpochMilli() }
                        .orElse(0L))
            .build()
      } else {
        AuditingInformationAvro.newBuilder()
            .setCreatedBy(toAggregateIdentifier(getCreatedBy().get(), USER.value))
            .setCreatedDate(0L)
            .setLastModifiedBy(toAggregateIdentifier(getLastModifiedBy().get(), USER.value))
            .setLastModifiedDate(
                getLastModifiedDate()
                    .map { d: LocalDateTime -> d.toInstant(UTC).toEpochMilli() }
                    .orElse(0L))
            .build()
      }

  /**
   * Reusable operation to map information of this class to the corresponding AVRO class.
   *
   * @return [AuditingInformationAvro]
   */
  @JvmOverloads
  fun toAggregateIdentifier(deleted: Boolean = false): AggregateIdentifierAvro =
      toAggregateIdentifier(this, getAggregateType(), deleted)

  /**
   * Reusable operation to map information of this class to the corresponding AVRO class.
   *
   * @param deleted flag to indicate that entity was deleted and entity version must be updated
   * @return [AuditingInformationAvro]
   */
  @Deprecated(
      "Replace with the version without type. The type is automatically detected.",
      ReplaceWith(
          "com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.toAggregateIdentifier(deleted)"))
  fun toAggregateIdentifier(type: String, deleted: Boolean): AggregateIdentifierAvro =
      toAggregateIdentifier(this, type, deleted)

  /**
   * Reusable operation to map information of this class to the corresponding AVRO class.
   *
   * @param type the type of the dependent entity
   * @param identifier the identifier of the dependent entity
   * @param version the identifier of the dependent entity
   * @return [AggregateIdentifierAvro]
   */
  protected fun toAggregateIdentifier(
      type: String,
      identifier: UUID,
      version: Long
  ): AggregateIdentifierAvro =
      AggregateIdentifierAvro.newBuilder()
          .setIdentifier(identifier.toString())
          .setVersion(version)
          .setType(type)
          .build()

  /**
   * Reusable operation to map information of a dependent entity to the corresponding AVRO class.
   *
   * @param entity the dependent entity
   * @param type the type of the dependent entity
   * @return [AggregateIdentifierAvro]
   */
  protected fun toAggregateIdentifier(
      entity: AbstractEntity<*, *>,
      type: String,
      deleted: Boolean = false
  ): AggregateIdentifierAvro =
      AggregateIdentifierAvro.newBuilder()
          .setIdentifier(entity.identifier.toString())
          .setVersion(
              if (deleted) requireNotNull(entity.version) + 1 else requireNotNull(entity.version))
          .setType(type)
          .build()

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
  abstract fun getAggregateType(): String

  @ExcludeFromCodeCoverage
  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    // do not compare via Class#equals because o is typically a Hibernate proxy, using a class
    // generated a runtime.
    if (other !is AbstractEntity<*, *>) {
      return false
    }

    return null != identifier && identifier == other.identifier
  }

  @ExcludeFromCodeCoverage override fun hashCode(): Int = Objects.hash(super.hashCode(), identifier)

  companion object {

    private const val serialVersionUID: Long = 2656381710487652395

    /**
     * Updates the given subject to contain the same elements contained in the reference:
     *
     * * Elements not contained in the reference are removed from the subject.
     * * Elements contained only in the reference but not in the subject are added to the subject.
     *
     * When subject and reference already contain the same elements, the subject won't be changed
     * and will not be considered dirty by Hibernate.
     *
     * @param subject the set to be updated
     * @param reference the reference set telling what the subject should look like after the update
     * @param <T> the type of objects held by the sets </T>
     */
    protected fun <T> updateSet(subject: MutableSet<T>, reference: Set<T>) {
      // determine added actions
      val addedActions: MutableSet<T> = Sets.newHashSet(reference)
      addedActions.removeAll(subject)

      // determine removed actions
      val removedActions: MutableSet<T> = Sets.newHashSet(subject)
      removedActions.removeAll(reference)

      // apply added/removed items. We don't use #setActions here because this would dirty
      // the selection even in case the set of selected actions equals the existing set.
      subject.addAll(addedActions)
      subject.removeAll(removedActions)
    }
  }
}

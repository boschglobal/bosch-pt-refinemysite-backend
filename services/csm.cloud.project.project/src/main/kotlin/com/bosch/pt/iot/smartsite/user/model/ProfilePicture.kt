/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.user.model

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureAggregateAvro
import com.bosch.pt.iot.smartsite.common.model.AbstractReplicatedEntity
import java.util.UUID
import jakarta.persistence.AssociationOverride
import jakarta.persistence.AssociationOverrides
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull

@Entity
@AssociationOverrides(
    AssociationOverride(
        name = "createdBy",
        joinColumns = [JoinColumn(nullable = false)],
        foreignKey = ForeignKey(name = "FK_ProfilePicture_CreatedBy")),
    AssociationOverride(
        name = "lastModifiedBy",
        joinColumns = [JoinColumn(nullable = false)],
        foreignKey = ForeignKey(name = "FK_ProfilePicture_LastModifiedBy")))
@Table(indexes = [Index(name = "UK_ProfilePicture_UserId", columnList = "user_id", unique = true)])
class ProfilePicture : AbstractReplicatedEntity<Long> {

  @field:NotNull
  @JoinColumn(foreignKey = ForeignKey(name = "FK_ProfilePicture_User"))
  @OneToOne(fetch = LAZY, optional = false)
  var user: User? = null

  @Column(nullable = true) var smallAvailable = false

  @Column(nullable = true) var fullAvailable = false

  @field:NotNull @Column(nullable = false) var width: Long? = null

  @field:NotNull @Column(nullable = false) var height: Long? = null

  @field:NotNull @Column(nullable = false) var fileSize: Long? = null

  constructor() : super()

  constructor(user: User?, width: Long?, height: Long?, fileSize: Long?) {
    this.user = user
    this.width = width
    this.height = height
    this.fileSize = fileSize
  }

  constructor(
      identifier: UUID?,
      version: Long?,
      user: User?,
      width: Long?,
      height: Long?,
      fileSize: Long?,
      smallAvailable: Boolean,
      fullAvailable: Boolean
  ) {
    this.identifier = identifier
    this.version = version
    this.user = user
    this.width = width
    this.height = height
    this.fileSize = fileSize
    this.smallAvailable = smallAvailable
    this.fullAvailable = fullAvailable
  }

  override fun getDisplayName(): String? = "User Attachment"

  public override fun setId(id: Long?) {
    super.setId(id)
  }

  override fun getAggregateType(): String {
    return UsermanagementAggregateTypeEnum.USERPICTURE.value
  }

  companion object {

    private const val serialVersionUID: Long = 32546774600205962L

    fun fromAvroMessage(
        aggregate: UserPictureAggregateAvro,
        user: User?,
        createdBy: User?,
        lastModifiedBy: User?
    ): ProfilePicture =
        ProfilePicture(
                aggregate.getAggregateIdentifier().getIdentifier().toUUID(),
                aggregate.getAggregateIdentifier().getVersion(),
                user,
                aggregate.getWidth(),
                aggregate.getHeight(),
                aggregate.getFileSize(),
                aggregate.getSmallAvailable(),
                aggregate.getFullAvailable())
            .apply {
              setCreatedBy(createdBy)
              setLastModifiedBy(lastModifiedBy)
              setCreatedDate(
                  aggregate.getAuditingInformation().getCreatedDate().toLocalDateTimeByMillis())
              setLastModifiedDate(
                  aggregate
                      .getAuditingInformation()
                      .getLastModifiedDate()
                      .toLocalDateTimeByMillis())
            }
  }
}

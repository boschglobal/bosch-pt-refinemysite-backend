/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.picture.shared.model

import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata
import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata.OwnerType.USER_PICTURE
import com.bosch.pt.csm.cloud.common.blob.model.BoundedContext
import com.bosch.pt.csm.cloud.common.blob.model.ImageBlobOwner
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.csm.cloud.usermanagement.user.picture.ProfilePictureId
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(indexes = [Index(name = "UK_ProfilePicture_UserId", columnList = "user_id", unique = true)])
class ProfilePicture() : AbstractSnapshotEntity<Long, ProfilePictureId>(), ImageBlobOwner {

  @JoinColumn(foreignKey = ForeignKey(name = "FK_ProfilePicture_User"))
  @OneToOne(fetch = LAZY, optional = false)
  lateinit var user: User

  @Column private var smallAvailable = false

  @Column private var fullAvailable = false

  @Column(nullable = false) var width: Long = -1

  @Column(nullable = false) var height: Long = -1

  @Column(nullable = false) var fileSize: Long = -1

  constructor(
      identifier: ProfilePictureId,
      user: User,
      width: Long,
      height: Long,
      fileSize: Long
  ) : this() {
    this.identifier = identifier
    this.user = user
    this.width = width
    this.height = height
    this.fileSize = fileSize
  }

  override fun getDisplayName() = "User Attachment"

  override fun isSmallAvailable(): Boolean = smallAvailable

  override fun setSmallAvailable(available: Boolean) {
    this.smallAvailable = available
  }

  override fun isFullAvailable(): Boolean = fullAvailable

  override fun setFullAvailable(available: Boolean) {
    this.fullAvailable = available
  }

  override fun getBoundedContext(): BoundedContext = BoundedContext.USER

  override fun getParentIdentifier(): UUID = user.getIdentifierUuid()

  override fun getOwnerType(): BlobMetadata.OwnerType = USER_PICTURE
}

/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.picture.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.UserReference
import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata
import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata.OwnerType.USER_PICTURE
import com.bosch.pt.csm.cloud.common.blob.model.BoundedContext
import com.bosch.pt.csm.cloud.common.blob.model.ImageBlobOwner
import com.bosch.pt.csm.cloud.common.command.handler.CommandHandler
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditableSnapshot
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot
import com.bosch.pt.csm.cloud.usermanagement.user.picture.ProfilePictureId
import com.bosch.pt.csm.cloud.usermanagement.user.picture.command.mapper.ProfilePictureAvroSnapshotEventMapper
import com.bosch.pt.csm.cloud.usermanagement.user.picture.shared.model.ProfilePicture
import java.time.LocalDateTime
import java.util.UUID

data class ProfilePictureSnapshot(
    override var identifier: ProfilePictureId,
    override var version: Long = VersionedSnapshot.INITIAL_SNAPSHOT_VERSION,
    override var createdDate: LocalDateTime?,
    override var createdBy: UserId?,
    override var lastModifiedDate: LocalDateTime?,
    override var lastModifiedBy: UserId?,
    var user: UserReference,
    private var smallAvailable: Boolean,
    private var fullAvailable: Boolean,
    var width: Long?,
    var height: Long?,
    var fileSize: Long?
) : VersionedSnapshot, AuditableSnapshot, ImageBlobOwner {

  constructor(
      profilePicture: ProfilePicture
  ) : this(
      profilePicture.identifier,
      profilePicture.version,
      profilePicture.createdDate.orElse(null),
      profilePicture.createdBy.orElse(null),
      profilePicture.lastModifiedDate.orElse(null),
      profilePicture.lastModifiedBy.orElse(null),
      profilePicture.user.toUserReference(),
      profilePicture.isSmallAvailable(),
      profilePicture.isFullAvailable(),
      profilePicture.width,
      profilePicture.height,
      profilePicture.fileSize)

  override fun getIdentifierUuid(): UUID = identifier.identifier

  override fun getOwnerType(): BlobMetadata.OwnerType = USER_PICTURE

  override fun getBoundedContext(): BoundedContext = BoundedContext.USER

  override fun getParentIdentifier(): UUID = user.identifier.identifier

  override fun isSmallAvailable(): Boolean = smallAvailable

  override fun setSmallAvailable(available: Boolean) {
    this.smallAvailable = available
  }

  override fun isFullAvailable(): Boolean = fullAvailable

  override fun setFullAvailable(available: Boolean) {
    this.fullAvailable = available
  }

  fun toCommandHandler() = CommandHandler.of(this, ProfilePictureAvroSnapshotEventMapper)
}

fun ProfilePicture.asSnapshot() = ProfilePictureSnapshot(this)

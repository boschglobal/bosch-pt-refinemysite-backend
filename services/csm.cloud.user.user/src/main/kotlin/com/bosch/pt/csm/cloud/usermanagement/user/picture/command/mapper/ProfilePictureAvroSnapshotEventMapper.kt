/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.picture.command.mapper

import com.bosch.pt.csm.cloud.common.command.mapper.AbstractAvroSnapshotMapper
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USERPICTURE
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.picture.command.snapshotstore.ProfilePictureSnapshot

object ProfilePictureAvroSnapshotEventMapper :
    AbstractAvroSnapshotMapper<ProfilePictureSnapshot>() {

  override fun <E : Enum<*>> toAvroMessageWithNewVersion(
      snapshot: ProfilePictureSnapshot,
      eventType: E
  ) =
      UserPictureEventAvro.newBuilder()
          .setName(eventType as UserPictureEventEnumAvro)
          .setAggregateBuilder(
              UserPictureAggregateAvro.newBuilder()
                  .setAggregateIdentifier(toAggregateIdentifierAvroWithNextVersion(snapshot))
                  .setAuditingInformation(toUpdatedAuditingInformationAvro(snapshot))
                  .setUser(snapshot.user.toAggregateIdentifier())
                  .setSmallAvailable(snapshot.isSmallAvailable())
                  .setFullAvailable(snapshot.isFullAvailable())
                  .setFileSize(snapshot.fileSize ?: 0)
                  .setHeight(snapshot.height ?: 0)
                  .setWidth(snapshot.width ?: 0))
          .build()

  override fun getAggregateType() = USERPICTURE.name

  override fun getRootContextIdentifier(snapshot: ProfilePictureSnapshot) =
      snapshot.user.identifier.toUuid()
}

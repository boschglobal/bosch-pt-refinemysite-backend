/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.picture.api

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.usermanagement.user.picture.ProfilePictureId

@Suppress("UnnecessaryAbstractClass")
abstract class ProfilePictureCommands(open val identifier: ProfilePictureId)

data class SaveProfilePictureCommand(
    override val identifier: ProfilePictureId,
    val userIdentifier: UserId,
    val binaryData: ByteArray,
    val fileName: String
) : ProfilePictureCommands(identifier)

data class UpdateImageMetadataCommand(
    override val identifier: ProfilePictureId,
    val fileSize: Long
) : ProfilePictureCommands(identifier)

data class DeleteProfilePictureOfUserCommand(
    val userIdentifier: UserId,
    val failIfNotExists: Boolean = true
)

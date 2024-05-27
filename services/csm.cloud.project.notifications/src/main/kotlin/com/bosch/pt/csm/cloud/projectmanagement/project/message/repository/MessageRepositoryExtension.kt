/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.message.repository

import java.util.UUID

interface MessageRepositoryExtension {

    fun deleteMessage(identifier: UUID, projectIdentifier: UUID)
}

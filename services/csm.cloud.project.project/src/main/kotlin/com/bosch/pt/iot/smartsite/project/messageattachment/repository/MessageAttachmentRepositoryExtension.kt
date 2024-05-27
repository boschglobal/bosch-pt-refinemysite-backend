/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.messageattachment.repository

import com.bosch.pt.iot.smartsite.project.messageattachment.model.MessageAttachment

interface MessageAttachmentRepositoryExtension {

  fun getByMessageIdsPartitioned(messageIds: List<Long>): List<MessageAttachment>

  fun deletePartitioned(ids: List<Long>)
}
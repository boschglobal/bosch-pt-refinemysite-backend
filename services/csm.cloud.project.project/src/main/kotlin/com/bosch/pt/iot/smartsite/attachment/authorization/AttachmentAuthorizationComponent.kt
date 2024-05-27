/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.attachment.authorization

import com.bosch.pt.iot.smartsite.project.attachment.repository.AttachmentRepository
import com.bosch.pt.iot.smartsite.project.task.authorization.TaskAuthorizationComponent
import java.util.UUID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
open class AttachmentAuthorizationComponent {

  @Autowired private lateinit var attachmentRepository: AttachmentRepository

  @Autowired private lateinit var taskAuthorizationComponent: TaskAuthorizationComponent

  open fun hasViewPermissionOnAttachment(attachmentIdentifier: UUID) =
      attachmentRepository.findTaskIdentifierByIdentifier(attachmentIdentifier).let {
        it != null && taskAuthorizationComponent.hasViewPermissionOnTask(it)
      }
}

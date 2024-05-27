/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.common.facade.rest

import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.FULL
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.MEDIUM
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.ORIGINAL
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.SMALL
import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils
import java.net.URI

object DeletedAttachmentUriBuilder {

  const val FILE_NAME_DELETED_IMAGE_LARGE = "deleted-image-large.png"
  const val FILE_NAME_DELETED_IMAGE_SMALL = "deleted-image-small.png"

  fun buildDeletedAttachmentUri(imageResolution: ImageResolution): URI =
      when (imageResolution) {
        SMALL -> FILE_NAME_DELETED_IMAGE_SMALL
        FULL, MEDIUM, ORIGINAL -> FILE_NAME_DELETED_IMAGE_LARGE
      }.let { LinkUtils.linkTemplateWithPathSegmentsUnversioned(it).build().toUri() }
}

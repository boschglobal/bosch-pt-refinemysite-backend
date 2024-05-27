/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.attachment.model

import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.FULL
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.SMALL

enum class AttachmentImageResolution {
  DATA {
    override val imageResolution: ImageResolution
      get() = FULL
  },
  PREVIEW {
    override val imageResolution: ImageResolution
      get() = SMALL
  },
  ORIGINAL {
    override val imageResolution: ImageResolution
      get() = ImageResolution.ORIGINAL
  };

  abstract val imageResolution: ImageResolution
}

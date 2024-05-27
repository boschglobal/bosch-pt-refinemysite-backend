/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.news.facade.rest.resource

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractResource
import com.bosch.pt.csm.cloud.projectmanagement.news.model.ObjectIdentifier
import java.time.Instant

class NewsResource(
    var context: ObjectIdentifier,
    var parent: ObjectIdentifier,
    var root: ObjectIdentifier,
    var createdDate: Instant?,
    var lastModifiedDate: Instant?
) : AbstractResource()

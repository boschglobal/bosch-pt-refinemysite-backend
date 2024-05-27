/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.topic.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractSliceResource

class TopicBatchResource(val topics: Collection<TopicResource>, pageNumber: Int, pageSize: Int) :
    AbstractSliceResource(pageNumber, pageSize)

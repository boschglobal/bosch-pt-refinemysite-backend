/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.resource

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractResource

open class StatisticsListResource(var items: List<StatisticsResource>) : AbstractResource()

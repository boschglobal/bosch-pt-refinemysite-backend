/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.news.facade.rest.resource

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractResource

class NewsListResource(var items: List<NewsResource>) : AbstractResource()

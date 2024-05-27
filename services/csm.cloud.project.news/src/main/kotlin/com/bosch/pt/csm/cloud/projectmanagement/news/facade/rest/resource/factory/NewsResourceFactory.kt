/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.news.facade.rest.resource.factory

import com.bosch.pt.csm.cloud.projectmanagement.news.facade.rest.resource.NewsResource
import com.bosch.pt.csm.cloud.projectmanagement.news.model.News
import org.springframework.stereotype.Component

@Component
class NewsResourceFactory {

  fun build(news: News): NewsResource =
      with(news) {
        NewsResource(contextObject, parentObject, rootObject, createdDate, lastModifiedDate)
      }
}

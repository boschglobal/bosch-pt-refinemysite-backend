/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.news.facade.rest.resource.factory

import com.bosch.pt.csm.cloud.projectmanagement.news.facade.rest.resource.NewsListResource
import com.bosch.pt.csm.cloud.projectmanagement.news.model.News
import org.springframework.stereotype.Component

@Component
class NewsListResourceFactory(private val newsResourceFactory: NewsResourceFactory) {

  fun build(newsList: List<News>): NewsListResource = buildListResource(newsList)

  private fun buildListResource(newsList: List<News>): NewsListResource =
      NewsListResource(newsList.map { news -> newsResourceFactory.build(news) })
}

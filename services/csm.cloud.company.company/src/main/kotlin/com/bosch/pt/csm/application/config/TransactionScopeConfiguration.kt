/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.application.config

import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.support.SimpleTransactionScope

@Configuration
class TransactionScopeConfiguration : BeanFactoryPostProcessor {

  override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
    beanFactory.registerScope(NAME, SimpleTransactionScope())
  }

  companion object {
    const val NAME = "tx"
  }
}

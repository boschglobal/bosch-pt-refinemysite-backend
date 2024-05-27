/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.facade.rest

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET
import org.springframework.context.annotation.Bean
import org.springframework.web.context.annotation.RequestScope

@AutoConfiguration
@ConditionalOnWebApplication(type = SERVLET)
class CustomLinkBuilderFactoryAutoConfiguration {

  @Bean @RequestScope fun customLinkBuilderFactory() = CustomLinkBuilderFactory()
}

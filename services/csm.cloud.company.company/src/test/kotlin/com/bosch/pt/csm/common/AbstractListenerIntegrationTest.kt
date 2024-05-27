/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.common

import com.bosch.pt.csm.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.application.config.EnableAllKafkaListeners
import com.bosch.pt.csm.common.facade.AbstractIntegrationTest

@EnableAllKafkaListeners
@SmartSiteSpringBootTest
@Suppress("UnnecessaryAbstractClass")
abstract class AbstractListenerIntegrationTest : AbstractIntegrationTest()

/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.common.test.context;

import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;

public class RelaxedCacheTestContextBootstrapper extends SpringBootTestContextBootstrapper {

  @Override
  protected CacheAwareContextLoaderDelegate getCacheAwareContextLoaderDelegate() {
    return RelaxedCacheAwareContextLoaderDelegate.getInstance();
  }
}

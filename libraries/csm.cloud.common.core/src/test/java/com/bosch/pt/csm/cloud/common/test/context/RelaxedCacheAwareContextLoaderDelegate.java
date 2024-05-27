/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.common.test.context;

import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.cache.ContextCache;
import org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate;

public class RelaxedCacheAwareContextLoaderDelegate extends DefaultCacheAwareContextLoaderDelegate {

  private static final CacheAwareContextLoaderDelegate INSTANCE =
      new RelaxedCacheAwareContextLoaderDelegate(new RelaxedContextCache());

  private RelaxedCacheAwareContextLoaderDelegate(ContextCache contextCache) {
    super(contextCache);
  }

  public static CacheAwareContextLoaderDelegate getInstance() {
    return INSTANCE;
  }
}

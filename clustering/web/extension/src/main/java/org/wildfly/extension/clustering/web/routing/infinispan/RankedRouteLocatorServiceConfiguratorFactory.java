/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.clustering.web.routing.infinispan;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.wildfly.clustering.ee.infinispan.InfinispanCacheConfiguration;
import org.wildfly.clustering.web.WebDeploymentConfiguration;
import org.wildfly.clustering.web.infinispan.routing.RankedRoutingConfiguration;
import org.wildfly.extension.clustering.web.routing.RouteLocatorServiceConfiguratorFactory;

/**
 * Factory for creating a service configurator for a ranked route locator.
 * @author Paul Ferraro
 */
public class RankedRouteLocatorServiceConfiguratorFactory<C extends InfinispanCacheConfiguration> implements RouteLocatorServiceConfiguratorFactory<C> {

    private final RankedRoutingConfiguration config;

    public RankedRouteLocatorServiceConfiguratorFactory(RankedRoutingConfiguration config) {
        this.config = config;
    }

    @Override
    public CapabilityServiceConfigurator createRouteLocatorServiceConfigurator(C configuration, WebDeploymentConfiguration deploymentConfiguration) {
        return new RankedRouteLocatorServiceConfigurator(configuration, deploymentConfiguration, this.config);
    }

    public RankedRoutingConfiguration getConfiguration() {
        return this.config;
    }
}

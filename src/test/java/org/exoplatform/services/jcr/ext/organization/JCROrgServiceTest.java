/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.exoplatform.services.jcr.ext.organization;

import org.exoplatform.container.configuration.ConfigurationException;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.registry.RegistryService;

/**
 * @author <a href="dvishinskiy@exoplatform.com">Dmitriy Vishinskiy</a>
 * @version $Id:$
 */
public class JCROrgServiceTest extends JCROrganizationServiceImpl
{

   public JCROrgServiceTest(InitParams params, RepositoryService repositoryService, CacheService cservice)
      throws ConfigurationException
   {
      super(params, repositoryService, cservice);
   }

   public JCROrgServiceTest(InitParams initParams, RepositoryService repositoryService,
      RegistryService registryService, CacheService cservice) throws ConfigurationException
   {
      super(initParams, repositoryService, registryService, cservice);
   }

   public void setStorageWorkspace(String workspaceName)
   {
      this.storageWorkspace = workspaceName;
   }

}

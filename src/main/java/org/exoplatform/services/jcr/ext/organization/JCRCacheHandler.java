/*
 * Copyright (C) 2011 eXo Platform SAS.
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

import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.organization.CacheHandler;
import org.exoplatform.services.organization.Group;

import java.io.Serializable;

import javax.jcr.RepositoryException;

/**
 * Cache handler for JCR implementation of organization service. Contains method for
 * hierarchically removing groups and related membership records. To remove node and its 
 * children in JCR need to remove only parent node, but cache has not hierarchy structrue.
 * We need dedicated method for this. 
 * 
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id$
 */
public class JCRCacheHandler extends CacheHandler
{
   private static char DELIMITER = ':';

   private final JCROrganizationServiceImpl jcrOrganizationServiceImpl;

   /**
    * JCRCacheHandler constructor.
    * 
    * @param cservice
    *          Cache service
    */
   public JCRCacheHandler(CacheService cservice, JCROrganizationServiceImpl jcrOrganizationServiceImpl)
   {
      super(cservice);
      this.jcrOrganizationServiceImpl = jcrOrganizationServiceImpl;
   }

   /**
    * Hierarchically removing groups and membership records in the cache.
    * 
    * @param groupId
    *          the parent group id
    */
   public void removeGroupHierarchy(String groupId)
   {
      try
      {
         for (Group group : groupCache.getCachedObjects())
         {
            if (group.getId().startsWith(groupId))
            {
               remove(group.getId(), CacheType.GROUP);
               remove(CacheHandler.GROUP_PREFIX + group.getId(), CacheType.MEMBERSHIP);
            }
         }
      }
      catch (Exception e)
      {
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected Serializable createCacheKey(Serializable orgServiceKey)
   {
      // Safe check
      if (orgServiceKey instanceof String)
      {
         try
         {
            // add "repository:" to OrgSerivce Key
            return jcrOrganizationServiceImpl.getWorkingRepository().getConfiguration().getName() + DELIMITER
               + orgServiceKey;
         }
         catch (RepositoryException e)
         {
            throw new IllegalStateException(e.getMessage(), e);
         }
         catch (RepositoryConfigurationException e)
         {
            throw new IllegalStateException(e.getMessage(), e);
         }
      }
      else
      {
         return orgServiceKey;
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected boolean matchKey(Serializable cacheKey)
   {
      if (cacheKey instanceof String)
      {
         try
         {
            // check is prefix equals to "repository:"
            String prefix = jcrOrganizationServiceImpl.getWorkingRepository().getConfiguration().getName() + DELIMITER;
            return ((String)cacheKey).startsWith(prefix);
         }
         catch (RepositoryException e)
         {
            throw new IllegalStateException(e.getMessage(), e);
         }
         catch (RepositoryConfigurationException e)
         {
            throw new IllegalStateException(e.getMessage(), e);
         }
      }
      return false;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected Serializable createOrgServiceKey(Serializable cacheKey)
   {
      if (cacheKey instanceof String)
      {
         // trim "repository:" from Cache Key
         int indexOfDelimiter = ((String)cacheKey).indexOf(DELIMITER);
         if (indexOfDelimiter >= 0)
         {
            return ((String)cacheKey).substring(indexOfDelimiter + 1);
         }
      }
      return cacheKey;
   }

}

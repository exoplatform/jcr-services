/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.ext.organization;

import org.exoplatform.services.organization.OrganizationService;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Dec 1, 2011  
 */
public class TestMultipleRepositories extends AbstractOrganizationServiceTest
{

   public void testSetCurrent() throws Exception
   {
      String currentRepo = repositoryService.getCurrentRepository().getConfiguration().getName();
      String userName = "TestMultipleRepositories-User1";
      String groupName = "TestMultipleRepositories-Group1";
      String type = "TestMultipleRepositories-Type1";
      try
      {
         createMembership(userName, groupName, type);
         // warmup cache
         uHandler.findUserByName(userName);
         mHandler.findMembershipsByUser(userName);
         mHandler.findMembershipByUserGroupAndType(userName, groupName, type);

         repositoryService.setCurrentRepositoryName("db2");
         prepareRepository();
         assertNull(uHandler.findUserByName(userName));
         assertNull(mHandler.findMembershipByUserGroupAndType(userName, groupName, type));
         createMembership(userName, groupName, type);
      }
      finally
      {
         repositoryService.setCurrentRepositoryName(currentRepo);
      }
   }

   private void prepareRepository() throws RepositoryException
   {
      JCROrganizationServiceImpl organizationService =
         (JCROrganizationServiceImpl)container.getComponentInstanceOfType(OrganizationService.class);
      Session storageSession = organizationService.getStorageSession();

      // will create new
      Node storage =
         storageSession.getRootNode().addNode(organizationService.getStoragePath().substring(1),
            "jos:organizationStorage");

      storage.addNode(UserHandlerImpl.STORAGE_JOS_USERS, "jos:organizationUsers");
      storage.addNode(GroupHandlerImpl.STORAGE_JOS_GROUPS, "jos:organizationGroups");
      storage.addNode(MembershipTypeHandlerImpl.STORAGE_JOS_MEMBERSHIP_TYPES, "jos:organizationMembershipTypes");

      storageSession.save(); // storage done configure
   }
}

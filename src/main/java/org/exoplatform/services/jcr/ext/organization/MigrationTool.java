/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS.
 * 
 * Date: 04.07.2012
 * 
 * @author <a href="mailto:dvishinskiy@exoplatform.com">Dmitriy Vishinskiy</a>
 * @version $Id: JCROrganizationServiceMigration.java 76870 2012-07-04 10:38:54Z dkuleshov $
 */

public class MigrationTool
{
   private JCROrganizationServiceImpl service;

   /**
    * Path where old structure will be moved.
    */
   private String oldStoragePath;

   /**
    * The child node of user node where membership is stored (old structure).
    */
   public static final String JOS_USER_MEMBERSHIP = "jos:userMembership";

   /**
    * The property of user node where group node uuid is stored (old structure).
    */
   public static final String JOS_GROUP = "jos:group";

   /**
    * The child node of user node where attributes is stored (old structure).
    */
   public static final String JOS_ATTRIBUTES = "jos:attributes";

   /**
    * The nodetype of old organization structure root node.
    */
   public static final String JOS_ORGANIZATION_NODETYPE_OLD = "jos:organizationStorage";

   /**
    * The property of a group node where parent id is stored (old structure).
    */
   public static final String JOS_PARENT_ID = "jos:parentId";

   /**
    * The property of a membership node where group id is stored (old structure).
    */
   public static final String JOS_GROUP_ID = "jos:groupId";

   protected static final Log LOG = ExoLogger.getLogger("exo-jcr-services.MigrationTool");

   /**
    * MigrationTool constructor.
    */
   MigrationTool(JCROrganizationServiceImpl service)
   {
      this.service = service;
   }

   /**
    * Method that aggregates all needed migration operations in needed order.
    * @throws RepositoryException
    */
   void migrate() throws RepositoryException
   {
      try
      {
         LOG.info("Migration started.");

         moveOldStructure();
         service.createStructure();

         migrateUsers();
         migrateMembershipTypes();
         migrateGroups();
         migrateProfiles();
         migrateMemberships();

         removeOldStructure();

         LOG.info("Migration completed.");
      }
      catch (Exception e)
      {
         throw new RepositoryException("Migration failed", e);
      }
   }

   /**
    * Method to know if migration is need.
    * @return true if migration is need false otherwise.
    * @throws RepositoryException 
    */
   boolean migrationRequired() throws RepositoryException
   {
      Session session = service.getStorageSession();
      oldStoragePath = service.getStoragePath() + "-old";

      try
      {
         if (session.itemExists(oldStoragePath))
         {
            return true;
         }

         try
         {
            Node node = (Node)session.getItem(service.getStoragePath());
            return node.isNodeType(JOS_ORGANIZATION_NODETYPE_OLD);
         }
         catch (PathNotFoundException e)
         {
            return false;
         }
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Method for moving old storage into temporary location.
    * @throws Exception
    */
   private void moveOldStructure() throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         try
         {
            if (session.itemExists(oldStoragePath))
            {
               session.getItem(service.getStoragePath()).remove();
               return;
            }
         }
         catch (PathNotFoundException e)
         {
            return;
         }

         session.move(service.getStoragePath(), oldStoragePath);
         session.save();
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Method for removing old storage from temporary location.
    * @throws RepositoryException
    */
   private void removeOldStructure() throws RepositoryException
   {
      Session session = service.getStorageSession();
      try
      {
         session.getItem(oldStoragePath).remove();
         session.save();
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Method for users migration.
    * @throws Exception
    */
   private void migrateUsers() throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         NodeIterator iterator =
            ((Node)session.getItem(oldStoragePath)).getNode(JCROrganizationServiceImpl.STORAGE_JOS_USERS).getNodes();

         while (iterator.hasNext())
         {
            ((UserHandlerImpl)service.getUserHandler()).migrateUser(iterator.nextNode());
         }
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Method for groups migration. Must be run after users and membershipTypes migration.
    * @throws Exception
    */
   private void migrateGroups() throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         NodeIterator iterator =
            ((Node)session.getItem(oldStoragePath)).getNode(JCROrganizationServiceImpl.STORAGE_JOS_GROUPS).getNodes();
         while (iterator.hasNext())
         {
            Node oldGroupNode = iterator.nextNode();
            ((GroupHandlerImpl)service.getGroupHandler()).migrateGroup(oldGroupNode);

            migrateGroups(oldGroupNode);
         }
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Method for groups migration.
    * @throws Exception
    */
   private void migrateGroups(Node startNode) throws Exception
   {
      NodeIterator iterator = startNode.getNodes();
      while (iterator.hasNext())
      {
         Node oldGroupNode = iterator.nextNode();
         ((GroupHandlerImpl)service.getGroupHandler()).migrateGroup(oldGroupNode);

         migrateGroups(oldGroupNode);
      }
   }

   /**
    * Method for membershipTypes migration.
    * @throws Exception
    */
   private void migrateMembershipTypes() throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         NodeIterator iterator =
            ((Node)session.getItem(oldStoragePath)).getNode(JCROrganizationServiceImpl.STORAGE_JOS_MEMBERSHIP_TYPES)
               .getNodes();
         while (iterator.hasNext())
         {
            Node oldTypeNode = iterator.nextNode();
            ((MembershipTypeHandlerImpl)service.getMembershipTypeHandler()).migrateMembershipType(oldTypeNode);
         }
      }
      finally
      {
         session.logout();
      }

   }

   /**
    * Method for profiles migration.
    * @throws Exception
    */
   private void migrateProfiles() throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         NodeIterator iterator =
            ((Node)session.getItem(oldStoragePath)).getNode(JCROrganizationServiceImpl.STORAGE_JOS_USERS).getNodes();
         while (iterator.hasNext())
         {
            Node oldUserNode = iterator.nextNode();
            ((UserProfileHandlerImpl)service.getUserProfileHandler()).migrateProfile(oldUserNode);
         }
      }
      finally
      {
         session.logout();
      }

   }

   /**
    * Method for memberships migration.
    * @throws Exception
    */
   private void migrateMemberships() throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         NodeIterator iterator =
            ((Node)session.getItem(oldStoragePath)).getNode(JCROrganizationServiceImpl.STORAGE_JOS_USERS).getNodes();
         while (iterator.hasNext())
         {
            Node oldUserNode = iterator.nextNode();
            ((MembershipHandlerImpl)service.getMembershipHandler()).migrateMemberships(oldUserNode);
         }
      }
      finally
      {
         session.logout();
      }

   }
}

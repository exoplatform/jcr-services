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

import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

import java.io.InputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * @author <a href="dvishinskiy@exoplatform.com">Dmitriy Vishinskiy</a>
 * @version $Id:$
 */
public class TestMigrationTool extends AbstractOrganizationServiceTest
{
   public void testMigrationTool() throws Exception
   {
      repository = (RepositoryImpl)repositoryService.getDefaultRepository();
      Session sess = (SessionImpl)repository.login(credentials, "ws5");
      TesterJCROrgService organizationService =
         (TesterJCROrgService)container.getComponentInstanceOfType(TesterJCROrgService.class);
      MigrationTool migrationTool = new MigrationTool((TesterJCROrgService)organizationService);

      organizationService.saveStorageWorkspaceName();
      organizationService.setStorageWorkspace("ws5");

      loadDataFromDump(sess, "jcrorgservice114dump.xml");

      assertTrue(migrationTool.migrationRequired());
      migrationTool.migrate();

      Node marryUserNode =
         (Node)sess.getItem(JCROrganizationServiceImpl.STORAGE_PATH_DEFAULT + "/"
            + JCROrganizationServiceImpl.STORAGE_JOS_USERS + "/marry");
      assertUserMigration(marryUserNode);

      Node administratorsGroupNode =
         (Node)sess.getItem(JCROrganizationServiceImpl.STORAGE_PATH_DEFAULT + "/"
            + JCROrganizationServiceImpl.STORAGE_JOS_GROUPS + "/platform/administrators");

      assertGroupMigration(administratorsGroupNode);
      assertMembershipsMigration(administratorsGroupNode, organizationService);
      assertMembershipTypesMigration(sess);

      organizationService.restoreStorageWorkspaceName();
   }

   private void loadDataFromDump(Session sess, String dumpName) throws Exception
   {

      if (sess.itemExists(JCROrganizationServiceImpl.STORAGE_PATH_DEFAULT))
      {
         sess.getItem(JCROrganizationServiceImpl.STORAGE_PATH_DEFAULT).remove();
      }
      InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(dumpName);
      sess.importXML("/", in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
      sess.save();
   }

   private void assertUserMigration(Node marryUserNode) throws RepositoryException
   {
      assertTrue(marryUserNode.getProperty("jos:firstName").getString().equals("Marry"));
      assertTrue(marryUserNode.getProperty("jos:lastName").getString().equals("Kelly"));
      assertTrue(marryUserNode.isNodeType(JCROrganizationServiceImpl.JOS_USERS_NODETYPE));
      assertTrue(marryUserNode.getNode(JCROrganizationServiceImpl.JOS_PROFILE).isNodeType("jos:userProfile-115"));

      PropertyIterator iterator = marryUserNode.getNode(JCROrganizationServiceImpl.JOS_PROFILE).getProperties();
      while (iterator.hasNext())
      {
         Property prop = iterator.nextProperty();
         if (prop.getName().equals("jcr:primaryType"))
         {
            continue;
         }
         assertTrue(prop.getName().startsWith("attr."));
      }
   }

   private void assertGroupMigration(Node administratorsGroupNode) throws RepositoryException
   {
      assertTrue(administratorsGroupNode.isNodeType(JCROrganizationServiceImpl.JOS_HIERARCHY_GROUP_NODETYPE));

      NodeIterator iterat = administratorsGroupNode.getNodes();
      while (iterat.hasNext())
      {
         Node nd = iterat.nextNode();
         assertTrue(nd.isNodeType("jos:memberships-115"));
      }
   }

   private void assertMembershipsMigration(Node administratorsGroupNode, JCROrganizationServiceImpl organizationService)
      throws Exception
   {
      assertEquals(2, organizationService.getUserHandler().findUsersByGroup("/platform/administrators").getAll().size());

   }

   private void assertMembershipTypesMigration(Session sess) throws RepositoryException
   {
      assertEquals(3, ((Node)sess.getItem("/exo:organization/jos:membershipTypes")).getNodes().getSize());

   }
}

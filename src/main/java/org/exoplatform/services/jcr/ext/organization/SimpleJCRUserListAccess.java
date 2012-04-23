/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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

import org.exoplatform.services.jcr.core.ExtendedNode;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: SimpleUserListAccess.java 111 2008-11-11 11:11:11Z $
 */
public class SimpleJCRUserListAccess extends JCRUserListAccess
{

   /**
    * The parent node where all users nodes are persisted.
    */
   private final ExtendedNode usersStorageNode;

   /**
    * JCRUserListAccess constructor.
    */
   public SimpleJCRUserListAccess(JCROrganizationServiceImpl service) throws RepositoryException
   {
      super(service);
      usersStorageNode = getUsersStorageNode();
   }

   /**
    * {@inheritDoc}
    */
   protected int getSize(Session session) throws Exception
   {
      return (int)usersStorageNode.getNodesCount();
   }

   /**
    * {@inheritDoc}
    */
   protected Object readObject(Node node) throws Exception
   {
      return uHandler.readUser(node);
   }

   /**
    * {@inheritDoc}
    */
   protected NodeIterator createIterator(Session session) throws RepositoryException
   {
      return usersStorageNode.getNodesLazily(DEFAULT_PAGE_SIZE);
   }

   /**
    * Returns users storage node.
    */
   private ExtendedNode getUsersStorageNode() throws RepositoryException
   {
      Session session = service.getStorageSession();
      try
      {
         return (ExtendedNode)utils.getUsersStorageNode(service.getStorageSession());
      }
      finally
      {
         session.logout();
      }
   }
}

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

import org.exoplatform.services.organization.User;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
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
    * JCRUserListAccess constructor.
    */
   public SimpleJCRUserListAccess(JCROrganizationServiceImpl service)
   {
      super(service);
   }

   /**
    * {@inheritDoc}
    */
   protected int getSize(Session session) throws Exception
   {
      Node usersStorageNode = utils.getUsersStorageNode(session);
      return (int)usersStorageNode.getNodes().getSize();
   }

   /**
    * {@inheritDoc}
    */
   protected User[] load(Session session, int index, int length) throws Exception
   {
      if (index < 0)
      {
         throw new IllegalArgumentException("Illegal index: index must be a positive number");
      }

      if (length < 0)
      {
         throw new IllegalArgumentException("Illegal length: length must be a positive number");
      }

      User[] users = new User[length];

      try
      {
         Node usersStorageNode = utils.getUsersStorageNode(session);

         NodeIterator usersNodes = usersStorageNode.getNodes();
         for (int p = 0, counter = 0; counter < length; p++)
         {
            if (!usersNodes.hasNext())
            {
               throw new IllegalArgumentException(
                  "Illegal index or length: sum of the index and the length cannot be greater than the list size");
            }

            Node result = usersNodes.nextNode();
            if (p >= index)
            {
               users[counter++] = uHandler.readUser(result);
            }
         }

         return users;
      }
      catch (Exception e)
      {
         throw new OrganizationServiceException("Can not load users", e);
      }
   }
}

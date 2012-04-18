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

import org.exoplatform.services.jcr.ext.organization.UserHandlerImpl.UserProperties;
import org.exoplatform.services.organization.User;

import java.util.Date;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: UserByQueryJCRUserListAccess.java 111 2008-11-11 11:11:11Z $
 */
public class UserByQueryJCRUserListAccess extends JCRUserListAccess
{

   /**
    * The query.
    */
   private org.exoplatform.services.organization.Query query;

   /**
    * JCRUserListAccess constructor.
    */
   public UserByQueryJCRUserListAccess(JCROrganizationServiceImpl service,
      org.exoplatform.services.organization.Query query)
   {
      super(service);
      this.query = query;
   }

   /**
    * {@inheritDoc}
    */
   protected int getSize(Session session) throws Exception
   {
      try
      {
         int result = 0;

         Node usersStorageNode = utils.getUsersStorageNode(session);

         NodeIterator userNodes = usersStorageNode.getNodes();
         while (userNodes.hasNext())
         {
            Node uNode = userNodes.nextNode();

            if (acceptQuery(uNode))
            {
               result++;
            }
         }

         return result;
      }
      catch (RepositoryException e)
      {
         throw new OrganizationServiceException("Can not get list size", e);
      }
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

      Node usersStorageNode = utils.getUsersStorageNode(session);

      NodeIterator userNodes = usersStorageNode.getNodes();
      for (int p = 0, counter = 0; counter < length;)
      {
         if (!userNodes.hasNext())
         {
            throw new IllegalArgumentException(
               "Illegal index or length: sum of the index and the length cannot be greater than the list size");
         }

         Node userNode = userNodes.nextNode();
         if (!acceptQuery(userNode))
         {
            continue;
         }

         if (p++ >= index)
         {
            users[counter++] = uHandler.readUser(userNode);
         }
      }

      return users;
   }

   private boolean acceptQuery(Node uNode) throws Exception
   {
      if (query.getUserName() != null && !isLike(uNode.getName(), query.getUserName(), true))
      {
         return false;
      }

      if (query.getFirstName() != null
         && !isLike(utils.readString(uNode, UserProperties.JOS_FIRST_NAME), query.getFirstName(), true))
      {
         return false;
      }

      if (query.getLastName() != null
         && !isLike(utils.readString(uNode, UserProperties.JOS_LAST_NAME), query.getLastName(), true))
      {
         return false;
      }

      if (query.getEmail() != null
         && !isLike(utils.readString(uNode, UserProperties.JOS_EMAIL), query.getEmail(), false))
      {
         return false;
      }

      Date lastLoginTime = utils.readDate(uNode, UserProperties.JOS_LAST_LOGIN_TIME);
      if (query.getFromLoginDate() != null
         && (lastLoginTime == null || query.getFromLoginDate().getTime() > lastLoginTime.getTime()))
      {
         return false;
      }

      if (query.getToLoginDate() != null
         && (lastLoginTime == null || query.getToLoginDate().getTime() < lastLoginTime.getTime()))
      {
         return false;
      }

      return true;
   }

   private boolean isLike(String jcrField, String queryField, boolean caseSensitive)
   {
      return caseSensitive ? jcrField.toUpperCase().indexOf(removeAsterisk(queryField.toUpperCase())) != -1 : jcrField
         .indexOf(removeAsterisk(queryField)) != -1;
   }

   private String removeAsterisk(String str)
   {
      if (str.startsWith("*"))
      {
         str = str.substring(1);
      }
      if (str.endsWith("*"))
      {
         str = str.substring(0, str.length() - 1);
      }
      return str;
   }
}

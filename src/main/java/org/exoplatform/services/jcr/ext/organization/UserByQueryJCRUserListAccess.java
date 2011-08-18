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

import java.util.Date;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
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
    * User handler.
    */
   private UserHandlerImpl uHandler;

   /**
    * JCRUserListAccess constructor.
    * 
    * @param service The JCROrganizationService
    * @param query The query
    */
   public UserByQueryJCRUserListAccess(JCROrganizationServiceImpl service,
      org.exoplatform.services.organization.Query query)
   {
      super(service);
      this.query = query;
      this.uHandler = new UserHandlerImpl(service);
   }

   /**
    * {@inheritDoc}
    */
   protected int getSize(Session session) throws Exception
   {
      try
      {
         int result = 0;

         Node storageNode = (Node)session.getItem(service.getStoragePath() + "/" + UserHandlerImpl.STORAGE_JOS_USERS);
         NodeIterator results = storageNode.getNodes();

         while (results.hasNext())
         {
            Node uNode = results.nextNode();

            if (checkQuery(uNode))
            {
               result++;
            }
         }

         return result;
      }
      catch (Exception e)
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
         throw new IllegalArgumentException("Illegal index: index must be a positive number");

      if (length < 0)
         throw new IllegalArgumentException("Illegal length: length must be a positive number");

      try
      {
         User[] users = new User[length];

         Node storageNode = (Node)session.getItem(service.getStoragePath() + "/" + UserHandlerImpl.STORAGE_JOS_USERS);
         NodeIterator results = storageNode.getNodes();

         for (int p = 0, counter = 0; counter < length;)
         {
            if (!results.hasNext())
               throw new IllegalArgumentException(
                  "Illegal index or length: sum of the index and the length cannot be greater than the list size");

            Node uNode = results.nextNode();

            if (!checkQuery(uNode))
            {
               continue;
            }

            if (p++ >= index)
            {
               users[counter++] = uHandler.readObjectFromNode(uNode);
            }
         }

         return users;
      }
      catch (Exception e)
      {
         throw new OrganizationServiceException("Can not load users", e);
      }
   }

   private boolean checkQuery(Node uNode) throws Exception
   {
      if (query.getUserName() != null && !isLike(uNode.getName(), query.getUserName(), true))
      {
         return false;
      }

      if (query.getFirstName() != null
         && !isLike(uHandler.readStringProperty(uNode, UserHandlerImpl.JOS_FIRST_NAME), query.getFirstName(), true))
      {
         return false;
      }

      if (query.getLastName() != null
         && !isLike(uHandler.readStringProperty(uNode, UserHandlerImpl.JOS_LAST_NAME), query.getLastName(), true))
      {
         return false;
      }

      if (query.getEmail() != null
         && !isLike(uHandler.readStringProperty(uNode, UserHandlerImpl.JOS_EMAIL), query.getEmail(), false))
      {
         return false;
      }

      Date lastLoginTime = uHandler.readDateProperty(uNode, UserHandlerImpl.JOS_LAST_LOGIN_TIME);
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

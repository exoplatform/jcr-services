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

import java.util.Date;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.exoplatform.services.organization.User;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: UserByQueryJCRUserListAccess.java 111 2008-11-11 11:11:11Z $
 */
public class UserByQueryJCRUserListAccess extends JCRUserListAccess {

  /**
   * The query.
   */
  private org.exoplatform.services.organization.Query query;

  /**
   * JCRUserListAccess constructor.
   * 
   * @param service
   *          The JCROrganizationService
   * @param query
   *          The query
   */
  public UserByQueryJCRUserListAccess(JCROrganizationServiceImpl service,
                                      org.exoplatform.services.organization.Query query) {
    super(service);
    this.query = query;
  }

  /**
   * {@inheritDoc}
   */
  protected int getSize(Session session) throws Exception {
    try {
      int result = 0;

      String where = "jcr:path LIKE '" + "%" + "'";
      if (query.getEmail() != null) {
        where += " AND " + ("exo:email LIKE '" + query.getEmail().replace('*', '%') + "'");
      }
      if (query.getFirstName() != null) {
        where += " AND " + ("exo:firstName LIKE '" + query.getFirstName().replace('*', '%') + "'");
      }
      if (query.getLastName() != null) {
        where += " AND " + ("exo:lastName LIKE '" + query.getLastName().replace('*', '%') + "'");
      }

      UserHandlerImpl uHandler = new UserHandlerImpl(service);

      String statement = "select * from exo:user " + (where.length() == 0 ? "" : "where " + where);
      Query uQuery = session.getWorkspace().getQueryManager().createQuery(statement, Query.SQL);
      QueryResult uRes = uQuery.execute();

      for (NodeIterator results = uRes.getNodes(); results.hasNext();) {
        Node uNode = results.nextNode();

        if (query.getUserName() == null || isNameLike(uNode.getName(), query.getUserName())) {
          Date lastLoginTime = uHandler.readDateProperty(uNode, UserHandlerImpl.EXO_LAST_LOGIN_TIME);
          if ((query.getFromLoginDate() == null || (lastLoginTime != null && query.getFromLoginDate()
                                                                                  .getTime() <= lastLoginTime.getTime()))
              && (query.getToLoginDate() == null || (lastLoginTime != null && query.getToLoginDate()
                                                                                   .getTime() >= lastLoginTime.getTime()))) {
            result++;
          }
        }
      }

      return result;
    } catch (Exception e) {
      throw new OrganizationServiceException("Can not get list size", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  protected User[] load(Session session, int index, int length) throws Exception {
    if (index < 0)
      throw new IllegalArgumentException("Illegal index: index must be a positive number");

    if (length < 0)
      throw new IllegalArgumentException("Illegal length: length must be a positive number");

    try {
      User[] users = new User[length];

      String where = "jcr:path LIKE '" + "%" + "'";
      if (query.getEmail() != null) {
        where += " AND " + ("exo:email LIKE '" + query.getEmail().replace('*', '%') + "'");
      }
      if (query.getFirstName() != null) {
        where += " AND " + ("exo:firstName LIKE '" + query.getFirstName().replace('*', '%') + "'");
      }
      if (query.getLastName() != null) {
        where += " AND " + ("exo:lastName LIKE '" + query.getLastName().replace('*', '%') + "'");
      }

      UserHandlerImpl uHandler = new UserHandlerImpl(service);

      String statement = "select * from exo:user " + (where.length() == 0 ? "" : "where " + where);
      Query uQuery = session.getWorkspace().getQueryManager().createQuery(statement, Query.SQL);
      QueryResult uRes = uQuery.execute();

      NodeIterator results = uRes.getNodes();

      for (int p = 0, counter = 0; counter < length;) {
        if (!results.hasNext())
          throw new IllegalArgumentException("Illegal index or length: sum of the index and the length cannot be greater than the list size");

        Node uNode = results.nextNode();

        if (query.getUserName() == null || isNameLike(uNode.getName(), query.getUserName())) {
          Date lastLoginTime = uHandler.readDateProperty(uNode, UserHandlerImpl.EXO_LAST_LOGIN_TIME);
          if ((query.getFromLoginDate() == null || (lastLoginTime != null && query.getFromLoginDate()
                                                                                  .getTime() <= lastLoginTime.getTime()))
              && (query.getToLoginDate() == null || (lastLoginTime != null && query.getToLoginDate()
                                                                                   .getTime() >= lastLoginTime.getTime()))) {
            if (p++ >= index) {
              users[counter++] = uHandler.readObjectFromNode(uNode);
            }
          }
        }
      }

      return users;
    } catch (Exception e) {
      throw new OrganizationServiceException("Can not load users", e);
    }
  }

  private boolean isNameLike(String userName, String queryName) {
    boolean startWith = false;
    boolean endWith = false;

    if (queryName.startsWith("*")) {
      startWith = true;
      queryName = queryName.substring(1);
    }

    if (queryName.endsWith("*")) {
      endWith = true;
      queryName = queryName.substring(0, queryName.length() - 1);
    }

    if (startWith && endWith) {
      return userName.indexOf(queryName) != -1;
    } else if (startWith) {
      return userName.startsWith(queryName);
    } else if (endWith) {
      return userName.endsWith(queryName);
    } else {
      return userName.equals(queryName);
    }
  }
}

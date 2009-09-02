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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.exoplatform.services.organization.User;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: UserByGroupJCRUserListAccess.java 111 2008-11-11 11:11:11Z $
 */
public class UserByGroupJCRUserListAccess extends JCRUserListAccess {

  /**
   * The groupId.
   */
  private String groupId;

  /**
   * JCRUserListAccess constructor.
   * 
   * @param service
   *          The JCROrganizationService
   * @param groupId
   *          The group identifier
   */
  public UserByGroupJCRUserListAccess(JCROrganizationServiceImpl service, String groupId) {
    super(service);
    this.groupId = groupId;
  }

  /**
   * {@inheritDoc}
   */
  protected int getSize(Session session) throws Exception {
    try {
      Node gNode = (Node) session.getItem(service.getStoragePath() + "/"
          + GroupHandlerImpl.STORAGE_EXO_GROUPS + groupId);

      String statement = "select * from exo:userMembership where exo:group='" + gNode.getUUID()
          + "'";
      Query mquery = session.getWorkspace().getQueryManager().createQuery(statement, Query.SQL);
      QueryResult mres = mquery.execute();

      return (int) mres.getNodes().getSize();
    } catch (PathNotFoundException e) {
      return 0;
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

      Node gNode = (Node) session.getItem(service.getStoragePath() + "/"
          + GroupHandlerImpl.STORAGE_EXO_GROUPS + groupId);

      String statement = "select * from exo:userMembership where exo:group='" + gNode.getUUID()
          + "'";
      Query mquery = session.getWorkspace().getQueryManager().createQuery(statement, Query.SQL);
      QueryResult mres = mquery.execute();

      NodeIterator results = mres.getNodes();

      UserHandlerImpl uHandler = new UserHandlerImpl(service);

      for (int p = 0, counter = 0; counter < length; p++) {
        if (!results.hasNext())
          throw new IllegalArgumentException("Illegal index or length: sum of the index and the length cannot be greater than the list size");

        Node result = results.nextNode();

        if (p >= index) {
          users[counter++] = uHandler.readObjectFromNode(result);
        }
      }

      return users;
    } catch (PathNotFoundException e) {
      return new User[0];
    } catch (Exception e) {
      throw new OrganizationServiceException("Can not load users", e);
    }
  }
}

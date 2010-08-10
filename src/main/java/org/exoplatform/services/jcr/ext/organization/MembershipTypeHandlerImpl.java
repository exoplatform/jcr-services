/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.MembershipTypeHandler;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id$
 */
public class MembershipTypeHandlerImpl extends CommonHandler implements MembershipTypeHandler {

  /**
   * Membership type property that contain description.
   */
  public static final String                 EXO_DESCRIPTION              = "exo:description";

  /**
   * The node to storage membership types.
   */
  public static final String                 STORAGE_EXO_MEMBERSHIP_TYPES = "exo:membershipTypes";

  /**
   * Organization service implementation covering the handler.
   */
  protected final JCROrganizationServiceImpl service;

  /**
   * MembershipTypeHandlerImpl constructor.
   * 
   * @param service
   *          The initialization data
   */
  MembershipTypeHandlerImpl(JCROrganizationServiceImpl service) {
    this.service = service;
  }

  /**
   * Log.
   */
  protected static Log log = ExoLogger.getLogger("jcr.MembershipTypeHandler");

  /**
   * Use this method to persist a new membership type.
   * 
   * @param session
   *          The current session
   * @param mt
   *          The new membership type that the developer want to persist
   * @param broadcast
   *          Broadcast the event if the broadcast value is 'true'
   * @return Return the MembershiptType object that contains the updated informations.
   * @throws Exception
   *           An exception is thrown if the method cannot access the database or a listener fail to
   *           handle the event
   */
  MembershipType createMembershipType(Session session, MembershipType mt, boolean broadcast) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("MembershipType.createMembershipType method is started");
    }

    try {
      Node storagePath = (Node) session.getItem(service.getStoragePath() + "/"
          + STORAGE_EXO_MEMBERSHIP_TYPES);
      Node mtNode = storagePath.addNode(mt.getName());
      writeObjectToNode(mt, mtNode);
      session.save();
      return readObjectFromNode(mtNode);

    } catch (Exception e) {
      throw new OrganizationServiceException("Can not create membership type '" + mt.getName()
          + "'", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  public MembershipType createMembershipType(MembershipType mt, boolean broadcast) throws Exception {
    Session session = service.getStorageSession();
    try {
      return createMembershipType(session, mt, broadcast);
    } finally {
      session.logout();
    }
  }

  /**
   * {@inheritDoc}
   */
  public MembershipType createMembershipTypeInstance() {
    if (log.isDebugEnabled()) {
      log.debug("MembershipType.createMembershipTypeInstance method is started");
    }

    return new MembershipTypeImpl();
  }

  /**
   * Use this method to search for a membership type with the specified name.
   * 
   * @param session
   *          The current Session
   * @param name
   *          the name of the membership type.
   * @return null if no membership type that matched the name or the found membership type.
   * @throws Exception
   *           An exception is thrown if the method cannot access the database or more than one
   *           membership type is found.
   */
  private MembershipType findMembershipType(Session session, String name) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("MembershipType.findMembershipType method is started");
    }

    try {
      Node mtNode = (Node) session.getItem(service.getStoragePath() + "/"
          + STORAGE_EXO_MEMBERSHIP_TYPES + "/" + name);
      return readObjectFromNode(mtNode);

    } catch (PathNotFoundException e) {
      return null;
    } catch (Exception e) {
      throw new OrganizationServiceException("Can not find membership type '" + name + "'", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  public MembershipType findMembershipType(String name) throws Exception {
    Session session = service.getStorageSession();
    try {
      return findMembershipType(session, name);
    } finally {
      session.logout();
    }
  }

  /**
   * Use this method to get all the membership types in the database.
   * 
   * @param session
   *          The current session
   * @return A collection of the membership type. The collection cannot be null. If there is no
   *         membership type in the database, the collection should be empty.
   * @throws Exception
   *           Usually an exception is thrown when the method cannot access the database.
   */
  private Collection findMembershipTypes(Session session) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("MembershipType.findMembershipTypes method is started");
    }

    try {
      List<MembershipType> types = new ArrayList<MembershipType>();

      Node storageNode = (Node) session.getItem(service.getStoragePath() + "/"
          + STORAGE_EXO_MEMBERSHIP_TYPES);
      for (NodeIterator nodes = storageNode.getNodes(); nodes.hasNext();) {
        Node mtNode = nodes.nextNode();
        if (!mtNode.getName().equals("*")) {
          types.add(readObjectFromNode(mtNode));
        }
      }
      return types;

    } catch (Exception e) {
      throw new OrganizationServiceException("Can not find membership types", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  public Collection findMembershipTypes() throws Exception {
    Session session = service.getStorageSession();
    try {
      return findMembershipTypes(session);
    } finally {
      session.logout();
    }
  }

  /**
   * Use this method to remove a membership type.
   * 
   * @param session
   *          The current session
   * @param name
   *          the membership type name
   * @param broadcast
   *          Broadcast the event to the registered listener if the broadcast value is 'true'
   * @return The membership type object which has been removed from the database
   * @throws Exception
   *           An exception is thrown if the method cannot access the database or the membership
   *           type is not found in the database or any listener fail to handle the event.
   */
  private MembershipType removeMembershipType(Session session, String name, boolean broadcast) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("MembershipType.removeMembershipType method is started");
    }

    try {
      Node mtNode = (Node) session.getItem(service.getStoragePath() + "/"
          + STORAGE_EXO_MEMBERSHIP_TYPES + "/" + name);

      // remove membership
      String mStatement = "select * from exo:userMembership where exo:membershipType='"
          + mtNode.getUUID() + "'";
      Query mQuery = session.getWorkspace().getQueryManager().createQuery(mStatement, Query.SQL);
      QueryResult mRes = mQuery.execute();
      for (NodeIterator mNodes = mRes.getNodes(); mNodes.hasNext();) {
        Node mNode = mNodes.nextNode();
        ((MembershipHandlerImpl) service.getMembershipHandler()).removeMembership(session,
                                                                                  mNode.getUUID(),
                                                                                  broadcast);
      }

      // remove membership type
      MembershipType mt = readObjectFromNode(mtNode);
      mtNode.remove();
      session.save();
      return mt;

    } catch (PathNotFoundException e) {
      return null;
    } catch (Exception e) {
      throw new OrganizationServiceException("Can not remove membership type '" + name + "'", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  public MembershipType removeMembershipType(String name, boolean broadcast) throws Exception {
    Session session = service.getStorageSession();
    try {
      return removeMembershipType(session, name, broadcast);
    } finally {
      session.logout();
    }
  }

  /**
   * Use this method to update an existed MembershipType data.
   * 
   * @param session
   *          The current session
   * @param mt
   *          The membership type object to update.
   * @param broadcast
   *          Broadcast the event to all the registered listener if the broadcast value is 'true'
   * @return Return the updated membership type object.
   * @throws Exception
   *           An exception is throwed if the method cannot access the database or any listener fail
   *           to handle the event.
   */
  private MembershipType saveMembershipType(Session session, MembershipType mt, boolean broadcast) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("MembershipType.saveMembershipType method is started");
    }

    try {
      MembershipTypeImpl mtImpl = (MembershipTypeImpl) mt;
      String mtUUID = mtImpl.getUUId() != null
          ? mtImpl.getUUId()
          : ((MembershipTypeImpl) findMembershipType(session, mt.getName())).getUUId();
      Node mtNode = session.getNodeByUUID(mtUUID);

      String srcPath = mtNode.getPath();
      int pos = srcPath.lastIndexOf('/');
      String prevName = srcPath.substring(pos + 1);

      if (!prevName.equals(mt.getName())) {
        String destPath = srcPath.substring(0, pos) + "/" + mt.getName();
        session.move(srcPath, destPath);
        mtNode = (Node) session.getItem(destPath);
      }

      writeObjectToNode(mt, mtNode);
      session.save();
      return readObjectFromNode(mtNode);

    } catch (Exception e) {
      throw new OrganizationServiceException("Can not save membership type '" + mt.getName() + "'",
                                             e);
    }
  }

  /**
   * {@inheritDoc}
   */
  public MembershipType saveMembershipType(MembershipType mt, boolean broadcast) throws Exception {
    Session session = service.getStorageSession();
    try {
      return saveMembershipType(session, mt, broadcast);
    } finally {
      session.logout();
    }
  }

  /**
   * Read membership type properties from the node in the storage.
   * 
   * @param node
   *          The node to read from
   * @return The membership type
   * @throws Exception
   *           An exception is thrown if method can not get access to the database
   */
  private MembershipType readObjectFromNode(Node node) throws Exception {
    try {
      MembershipType mt = new MembershipTypeImpl(node.getUUID());
      mt.setName(node.getName());
      mt.setDescription(readStringProperty(node, EXO_DESCRIPTION));
      return mt;
    } catch (Exception e) {
      throw new OrganizationServiceException("Can not read membership type properties", e);
    }
  }

  /**
   * Write membership type properties to the node.
   * 
   * @param mt
   *          The membership type
   * @param node
   *          The node in the storage
   * @throws Exception
   *           An exception is thrown if method can not get access to the database
   */
  private void writeObjectToNode(MembershipType mt, Node node) throws Exception {
    try {
      node.setProperty(EXO_DESCRIPTION, mt.getDescription());
    } catch (Exception e) {
      throw new OrganizationServiceException("Can not write membership type properties", e);
    }
  }

}

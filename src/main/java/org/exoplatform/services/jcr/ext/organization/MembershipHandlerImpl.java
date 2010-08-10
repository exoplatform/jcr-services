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
import java.util.Collections;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.naming.InvalidNameException;

import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipEventListener;
import org.exoplatform.services.organization.MembershipEventListenerHandler;
import org.exoplatform.services.organization.MembershipHandler;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.User;

/**
 * Created by The eXo Platform SAS. NOTE: Check if nodetypes and/or existing
 * interfaces of API don't relate one to other. Date: 24.07.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter
 *         Nedonosko</a>
 * @version $Id$
 */
public class MembershipHandlerImpl extends CommonHandler implements MembershipHandler, MembershipEventListenerHandler
{

   /**
    * The membership type property that contain reference to linked group.
    */
   public static final String EXO_GROUP = "exo:group";

   /**
    * The membership type property that contain reference to linked membership
    * type.
    */
   public static final String EXO_MEMBERSHIP_TYPE = "exo:membershipType";

   /**
    * The list of listeners to broadcast the events.
    */
   protected final List<MembershipEventListener> listeners = new ArrayList<MembershipEventListener>();

   /**
    * Organization service implementation covering the handler.
    */
   protected final JCROrganizationServiceImpl service;

   /**
    * Log.
    */
   protected static Log log = ExoLogger.getLogger("jcr.MembershipHandlerImpl");

   /**
    * MembershipHandlerImpl constructor.
    * 
    * @param service The initialization data
    */
   MembershipHandlerImpl(JCROrganizationServiceImpl service)
   {
      this.service = service;
   }

   /**
    * {@inheritDoc}
    */
   public void addMembershipEventListener(MembershipEventListener listener)
   {
      listeners.add(listener);
   }

   /**
    * {@inheritDoc}
    */
   public void createMembership(Membership m, boolean broadcast) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         createMembership(session, m, broadcast);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Persist new membership.
    * 
    * @param session The current session
    * @param m The membership
    * @param broadcast Broadcast the event to the registered listeners if the
    *          broadcast event is 'true'
    * @throws Exception An exception is thrown if the method is fail to access
    *           the database or any listener fail to handle the event.
    */
   private void createMembership(Session session, Membership m, boolean broadcast) throws Exception
   {
      try
      {
         if (!session.itemExists(service.getStoragePath() + "/" + UserHandlerImpl.STORAGE_EXO_USERS + "/"
            + m.getUserName()))
         {
            return;
         }

         if (!session.itemExists(service.getStoragePath() + "/" + GroupHandlerImpl.STORAGE_EXO_GROUPS + m.getGroupId()))
         {
            return;
         }

         if (!session.itemExists(service.getStoragePath() + "/"
            + MembershipTypeHandlerImpl.STORAGE_EXO_MEMBERSHIP_TYPES + "/" + m.getMembershipType()))
         {
            return;
         }

         Node uNode =
            (Node)session.getItem(service.getStoragePath() + "/" + UserHandlerImpl.STORAGE_EXO_USERS + "/"
               + m.getUserName());

         Node mNode = uNode.addNode(UserHandlerImpl.EXO_MEMBERSHIP);

         if (broadcast)
         {
            preSave(m, true);
         }

         writeObjectToNode(session, m, mNode);
         session.save();

         if (broadcast)
         {
            postSave(m, true);
         }
      }
      catch (Exception e)
      {
         throw new OrganizationServiceException("Can not create membership record", e);
      }

   }

   /**
    * {@inheritDoc}
    */
   public Membership createMembershipInstance()
   {
      if (log.isDebugEnabled())
      {
         log.debug("createMembershipInstance");
      }

      return new MembershipImpl();
   }

   /**
    * {@inheritDoc}
    */
   public Membership findMembership(String id) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         return findMembership(session, id);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Use this method to search for an membership record with the given id.
    * 
    * @param session The current session
    * @param id The id of the membership
    * @return Return The membership object that matched the id
    * @throws Exception An exception is thrown if the method fail to access the
    *           database or no membership is found.
    */
   private Membership findMembership(Session session, String id) throws Exception
   {
      if (log.isDebugEnabled())
      {
         log.debug("findMembership");
      }

      try
      {
         Node mNode = session.getNodeByUUID(id);
         return readObjectFromNode(session, mNode);

      }
      catch (ItemNotFoundException e)
      {
         return null;
      }
      catch (Exception e)
      {
         throw new OrganizationServiceException("Can not find membership by UUId", e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public Membership findMembershipByUserGroupAndType(String userName, String groupId, String type) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         return findMembershipByUserGroupAndType(session, userName, groupId, type);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Use this method to search for a specific membership type of an user in a
    * group.
    * 
    * @param session The current session
    * @param userName The username of the user.
    * @param groupId The group identifier
    * @param type The membership type
    * @return Null if no such membership record or a membership object.
    * @throws Exception Usually an exception is thrown if the method cannot
    *           access the database
    */
   private Membership findMembershipByUserGroupAndType(Session session, String userName, String groupId, String type)
      throws Exception
   {
      if (log.isDebugEnabled())
      {
         log.debug("findMembershipByUserGroupAndType");
      }

      try
      {
         Membership membership = null;

         String groupUUId = getGroupUUID(session, groupId);
         if (groupUUId != null)
         {

            String membershipTypeUUId = getMembershipTypeUUID(session, type);
            if (membershipTypeUUId != null)
            {

               Node uNode =
                  (Node)session.getItem(service.getStoragePath() + "/" + UserHandlerImpl.STORAGE_EXO_USERS + "/"
                     + userName);
               for (NodeIterator mNodes = uNode.getNodes(UserHandlerImpl.EXO_MEMBERSHIP); mNodes.hasNext();)
               {
                  Node mNode = mNodes.nextNode();

                  if (readStringProperty(mNode, EXO_GROUP).equals(groupUUId)
                     && readStringProperty(mNode, EXO_MEMBERSHIP_TYPE).equals(membershipTypeUUId))
                  {
                     if (membership != null)
                     {
                        throw new OrganizationServiceException("More than one membership is found");
                     }
                     membership = new MembershipImpl(mNode.getUUID(), userName, groupId, type);
                  }
               }
            }
         }

         return membership;

      }
      catch (PathNotFoundException e)
      {
         return null;
      }
      catch (Exception e)
      {
         throw new OrganizationServiceException("Can not find membership type for user '" + userName + "' groupId '"
            + groupId + "' type '" + type + "'", e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public Collection findMembershipsByGroup(Group group) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         return findMembershipsByGroup(session, group);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Use this method to find all the membership in a group. Note that an user
    * can have more than one membership in a group. For example , user admin can
    * have meberhsip 'member' and 'admin' in the group '/users'.
    * 
    * @param session The current session
    * @param group The group
    * @return A collection of the memberships. The collection cannot be none and
    *         empty if no membership is found.
    * @throws Exception An exception is thrown if the method cannot access the
    *           database
    */
   private Collection findMembershipsByGroup(Session session, Group group) throws Exception
   {
      if (log.isDebugEnabled())
      {
         log.debug("findMembershipByGroup");
      }

      try
      {
         List<Membership> types = new ArrayList<Membership>();

         String groupUUID = getGroupUUID(session, group.getId());
         if (groupUUID != null)
         {
            String statement = "select * from exo:userMembership where exo:group='" + groupUUID + "'";
            Query mQuery = session.getWorkspace().getQueryManager().createQuery(statement, Query.SQL);
            QueryResult mRes = mQuery.execute();
            for (NodeIterator mNodes = mRes.getNodes(); mNodes.hasNext();)
            {
               types.add(readObjectFromNode(session, mNodes.nextNode()));
            }
         }
         return types;

      }
      catch (Exception e)
      {
         throw new OrganizationServiceException("Can not find membership by group", e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public Collection findMembershipsByUser(String userName) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         return findMembershipsByUser(session, userName);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Use this method to find all the memberships of an user in any group.
    * 
    * @param session The current session
    * @param userName The user name
    * @return A collection of the membership. The collection cannot be null and
    *         if no membership is found , the collection should be empty
    * @throws Exception Usually an exception is thrown if the method cannot
    *           access the database.
    */
   private Collection findMembershipsByUser(Session session, String userName) throws Exception
   {
      if (log.isDebugEnabled())
      {
         log.debug("findMembeshipByUser");
      }

      List<Membership> types = new ArrayList<Membership>();
      try
      {
         Node uNode =
            (Node)session.getItem(service.getStoragePath() + "/" + UserHandlerImpl.STORAGE_EXO_USERS + "/" + userName);

         // find membership
         for (NodeIterator mNodes = uNode.getNodes(UserHandlerImpl.EXO_MEMBERSHIP); mNodes.hasNext();)
         {
            types.add(readObjectFromNode(session, mNodes.nextNode()));
         }
         return types;

      }
      catch (PathNotFoundException e)
      {
         return types;
      }
      catch (Exception e)
      {
         throw new OrganizationServiceException("Can not find membership by user '" + userName + "'", e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public Collection findMembershipsByUserAndGroup(String userName, String groupId) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         return findMembershipsByUserAndGroup(session, userName, groupId);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Use this method to find all the memberships of an user in a group.
    * 
    * @param session The current session
    * @param userName The user name
    * @param groupId The group id
    * @return A collection of the membership of an user in a group. The
    *         collection cannot be null and the collection should be empty is no
    *         membership is found
    * @throws Exception Usually an exception is thrown if the method cannot
    *           access the database.
    */
   private Collection findMembershipsByUserAndGroup(Session session, String userName, String groupId) throws Exception
   {
      if (log.isDebugEnabled())
      {
         log.debug("findMembershipByUserAndGroup");
      }

      List<Membership> types = new ArrayList<Membership>();
      try
      {
         String groupUUId = getGroupUUID(session, groupId);
         if (groupUUId != null)
         {
            Node uNode =
               (Node)session.getItem(service.getStoragePath() + "/" + UserHandlerImpl.STORAGE_EXO_USERS + "/"
                  + userName);

            for (NodeIterator mNodes = uNode.getNodes(UserHandlerImpl.EXO_MEMBERSHIP); mNodes.hasNext();)
            {
               Node mNode = mNodes.nextNode();

               // check group and add
               if (readStringProperty(mNode, EXO_GROUP).equals(groupUUId))
               {
                  types.add(readObjectFromNode(session, mNode));
               }
            }
         }

         return types;

      }
      catch (PathNotFoundException e)
      {
         return types;
      }
      catch (Exception e)
      {
         throw new OrganizationServiceException("Can not find membership by user '" + userName + "' and group '"
            + groupId + "'", e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void linkMembership(User user, Group group, MembershipType m, boolean broadcast) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         linkMembership(session, user, group, m, broadcast);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Use this method to create a membership record, a relation of the user ,
    * group and membership type.
    * 
    * @param session The current session
    * @param user The user of the membership
    * @param group The group of the membership
    * @param m The MembershipType of the membership
    * @param broadcast Broadcast the event if the value of the broadcast is
    *          'true'
    * @throws Exception An exception is thrown if the method is fail to access
    *           the database, a membership record with the same user , group and
    *           membership type existed or any listener fail to handle the event.
    */
   private void linkMembership(Session session, User user, Group group, MembershipType m, boolean broadcast)
      throws Exception
   {

      try
      {
         if (group == null)
         {
            throw new InvalidNameException("Can not create membership record for " + user.getUserName()
               + " because group is null");
         }

         if (m == null)
         {
            throw new InvalidNameException("Can not create membership record for " + user.getUserName()
               + " because membership type is null");
         }

         Membership membership = new MembershipImpl(null, user.getUserName(), group.getId(), m.getName());
         createMembership(session, membership, broadcast);
      }
      catch (Exception e)
      {
         throw new OrganizationServiceException("Can not link membership for user '" + user.getUserName(), e);
      }
   }

   /**
    * Use this method to remove a membership. Usually you need to call the method
    * findMembershipByUserGroupAndType(..) to find the membership and remove.
    * 
    * @param session The current session
    * @param id The id of the membership
    * @param broadcast Broadcast the event to the registered listeners if the
    *          broadcast event is 'true'
    * @return The membership object which has been removed from the database
    * @throws Exception An exception is throwed if the method cannot access the
    *           database or any listener fail to handle the event.
    */
   Membership removeMembership(Session session, String id, boolean broadcast) throws Exception
   {
      if (log.isDebugEnabled())
      {
         log.debug("removeMembership");
      }

      try
      {
         Node mNode = session.getNodeByUUID(id);
         Membership membership = readObjectFromNode(session, mNode);

         if (broadcast)
         {
            preDelete(membership);
         }

         mNode.remove();
         session.save();

         if (broadcast)
         {
            postDelete(membership);
         }

         return membership;

      }
      catch (ItemNotFoundException e)
      {
         return null;
      }
      catch (Exception e)
      {
         throw new OrganizationServiceException("Can not remove membership", e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public Membership removeMembership(String id, boolean broadcast) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         return removeMembership(session, id, broadcast);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * {@inheritDoc}
    */
   public Collection removeMembershipByUser(String userName, boolean broadcast) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         return removeMembershipByUser(session, userName, broadcast);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Use this method to remove all user's membership.
    * 
    * @param session The current session
    * @param userName The username which user object need remove memberships
    * @param broadcast Broadcast the event to the registered listeners if the
    *          broadcast event is 'true'
    * @return The membership object which has been removed from the database
    * @throws Exception An exception is thrown if the method cannot access the
    *           database or any listener fail to handle the event.
    */
   private Collection removeMembershipByUser(Session session, String userName, boolean broadcast) throws Exception
   {
      if (log.isDebugEnabled())
      {
         log.debug("removeMembershipByUser");
      }

      List<Membership> types = new ArrayList<Membership>();
      try
      {
         Node uNode =
            (Node)session.getItem(service.getStoragePath() + "/" + UserHandlerImpl.STORAGE_EXO_USERS + "/" + userName);

         for (NodeIterator mNodes = uNode.getNodes(UserHandlerImpl.EXO_MEMBERSHIP); mNodes.hasNext();)
         {
            Node mNode = mNodes.nextNode();
            Membership membership = readObjectFromNode(session, mNode);
            types.add(membership);

            if (broadcast)
            {
               preDelete(membership);
            }

            mNode.remove();
         }

         session.save();
         for (int i = 0; i < types.size(); i++)
         {
            if (broadcast)
            {
               postDelete(types.get(i));
            }
         }

         return types;

      }
      catch (PathNotFoundException e)
      {
         return types;
      }
      catch (Exception e)
      {
         throw new OrganizationServiceException("Can not remove membership by user '" + userName + "'", e);
      }
   }

   /**
    * Remove registered listener.
    * 
    * @param listener The registered listener
    */
   public void removeMembershipEventListener(MembershipEventListener listener)
   {
      listeners.remove(listener);
   }

   /**
    * Get membership type UUID by the name.
    * 
    * @param session The Session
    * @param type The membership type
    * @return The membership type UUId in the storage
    * @throws Exception An exception is thrown if the method cannot access the
    *           database
    */
   private String getMembershipTypeUUID(Session session, String type) throws Exception
   {
      try
      {
         String mtPath = service.getStoragePath() + "/" + MembershipTypeHandlerImpl.STORAGE_EXO_MEMBERSHIP_TYPES;
         return (type != null && type.length() != 0 && session.itemExists(mtPath + "/" + type) ? ((Node)session
            .getItem(mtPath + "/" + type)).getUUID() : null);

      }
      catch (Exception e)
      {
         throw new OrganizationServiceException("Can not find membership type '" + type + "'", e);
      }
   }

   /**
    * Get group UUID from the name of the group.
    * 
    * @param session The Session
    * @param groupId The name of the group
    * @return The group UUId of the group and null if group does not exist
    * @throws Exception An exception is thrown if the method cannot access the
    *           database
    */
   private String getGroupUUID(Session session, String groupId) throws Exception
   {
      try
      {
         String gPath = service.getStoragePath() + "/" + GroupHandlerImpl.STORAGE_EXO_GROUPS;
         return (groupId != null && groupId.length() != 0 && session.itemExists(gPath + groupId) ? ((Node)session
            .getItem(gPath + groupId)).getUUID() : null);

      }
      catch (Exception e)
      {
         throw new OrganizationServiceException("Can not find group '" + groupId + "'", e);
      }
   }

   /**
    * Get membership type name from the UUID.
    * 
    * @param session The Session
    * @param UUID The UUID of the group in the storage
    * @return The membership type name
    * @throws Exception An exception is thrown if the method cannot access the
    *           database
    */
   private String getMembershipType(Session session, String UUID) throws Exception
   {
      try
      {
         return session.getNodeByUUID(UUID).getName();
      }
      catch (Exception e)
      {
         throw new OrganizationServiceException("Can not find membership type by uuid " + UUID, e);
      }
   }

   /**
    * Get groupId from the UUId.
    * 
    * @param session The Session
    * @param UUID The UUID of the group in the storage
    * @return The groupId of the group
    * @throws Exception An exception is thrown if the method cannot access the
    *           database
    */
   private String getGroupId(Session session, String UUID) throws Exception
   {
      try
      {
         Node gNode = session.getNodeByUUID(UUID);
         return readStringProperty(gNode, GroupHandlerImpl.EXO_GROUP_ID);
      }
      catch (Exception e)
      {
         throw new OrganizationServiceException("Can not find group by uuid " + UUID, e);
      }
   }

   /**
    * Create membership type '*' if not exist.
    * 
    * @param session The current session
    * @param name The membership type name
    * @param broadcast Broadcast the event to the registered listeners if the
    *          broadcast event is 'true'
    * @throws Exception An exception is thrown if the method is fail to access
    *           the database or any listener fail to handle the event.
    */
   private void createAnyMembershipType(Session session, String name, boolean broadcast) throws Exception
   {
      if (name.equals("*")
         && !session.itemExists(service.getStoragePath() + "/" + MembershipTypeHandlerImpl.STORAGE_EXO_MEMBERSHIP_TYPES
            + "/" + name))
      {

         MembershipType m = service.getMembershipTypeHandler().createMembershipTypeInstance();
         m.setName("*");
         m.setDescription("any membership type");
         ((MembershipTypeHandlerImpl)service.getMembershipTypeHandler()).createMembershipType(session, m, broadcast);
      }
   }

   /**
    * Read membership properties from the node in the storage.
    * 
    * @param session The Session
    * @param node The node to read from
    * @return The membership
    * @throws Exception An Exception is thrown if method can not get access to
    *           the database
    */
   private Membership readObjectFromNode(Session session, Node node) throws Exception
   {
      try
      {
         String groupUUID = readStringProperty(node, EXO_GROUP);
         String membershipTypeUUID = readStringProperty(node, EXO_MEMBERSHIP_TYPE);

         String groupId = getGroupId(session, groupUUID);
         String membershipType = getMembershipType(session, membershipTypeUUID);
         String userName = node.getParent().getName();

         return new MembershipImpl(node.getUUID(), userName, groupId, membershipType);
      }
      catch (Exception e)
      {
         throw new OrganizationServiceException("Can not read membership properties", e);
      }
   }

   /**
    * Write membership properties to the node in the storage.
    * 
    * @param session The Session
    * @param m The membership
    * @param node The node to write in
    * @throws Exception An Exception is thrown if method can not get access to
    *           the database
    */
   private void writeObjectToNode(Session session, Membership m, Node node) throws Exception
   {
      try
      {
         String groupUUId = getGroupUUID(session, m.getGroupId());
         String membershipTypeUUId = getMembershipTypeUUID(session, m.getMembershipType());

         node.setProperty(EXO_GROUP, groupUUId);
         node.setProperty(EXO_MEMBERSHIP_TYPE, membershipTypeUUId);

      }
      catch (Exception e)
      {
         throw new OrganizationServiceException("Can not write membership properties", e);
      }
   }

   /**
    * PreSave event.
    * 
    * @param membership The membership to save
    * @param isNew Is it new membership or not
    * @throws Exception If listeners fail to handle the user event
    */
   private void preSave(Membership membership, boolean isNew) throws Exception
   {
      for (int i = 0; i < listeners.size(); i++)
      {
         MembershipEventListener listener = listeners.get(i);
         listener.preSave(membership, isNew);
      }
   }

   /**
    * PostSave event.
    * 
    * @param membership The membership to save
    * @param isNew Is it new membership or not
    * @throws Exception If listeners fail to handle the user event
    */
   private void postSave(Membership membership, boolean isNew) throws Exception
   {
      for (int i = 0; i < listeners.size(); i++)
      {
         MembershipEventListener listener = listeners.get(i);
         listener.postSave(membership, isNew);
      }
   }

   /**
    * PreDelete event.
    * 
    * @param membership The membership to delete
    * @throws Exception If listeners fail to handle the user event
    */
   private void preDelete(Membership membership) throws Exception
   {
      for (int i = 0; i < listeners.size(); i++)
      {
         MembershipEventListener listener = listeners.get(i);
         listener.preDelete(membership);
      }
   }

   /**
    * PostDelete event.
    * 
    * @param membership The membership to delete
    * @throws Exception If listeners fail to handle the user event
    */
   private void postDelete(Membership membership) throws Exception
   {
      for (int i = 0; i < listeners.size(); i++)
      {
         MembershipEventListener listener = listeners.get(i);
         listener.postDelete(membership);
      }
   }

   /**
    * {@inheritDoc}
    */
   public List<MembershipEventListener> getMembershipListeners()
   {
      return Collections.unmodifiableList(listeners);
   }
}

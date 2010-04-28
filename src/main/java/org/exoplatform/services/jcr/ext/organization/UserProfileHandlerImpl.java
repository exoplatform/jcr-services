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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Session;

import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.organization.UserProfileEventListener;
import org.exoplatform.services.organization.UserProfileEventListenerHandler;
import org.exoplatform.services.organization.UserProfileHandler;

/**
 * Created by The eXo Platform SAS Date: 24.07.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter
 *         Nedonosko</a>
 * @version $Id: UserProfileHandlerImpl.java 33732 2009-07-08 15:00:43Z
 *          pnedonosko $
 */
public class UserProfileHandlerImpl extends CommonHandler implements UserProfileHandler,
   UserProfileEventListenerHandler
{

   /**
    * The child not to storage users profile properties.
    */
   public static final String EXO_ATTRIBUTES = "exo:attributes";

   /**
    * The list of listeners to broadcast events.
    */
   protected final List<UserProfileEventListener> listeners = new ArrayList<UserProfileEventListener>();

   /**
    * Organization service implementation covering the handler.
    */
   protected final JCROrganizationServiceImpl service;

   /**
    * Log.
    */
   protected static Log log = ExoLogger.getLogger("jcr.UserProfileHandlerImpl");

   /**
    * UserProfileHandlerImpl constructor.
    * 
    * @param service The initialization data
    */
   UserProfileHandlerImpl(JCROrganizationServiceImpl service)
   {
      this.service = service;
   }

   /**
    * {@inheritDoc}
    */
   public void addUserProfileEventListener(UserProfileEventListener listener)
   {
      listeners.add(listener);
   }

   /**
    * {@inheritDoc}
    */
   public UserProfile createUserProfileInstance()
   {
      if (log.isDebugEnabled())
      {
         log.debug("UserProfile.createUserProfileInstance() method is started");
      }

      return new UserProfileImpl();
   }

   /**
    * {@inheritDoc}
    */
   public UserProfile createUserProfileInstance(String userName)
   {
      if (log.isDebugEnabled())
      {
         log.debug("UserProfile.createUserProfileInstance(String) method is started");
      }

      return new UserProfileImpl(userName);
   }

   /**
    * {@inheritDoc}
    */
   public UserProfile findUserProfileByName(String userName) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         return findUserProfileByName(session, userName);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * This method should search for and return UserProfile record according to
    * the username.
    * 
    * @param session The current session
    * @param userName The user name
    * @return return null if no record match the userName. return an UserProfile
    *         instance if a record match the username.
    * @throws Exception Throw Exception if the method fail to access the database
    *           or find more than one record that match the username.
    */
   private UserProfile findUserProfileByName(Session session, String userName) throws Exception
   {
      if (log.isDebugEnabled())
      {
         log.debug("UserProfile.findUserProfileByName method is started");
      }

      try
      {
         return readUserProfile(session, userName);
      }
      catch (Exception e)
      {
         throw new OrganizationServiceException("Can not find '" + userName + "' profile", e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public Collection findUserProfiles() throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         return findUserProfiles(session);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Find and return all the UserProfile record in the database.
    * 
    * @param session The current session
    * @return The collection of user profiles
    * @throws Exception Throw exception if the method fail to access the database
    */
   private Collection findUserProfiles(Session session) throws Exception
   {
      if (log.isDebugEnabled())
      {
         log.debug("UserProfile.findUserProfiles method is started");
      }

      try
      {
         List<UserProfile> types = new ArrayList<UserProfile>();

         Node storagePath = (Node)session.getItem(service.getStoragePath() + "/" + UserHandlerImpl.STORAGE_EXO_USERS);
         for (NodeIterator nodes = storagePath.getNodes(); nodes.hasNext();)
         {
            UserProfile userProfile = readUserProfile(session, nodes.nextNode().getName());
            if (userProfile != null)
            {
               types.add(userProfile);
            }
         }
         return types;

      }
      catch (Exception e)
      {
         throw new OrganizationServiceException("Can not find user profiles", e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public UserProfile removeUserProfile(String userName, boolean broadcast) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         return removeUserProfile(session, userName, broadcast);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * This method should remove the user profile record in the database.
    * 
    * @param session The current session
    * @param userName The user profile record with the username should be removed
    *          from the database
    * @param broadcast Broadcast the event the listeners if broadcast is true.
    * @return The UserProfile instance that has been removed.
    * @throws Exception Throw exception if the method fail to remove the record
    *           or any listener fail to handle the event
    */
   private UserProfile removeUserProfile(Session session, String userName, boolean broadcast) throws Exception
   {
      if (log.isDebugEnabled())
      {
         log.debug("UserProfile.removeUserProfile method is started");
      }

      try
      {
         Node profileNode =
            (Node)session.getItem(service.getStoragePath() + "/" + UserHandlerImpl.STORAGE_EXO_USERS + "/" + userName
               + "/" + UserHandlerImpl.EXO_PROFILE);
         UserProfile userProfile = readObjectFromNode(session, profileNode.getNode(EXO_ATTRIBUTES));

         if (broadcast)
         {
            preDelete(userProfile);
         }

         profileNode.remove();
         session.save();

         if (broadcast)
         {
            postDelete(userProfile);
         }

         return userProfile;

      }
      catch (PathNotFoundException e)
      {
         return null;
      }
      catch (Exception e)
      {
         throw new OrganizationServiceException("Can not remove '" + userName + "' profile", e);
      }
   }

   /**
    * Remove registered listener.
    * 
    * @param listener The registered listener for removing
    */
   public void removeUserProfileEventListener(UserProfileEventListener listener)
   {
      listeners.remove(listener);
   }

   /**
    * {@inheritDoc}
    */
   public void saveUserProfile(UserProfile profile, boolean broadcast) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         saveUserProfile(session, profile, broadcast);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * This method should persist the profile instance to the database. If the
    * profile is not existed yet. the method should create a new user profile
    * record. If there is an existed record. The method should merge the data
    * with the existed record.
    * 
    * @param session The current session
    * @param profile the profile instance to persist.
    * @param broadcast broadcast the event to the listener if broadcast is true
    * @throws Exception throw exception if the method fail to access the database
    *           or any listener fail to handle the event.
    */
   private void saveUserProfile(Session session, UserProfile profile, boolean broadcast) throws Exception
   {
      if (log.isDebugEnabled())
      {
         log.debug("UserProfile.saveUserProfile method is started");
      }

      try
      {
         Node uNode =
            (Node)session.getItem(service.getStoragePath() + "/" + UserHandlerImpl.STORAGE_EXO_USERS + "/"
               + profile.getUserName());

         if (!session.itemExists(uNode.getPath() + "/" + UserHandlerImpl.EXO_PROFILE))
         {
            uNode.addNode(UserHandlerImpl.EXO_PROFILE);
         }
         Node profileNode = uNode.getNode(UserHandlerImpl.EXO_PROFILE);

         if (!session.itemExists(profileNode.getPath() + "/" + EXO_ATTRIBUTES))
         {
            profileNode.addNode(EXO_ATTRIBUTES);
         }
         Node attrNode = profileNode.getNode(EXO_ATTRIBUTES);

         if (broadcast)
         {
            preSave(profile, false);
         }

         for (String key : profile.getUserInfoMap().keySet())
         {
            attrNode.setProperty(key, profile.getAttribute(key));
         }

         session.save();

         if (broadcast)
         {
            postSave(profile, false);
         }

      }
      catch (PathNotFoundException e)
      {
         return;
      }
      catch (Exception e)
      {
         throw new OrganizationServiceException("Can not save '" + profile.getUserName() + "' profile", e);
      }
   }

   /**
    * Read user profile data for specific user.
    * 
    * @param session The current session
    * @param userName The user name
    * @return The user profile data or null if profile does not exist
    * @throws Exception An exception is thrown if method can not get access to
    *           the database
    */
   private UserProfile readUserProfile(Session session, String userName) throws Exception
   {
      try
      {
         Node attrNode =
            (Node)session.getItem(service.getStoragePath() + "/" + UserHandlerImpl.STORAGE_EXO_USERS + "/" + userName
               + "/" + UserHandlerImpl.EXO_PROFILE + "/" + EXO_ATTRIBUTES);

         return readObjectFromNode(session, attrNode);

      }
      catch (PathNotFoundException e)
      {
         return null;
      }
      catch (Exception e)
      {
         throw new OrganizationServiceException("Can not read user profile data", e);
      }
   }

   /**
    * Read user profile from the node in the storage.
    * 
    * @param session The current session
    * @param node The node to read from
    * @return The user profile data
    * @throws Exception An exception is thrown if method can not get access to
    *           the database
    */
   private UserProfile readObjectFromNode(Session session, Node node) throws Exception
   {
      try
      {
         UserProfile userProfile = new UserProfileImpl(node.getParent().getParent().getName());
         for (PropertyIterator props = node.getProperties(); props.hasNext();)
         {
            Property prop = props.nextProperty();

            // ignore system properties
            if (!(prop.getName()).startsWith("jcr:") && !(prop.getName()).startsWith("exo:"))
            {
               userProfile.setAttribute(prop.getName(), prop.getString());
            }
         }
         return userProfile;

      }
      catch (Exception e)
      {
         throw new OrganizationServiceException("Can not read user profile data from node", e);
      }
   }

   /**
    * PreSave event.
    * 
    * @param userProfile The userProfile to save
    * @param isNew Is it new profile or not
    * @throws Exception If listeners fail to handle the user event
    */
   private void preSave(UserProfile userProfile, boolean isNew) throws Exception
   {
      for (UserProfileEventListener listener : listeners)
         listener.preSave(userProfile, isNew);
   }

   /**
    * PostSave event.
    * 
    * @param userProfile The user profile to save
    * @param isNew Is it new profile or not
    * @throws Exception If listeners fail to handle the user event
    */
   private void postSave(UserProfile userProfile, boolean isNew) throws Exception
   {
      for (UserProfileEventListener listener : listeners)
         listener.postSave(userProfile, isNew);
   }

   /**
    * PreDelete event.
    * 
    * @param userProfile The user profile to delete
    * @throws Exception If listeners fail to handle the user event
    */
   private void preDelete(UserProfile userProfile) throws Exception
   {
      for (UserProfileEventListener listener : listeners)
         listener.preDelete(userProfile);
   }

   /**
    * PostDelete event.
    * 
    * @param userProfile The user profile to delete
    * @throws Exception If listeners fail to handle the user event
    */
   private void postDelete(UserProfile userProfile) throws Exception
   {
      for (UserProfileEventListener listener : listeners)
         listener.postDelete(userProfile);
   }

   /**
    * {@inheritDoc}
    */
   public List<UserProfileEventListener> getUserProfileListeners()
   {
      return Collections.unmodifiableList(listeners);
   }
}

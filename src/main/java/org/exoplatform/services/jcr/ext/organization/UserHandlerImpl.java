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
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;

import org.exoplatform.services.log.Log;
import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.organization.Query;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;
import org.exoplatform.services.organization.UserHandler;

/**
 * Created by The eXo Platform SAS.
 * 
 * Date: 24.07.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id$
 */
public class UserHandlerImpl extends CommonHandler implements UserHandler {

  /**
   * The user property that contain the date of creation.
   */
  public static final String                 EXO_CREATED_DATE    = "exo:createdDate";

  /**
   * The user property that contain email.
   */
  public static final String                 EXO_EMAIL           = "exo:email";

  /**
   * The user property that contain fist name.
   */
  public static final String                 EXO_FIRST_NAME      = "exo:firstName";

  /**
   * The user property that contain last login time.
   */
  public static final String                 EXO_LAST_LOGIN_TIME = "exo:lastLoginTime";

  /**
   * The user property that contain last name.
   */
  public static final String                 EXO_LAST_NAME       = "exo:lastName";

  /**
   * The child node to storage membership properties.
   */
  public static final String                 EXO_MEMBERSHIP      = "exo:membership";

  /**
   * The user property that contain password.
   */
  public static final String                 EXO_PASSWORD        = "exo:password";

  /**
   * The child node to storage user addition information.
   */
  public static final String                 EXO_PROFILE         = "exo:profile";

  /**
   * The node to storage users.
   */
  public static final String                 STORAGE_EXO_USERS   = "exo:users";

  /**
   * The list of listeners to broadcast the events.
   */
  protected final List<UserEventListener>    listeners           = new ArrayList<UserEventListener>();

  /**
   * Organization service implementation covering the handler.
   */
  protected final JCROrganizationServiceImpl service;

  /**
   * Log.
   */
  protected static Log                       log                 = ExoLogger.getLogger("jcr.UserHandlerImpl");

  /**
   * UserHandlerImpl constructor.
   * 
   * @param service
   *          The initialization data.
   */
  UserHandlerImpl(JCROrganizationServiceImpl service) {
    this.service = service;
  }

  /**
   * {@inheritDoc}
   */
  public void addUserEventListener(UserEventListener listener) {
    listeners.add(listener);
  }

  /**
   * {@inheritDoc}
   */
  public boolean authenticate(String username, String password) throws Exception {
    Session session = service.getStorageSession();
    try {
      return authenticate(session, username, password);
    } finally {
      session.logout();
    }
  }

  /**
   * Check if the username and the password of an user is valid.
   * 
   * @param session
   *          The current session
   * @param username
   *          The user name
   * @param password
   *          The password
   * @return return true if the username and the password is match with an user record in the
   *         database, else return false.
   * @throws Exception
   *           throw an exception if cannot access the database
   */
  private boolean authenticate(Session session, String username, String password) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("User.authenticate method is started");
    }

    try {
      Node uNode = (Node) session.getItem(service.getStoragePath() + "/" + STORAGE_EXO_USERS + "/"
          + username);
      boolean authenticated = readStringProperty(uNode, EXO_PASSWORD).equals(password);
      if (authenticated) {
        uNode.setProperty(EXO_LAST_LOGIN_TIME, Calendar.getInstance());
      }
      return authenticated;

    } catch (PathNotFoundException e) {
      return false;
    } catch (Exception e) {
      throw new OrganizationServiceException("Can not authenticate user '" + username + "'", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void createUser(User user, boolean broadcast) throws Exception {
    Session session = service.getStorageSession();
    try {
      createUser(session, user, broadcast);
    } finally {
      session.logout();
    }
  }

  /**
   * This method is used to persist a new user object.
   * 
   * @param session
   *          The current session
   * @param user
   *          The user object to save
   * @param broadcast
   *          If the broadcast value is true , then the UserHandler should broadcast the event to
   *          all the listener that register with the organization service.
   * @throws Exception
   *           The exception can be thrown if the the UserHandler cannot persist the user object or
   *           any listeners fail to handle the user event.
   */
  private void createUser(Session session, User user, boolean broadcast) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("User.createUser method is started");
    }

    try {
      Node storageNode = (Node) session.getItem(service.getStoragePath() + "/" + STORAGE_EXO_USERS);
      Node uNode = storageNode.addNode(user.getUserName());

      // set default value for createdDate
      if (user.getCreatedDate() == null) {
        Calendar calendar = Calendar.getInstance();
        user.setCreatedDate(calendar.getTime());
      }

      if (broadcast) {
        preSave(user, true);
      }

      writeObjectToNode(user, uNode);
      session.save();

      if (broadcast) {
        postSave(user, true);
      }

    } catch (Exception e) {
      throw new OrganizationServiceException("Can not create user '" + user.getUserName() + "'", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  public User createUserInstance() {
    if (log.isDebugEnabled()) {
      log.debug("User.createUserInstance() method is started");
    }

    return new UserImpl();
  }

  /**
   * {@inheritDoc}
   */
  public User createUserInstance(String username) {
    if (log.isDebugEnabled()) {
      log.debug("User.createUserInstance(String) method is started");
    }

    return new UserImpl(username);
  }

  /**
   * Find user by specific name.
   * 
   * 
   * @param session
   *          The current session
   * @param userName
   *          the user that the user handler should search for
   * @return The method return null if there no user matches the given username. The method return
   *         an User object if an user that match the username.
   * @throws Exception
   *           The exception is thrown if the method fail to access the user database or more than
   *           one user object with the same username is found
   */
  User findUserByName(Session session, String userName) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("User.findUserByName method is started");
    }

    try {
      Node uNode = (Node) session.getItem(service.getStoragePath() + "/" + STORAGE_EXO_USERS + "/"
          + userName);
      return readObjectFromNode(uNode);

    } catch (PathNotFoundException e) {
      return null;
    } catch (Exception e) {
      throw new OrganizationServiceException("Can not find user '" + userName + "'", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  public User findUserByName(String userName) throws Exception {
    Session session = service.getStorageSession();
    try {
      return findUserByName(session, userName);
    } finally {
      session.logout();
    }
  }

  /**
   * {@inheritDoc}
   */
  public LazyPageList findUsers(org.exoplatform.services.organization.Query query) throws Exception {
    return new LazyPageList(new UserByQueryJCRUserListAccess(service, query), 10);
  }

  /**
   * {@inheritDoc}
   */
  public LazyPageList findUsersByGroup(String groupId) throws Exception {
    return new LazyPageList(new UserByGroupJCRUserListAccess(service, groupId), 10);
  }

  /**
   * {@inheritDoc}
   */
  public LazyPageList getUserPageList(int pageSize) throws Exception {
    return new LazyPageList(new SimpleJCRUserListAccess(service), pageSize);
  }

  /**
   * {@inheritDoc}
   */
  public ListAccess<User> findAllUsers() throws Exception {
    return new SimpleJCRUserListAccess(service);
  }

  /**
   * {@inheritDoc}
   */
  public ListAccess<User> findUsersByGroupId(String groupId) throws Exception {
    return new UserByGroupJCRUserListAccess(service, groupId);
  }

  /**
   * {@inheritDoc}
   */
  public ListAccess<User> findUsersByQuery(Query query) throws Exception {
    return new UserByQueryJCRUserListAccess(service, query);
  }

  /**
   * {@inheritDoc}
   */
  public User removeUser(String userName, boolean broadcast) throws Exception {
    Session session = service.getStorageSession();
    try {
      return removeUser(session, userName, broadcast);
    } finally {
      session.logout();
    }
  }

  /**
   * Remove an user and broadcast the event to all the registered listener. When the user is removed
   * , the user profile and all the membership of the user should be removed as well.
   * 
   * @param session
   *          The current session
   * @param userName
   *          The user should be removed from the user database
   * @param broadcast
   *          If broadcast is true, the the delete user event should be broadcasted to all
   *          registered listener
   * @return return the User object after that user has beed removed from database
   * @throws Exception
   *           If method can not get access to the database or any listeners fail to handle the user
   *           event.
   */
  private User removeUser(Session session, String userName, boolean broadcast) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("User.removeUser method is started");
    }

    try {
      Node uNode = (Node) session.getItem(service.getStoragePath() + "/" + STORAGE_EXO_USERS + "/"
          + userName);
      User user = readObjectFromNode(uNode);

      if (broadcast) {
        preDelete(user);
      }

      uNode.remove();
      session.save();

      if (broadcast) {
        postDelete(user);
      }

      return user;

    } catch (PathNotFoundException e) {
      return null;
    } catch (Exception e) {
      throw new OrganizationServiceException("Can not remove user '" + userName + "'", e);
    }
  }

  /**
   * Remove registered listener.
   * 
   * @param listener
   *          The registered listener for remove
   */
  public void removeUserEventListener(UserEventListener listener) {
    listeners.remove(listener);
  }

  /**
   * {@inheritDoc}
   */
  public void saveUser(User user, boolean broadcast) throws Exception {
    Session session = service.getStorageSession();
    try {
      saveUser(session, user, broadcast);
    } finally {
      session.logout();
    }
  }

  /**
   * This method is used to update an existing User object.
   * 
   * @param session
   *          The current session
   * @param user
   *          The user object to update
   * @param broadcast
   *          If the broadcast is true , then all the user event listener that register with the
   *          organization service will be called
   * @throws Exception
   *           The exception can be thrown if the the UserHandler cannot save the user object or any
   *           listeners fail to handle the user event.
   */
  private void saveUser(Session session, User user, boolean broadcast) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("User.saveUser method is started");
    }

    try {
      UserImpl userImpl = (UserImpl) user;
      String userUUID = userImpl.getUUId() != null
          ? userImpl.getUUId()
          : ((UserImpl) findUserByName(session, user.getUserName())).getUUId();
      Node uNode = session.getNodeByUUID(userUUID);

      String srcPath = uNode.getPath();
      int pos = srcPath.lastIndexOf('/');
      String prevName = srcPath.substring(pos + 1);

      if (!prevName.equals(user.getUserName())) {
        String destPath = srcPath.substring(0, pos) + "/" + user.getUserName();
        session.move(srcPath, destPath);
        uNode = (Node) session.getItem(destPath);
      }

      if (broadcast) {
        preSave(user, false);
      }

      writeObjectToNode(user, uNode);
      session.save();

      if (broadcast) {
        postSave(user, false);
      }

    } catch (Exception e) {
      throw new OrganizationServiceException("Can not save user '" + user.getUserName() + "'", e);
    }
  }

  /**
   * Read user properties from the node in the storage.
   * 
   * @param node
   *          The node to read from
   * @return The user
   * @throws Exception
   *           An exception is thrown if method can not get access to the database
   */
  public User readObjectFromNode(Node node) throws Exception {
    try {
      User user = new UserImpl(node.getName(), node.getUUID());
      user.setCreatedDate(readDateProperty(node, EXO_CREATED_DATE));
      user.setLastLoginTime(readDateProperty(node, EXO_LAST_LOGIN_TIME));
      user.setEmail(readStringProperty(node, EXO_EMAIL));
      user.setPassword(readStringProperty(node, EXO_PASSWORD));
      user.setFirstName(readStringProperty(node, EXO_FIRST_NAME));
      user.setLastName(readStringProperty(node, EXO_LAST_NAME));
      return user;
    } catch (Exception e) {
      throw new OrganizationServiceException("Can not read user properties", e);
    }
  }

  /**
   * Write user properties to the node.
   * 
   * @param user
   *          The user
   * @param node
   *          The node in the storage
   * @throws Exception
   *           An exception is thrown if method can not get access to the database
   */
  private void writeObjectToNode(User user, Node node) throws Exception {
    try {
      Calendar calendar = null;
      node.setProperty(EXO_EMAIL, user.getEmail());
      node.setProperty(EXO_FIRST_NAME, user.getFirstName());
      node.setProperty(EXO_LAST_NAME, user.getLastName());
      node.setProperty(EXO_PASSWORD, user.getPassword());

      if (user.getLastLoginTime() == null) {
        node.setProperty(EXO_LAST_LOGIN_TIME, calendar);
      } else {
        calendar = Calendar.getInstance();
        calendar.setTime(user.getLastLoginTime());
        node.setProperty(EXO_LAST_LOGIN_TIME, calendar);
      }

      calendar = Calendar.getInstance();
      calendar.setTime(user.getCreatedDate());
      node.setProperty(EXO_CREATED_DATE, calendar);

    } catch (Exception e) {
      throw new OrganizationServiceException("Can not write user properties", e);
    }
  }

  /**
   * PreSave Event.
   * 
   * @param user
   *          The user to save
   * @param isNew
   *          It is new user or not
   * @throws Exception
   *           If listeners fail to handle the user event
   */
  private void preSave(User user, boolean isNew) throws Exception {
    for (UserEventListener listener : listeners)
      listener.preSave(user, isNew);
  }

  /**
   * PostSave Event.
   * 
   * @param user
   *          The user to save
   * @param isNew
   *          It is new user or not
   * @throws Exception
   *           If listeners fail to handle the user event
   */
  private void postSave(User user, boolean isNew) throws Exception {
    for (UserEventListener listener : listeners)
      listener.postSave(user, isNew);
  }

  /**
   * PreDelete Event.
   * 
   * @param user
   *          The user to delete
   * @throws Exception
   *           If listeners fail to handle the user event
   */
  private void preDelete(User user) throws Exception {
    for (UserEventListener listener : listeners)
      listener.preDelete(user);
  }

  /**
   * PostDelete Event.
   * 
   * @param user
   *          The user to delete
   * @throws Exception
   *           If listeners fail to handle the user event
   */
  private void postDelete(User user) throws Exception {
    for (UserEventListener listener : listeners)
      listener.postDelete(user);
  }

  /**
   * RemoveAsterix remove char '*' from start and end if string starts and ends with '*'.
   * 
   * @param str
   *          String to remove char
   * @return String with removed chars or the same string
   */
  private String removeAsterix(String str) {
    if (str.startsWith("*")) {
      str = str.substring(1);
    }
    if (str.endsWith("*")) {
      str = str.substring(0, str.length() - 1);
    }
    return str;
  }

}

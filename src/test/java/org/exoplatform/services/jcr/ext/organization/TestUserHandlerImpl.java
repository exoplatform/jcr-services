/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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

import java.util.Calendar;

import org.exoplatform.services.jcr.ext.BaseStandaloneTest;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.Query;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: TestOrganizationService.java 111 2008-11-11 11:11:11Z $
 */
public class TestUserHandlerImpl extends BaseStandaloneTest {

  private Calendar            calendar;

  private OrganizationService organizationService;

  private UserHandler         uHandler;

  /**
   * {@inheritDoc}
   */
  public void setUp() throws Exception {
    super.setUp();

    organizationService = (OrganizationService) container.getComponentInstance(OrganizationService.class);
    uHandler = organizationService.getUserHandler();

    calendar = Calendar.getInstance();
    calendar.set(2008, 1, 1);
  }

  /**
   * Authenticate users.
   */
  public void testAuthenticate() throws Exception {
    try {
      assertTrue(uHandler.authenticate("demo", "exo"));
      assertFalse(uHandler.authenticate("demo", "exo_"));
      assertFalse(uHandler.authenticate("demo_", "exo"));

    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown.");
    }
  }

  /**
   * Find user with specific name and check it properties.
   */
  public void testFindUserByName() throws Exception {
    try {
      User u = uHandler.findUserByName("demo");
      assertNotNull(u);
      assertEquals(u.getEmail(), "demo@localhost");
      assertEquals(u.getFirstName(), "Demo");
      assertEquals(u.getLastName(), "exo");
      assertEquals(u.getPassword(), "exo");
      assertEquals(u.getUserName(), "demo");

      assertNull(uHandler.findUserByName("not-existed-user"));
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown.");
    }
  }

  /**
   * Find users using query and check it count.
   */
  public void testFindUsersByQuery() throws Exception {
    try {
      createUser("tolik");
      Query query = new Query();

      query.setEmail("email@test");
      assertEquals(uHandler.findUsersByQuery(query).getSize(), 1);
      query.setEmail(null);

      query.setUserName("*tolik*");
      assertEquals(uHandler.findUsersByQuery(query).getSize(), 1);

      query.setUserName("*tolik");
      assertEquals(uHandler.findUsersByQuery(query).getSize(), 1);

      query.setUserName("tolik*");
      assertEquals(uHandler.findUsersByQuery(query).getSize(), 1);

      query.setUserName("tolik");
      assertEquals(uHandler.findUsersByQuery(query).getSize(), 1);

      query.setUserName("tol");
      assertEquals(uHandler.findUsersByQuery(query).getSize(), 0);
      query.setUserName(null);

      query.setFirstName("first");
      query.setLastName("last");
      assertEquals(uHandler.findUsersByQuery(query).getSize(), 1);
      query.setFirstName(null);
      query.setLastName(null);

      Calendar calc = Calendar.getInstance();
      calc.set(2007, 1, 1);
      query.setFromLoginDate(calc.getTime());
      query.setUserName("*tolik*");
      assertEquals(uHandler.findUsersByQuery(query).getSize(), 1);

      calc.set(2009, 1, 1);
      query.setFromLoginDate(calc.getTime());
      assertEquals(uHandler.findUsersByQuery(query).getSize(), 0);
      query.setFromLoginDate(null);

      calc.set(2007, 1, 1);
      query.setToLoginDate(calc.getTime());
      assertEquals(uHandler.findUsersByQuery(query).getSize(), 0);

      calc.set(2009, 1, 1);
      query.setToLoginDate(calc.getTime());
      assertEquals(uHandler.findUsersByQuery(query).getSize(), 1);
      query.setUserName(null);
      query.setToLoginDate(null);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown.");
    } finally {
      uHandler.removeUser("tolik", true);
    }
  }

  /**
   * Find users using query and check it count.
   */
  public void testFindUsers() throws Exception {
    try {
      createUser("tolik");
      org.exoplatform.services.organization.Query query = new org.exoplatform.services.organization.Query();

      query.setEmail("email@test");
      assertEquals(uHandler.findUsers(query).getAll().size(), 1);
      query.setEmail(null);

      query.setUserName("*tolik*");
      assertEquals(uHandler.findUsers(query).getAll().size(), 1);

      query.setUserName("*tolik");
      assertEquals(uHandler.findUsers(query).getAll().size(), 1);

      query.setUserName("tolik*");
      assertEquals(uHandler.findUsers(query).getAll().size(), 1);

      query.setUserName("tolik");
      assertEquals(uHandler.findUsers(query).getAll().size(), 1);

      query.setUserName("tol");
      assertEquals(uHandler.findUsers(query).getAll().size(), 0);
      query.setUserName(null);

      query.setFirstName("first");
      query.setLastName("last");
      assertEquals(uHandler.findUsers(query).getAll().size(), 1);
      query.setFirstName(null);
      query.setLastName(null);

      Calendar calc = Calendar.getInstance();
      calc.set(2007, 1, 1);
      query.setFromLoginDate(calc.getTime());
      query.setUserName("*tolik*");
      assertEquals(uHandler.findUsers(query).getAll().size(), 1);

      calc.set(2009, 1, 1);
      query.setFromLoginDate(calc.getTime());
      assertEquals(uHandler.findUsers(query).getAll().size(), 0);
      query.setFromLoginDate(null);

      calc.set(2007, 1, 1);
      query.setToLoginDate(calc.getTime());
      assertEquals(uHandler.findUsers(query).getAll().size(), 0);

      calc.set(2009, 1, 1);
      query.setToLoginDate(calc.getTime());
      assertEquals(uHandler.findUsers(query).getAll().size(), 1);
      query.setUserName(null);
      query.setToLoginDate(null);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown.");
    } finally {
      uHandler.removeUser("tolik", true);
    }
  }

  /**
   * Get users page list.
   */
  public void testGetUserPageList() throws Exception {
    try {
      assertEquals(uHandler.getUserPageList(10).getAll().size(), 4);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown.");
    }
  }

  /**
   * Get users page list.
   */
  public void testFindAllUsers() throws Exception {
    try {
      assertEquals(uHandler.findAllUsers().getSize(), 4);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown.");
    }
  }

  /**
   * Create user and than try to remove it.
   */
  public void testRemoveUser() throws Exception {
    User u;
    try {
      createUser("user");
      u = uHandler.removeUser("user", true);
      assertNotNull(u);
      assertNull(uHandler.findUserByName("user"));
      assertNull(uHandler.removeUser("not-existed-user", true));

    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown.");
    }
  }

  /**
   * Create user, change it properties and than try to save it.
   */
  public void testSaveUser() throws Exception {
    try {
      createUser("user_");

      // change name
      User u = uHandler.findUserByName("user_");
      u.setUserName("userNew");
      uHandler.saveUser(u, true);
      u = uHandler.findUserByName("userNew");
      assertNotNull(u);

      // change email
      u.setEmail("email_");
      uHandler.saveUser(u, true);
      u = uHandler.findUserByName("userNew");
      assertEquals(u.getEmail(), "email_");

    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown.");
    } finally {
      uHandler.removeUser("userNew", true);
    }
  }

  /**
   * Create user.
   */
  public void testCreateUser() throws Exception {
    try {
      User u = uHandler.createUserInstance("user");
      u.setEmail("email@test");
      u.setFirstName("first");
      u.setLastName("last");
      u.setPassword("pwd");
      uHandler.createUser(u, true);

      assertNotNull(uHandler.findUserByName("user"));

    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown.");
    } finally {
      uHandler.removeUser("user", true);
    }
  }

  /**
   * Create new user.
   */
  private void createUser(String userName) throws Exception {
    try {
      User u = uHandler.createUserInstance(userName);
      u.setEmail("email@test");
      u.setFirstName("first");
      u.setLastLoginTime(calendar.getTime());
      u.setCreatedDate(calendar.getTime());
      u.setLastName("last");
      u.setPassword("pwd");
      uHandler.createUser(u, true);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown.");
    }
  }

  /**
   * {@inheritDoc}
   */
  protected void tearDown() throws Exception {
    super.tearDown();
  }
}

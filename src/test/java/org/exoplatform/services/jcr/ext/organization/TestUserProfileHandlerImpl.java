/**
 * 
 */
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

import java.util.Collection;

import org.exoplatform.services.jcr.ext.BaseStandaloneTest;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.organization.UserProfileHandler;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: TestUserProfileHandlerImpl.java 111 2008-11-11 11:11:11Z $
 */
public class TestUserProfileHandlerImpl extends BaseStandaloneTest {
  private OrganizationService organizationService;

  private UserHandler         uHandler;

  private UserProfileHandler  upHandler;

  /**
   * {@inheritDoc}
   */
  public void setUp() throws Exception {
    super.setUp();

    organizationService = (OrganizationService) container.getComponentInstance(OrganizationService.class);

    upHandler = organizationService.getUserProfileHandler();
    uHandler = organizationService.getUserHandler();
  }

  /**
   * Find user profile by user name and check attributes.
   */
  public void testFindUserProfileByName() throws Exception {
    UserProfile up;
    try {
      createUserProfile("userP1", true);
      up = upHandler.findUserProfileByName("userP1");
      assertNotNull(up);
      assertEquals(up.getUserName(), "userP1");
      assertEquals(up.getAttribute("key1"), "value1");
      assertEquals(up.getAttribute("key2"), "value2");

      // find profile for not existed user
      assertNull(upHandler.findUserProfileByName("not-existed-user"));

      // find not existed profile
      createUserProfile("userP2", false);
      assertNull(upHandler.findUserProfileByName("userP2"));

    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown");
    } finally {
      upHandler.removeUserProfile("userP1", true);
      upHandler.removeUserProfile("userP2", true);
      uHandler.removeUser("userP1", true);
      uHandler.removeUser("userP2", true);
    }
  }

  /**
   * Find all profiles and check it count.
   */
  public void testFindUserProfiles() throws Exception {
    try {
      createUserProfile("userP1", true);
      createUserProfile("userP2", true);
      Collection list = upHandler.findUserProfiles();
      assertNotNull(list);
      assertEquals(list.size(), 2);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown");
    } finally {
      upHandler.removeUserProfile("userP1", true);
      upHandler.removeUserProfile("userP2", true);
      uHandler.removeUser("userP1", true);
      uHandler.removeUser("userP2", true);
    }
  }

  /**
   * Create user profile and than try to remove it.
   */
  public void testRemoveUserProfile() throws Exception {
    UserProfile up;
    try {
      createUserProfile("userP1", true);

      up = upHandler.removeUserProfile("userP1", true);
      assertEquals(up.getAttribute("key1"), "value1");
      assertEquals(up.getAttribute("key2"), "value2");
      assertNull(upHandler.findUserProfileByName("userP1"));

      // remove not existed profile
      assertNull(upHandler.removeUserProfile("not-existed-user", true));

    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown");
    } finally {
      upHandler.removeUserProfile("userP1", true);
      uHandler.removeUser("userP1", true);
    }
  }

  /**
   * Create user profile, make changes, save and than try to check it.
   */
  public void testSaveUserProfile() throws Exception {
    try {
      createUserProfile("userP1", true);

      UserProfile up = upHandler.findUserProfileByName("userP1");
      up.setAttribute("key1", "value11");
      up.setAttribute("key2", null);
      upHandler.saveUserProfile(up, true);

      up = upHandler.findUserProfileByName("userP1");
      assertEquals(up.getAttribute("key1"), "value11");
      assertNull(up.getAttribute("key2"));

      // save user profile for not existed user
      try {
        up = upHandler.createUserProfileInstance("not-existed-user");
        upHandler.saveUserProfile(up, true);
      } catch (Exception e) {
        e.printStackTrace();
        fail("Exception should not be thrown");
      }

    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown");
    } finally {
      upHandler.removeUserProfile("userP1", true);
      uHandler.removeUser("userP1", true);
    }
  }

  /**
   * Create user with profile.
   */
  private void createUserProfile(String userName, boolean createProfile) {
    // create users
    try {
      User u = uHandler.createUserInstance(userName);
      u.setEmail("email");
      u.setFirstName("first");
      u.setLastName("last");
      u.setPassword("pwd");
      uHandler.createUser(u, true);

      // create profile
      if (createProfile) {
        UserProfile up = upHandler.createUserProfileInstance(userName);
        up.setAttribute("key1", "value1");
        up.setAttribute("key2", "value2");
        upHandler.saveUserProfile(up, true);
      }
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

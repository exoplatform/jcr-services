/**
 * 
 */
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

import org.exoplatform.services.jcr.ext.BaseStandaloneTest;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipHandler;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.MembershipTypeHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: TestMembershipImpl.java 111 2008-11-11 11:11:11Z $
 */
public class TestMembershipImpl extends BaseStandaloneTest {
  private GroupHandler          gHandler;

  private MembershipHandler     mHandler;

  private UserHandler           uHandler;

  private MembershipTypeHandler mtHandler;

  private OrganizationService   organizationService;

  /**
   * {@inheritDoc}
   */
  public void setUp() throws Exception {
    super.setUp();

    organizationService = (OrganizationService) container.getComponentInstanceOfType(OrganizationService.class);

    gHandler = organizationService.getGroupHandler();
    uHandler = organizationService.getUserHandler();
    mHandler = organizationService.getMembershipHandler();
    mtHandler = organizationService.getMembershipTypeHandler();
  }

  /**
   * Find membership.
   */
  public void testFindMembership() throws Exception {
    try {
      createMembership("user", "group", "type");
      Membership m = mHandler.findMembershipByUserGroupAndType("user", "/group", "type");
      assertNotNull(mHandler.findMembership(m.getId()));
      assertNull(mHandler.findMembership("not-existed-id"));

    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown");
    } finally {
      Membership m = mHandler.findMembershipByUserGroupAndType("user", "/group", "type");
      mHandler.removeMembership(m.getId(), true);
      uHandler.removeUser("user", true);
      gHandler.removeGroup(gHandler.findGroupById("/group"), true);
      mtHandler.removeMembershipType("type", true);
    }
  }

  /**
   * Find membership and check it properties.
   */
  public void testFindMembershipByUserGroupAndType() throws Exception {
    try {
      Membership m = mHandler.findMembershipByUserGroupAndType("marry", "/platform/users", "member");
      assertNotNull(m);
      assertEquals(m.getGroupId(), "/platform/users");
      assertEquals(m.getMembershipType(), "member");
      assertEquals(m.getUserName(), "marry");

      assertNull(mHandler.findMembershipByUserGroupAndType("non-existed-marry",
                                                           "/platform/users",
                                                           "member"));
      assertNull(mHandler.findMembershipByUserGroupAndType("marry", "/non-existed-group", "member"));
      assertNull(mHandler.findMembershipByUserGroupAndType("marry",
                                                           "/platform/users",
                                                           "non-existed-member"));
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown");
    }
  }

  /**
   * Find membership by specific group and check it count.
   */
  public void testFindMembershipsByGroup() throws Exception {
    try {
      Group g = gHandler.findGroupById("/platform/users");
      assertEquals(mHandler.findMembershipsByGroup(g).size(), 5);

      g = gHandler.createGroupInstance();
      g.setGroupName("not-existed-group");
      assertEquals(mHandler.findMembershipsByGroup(g).size(), 0);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown");
    }
  }

  /**
   * Find all membership by specific user and check it count.
   */
  public void testFindMembershipsByUser() throws Exception {
    try {
      assertEquals(mHandler.findMembershipsByUser("john").size(), 5);
      assertEquals(mHandler.findMembershipsByUser("not-existed-user").size(), 0);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown");
    }
  }

  /**
   * Find all membership by specific user and group and check it count.
   */
  public void testFindMembershipsByUserAndGroup() throws Exception {
    try {
      assertEquals(mHandler.findMembershipsByUserAndGroup("john", "/platform/users").size(), 1);
      assertEquals(mHandler.findMembershipsByUserAndGroup("non-existed-john", "/platform/users")
                           .size(), 0);
      assertEquals(mHandler.findMembershipsByUserAndGroup("john", "/non-existed-group").size(), 0);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown");
    }
  }

  /**
   * Link membership.
   */
  public void testLinkMembership() throws Exception {
    try {
      // create users
      User u = uHandler.createUserInstance("linkUser");
      u.setEmail("email");
      u.setFirstName("first");
      u.setLastName("last");
      u.setPassword("pwd");
      uHandler.createUser(u, true);
      u = uHandler.findUserByName("linkUser");

      // create groups
      Group g = gHandler.createGroupInstance();
      g.setGroupName("linkGroup");
      g.setLabel("label");
      g.setDescription("desc");
      gHandler.createGroup(g, true);
      g = gHandler.findGroupById("/linkGroup");

      // Create membership types
      MembershipType mt = mtHandler.createMembershipTypeInstance();
      mt.setName("linkType");
      mt.setDescription("desc");
      mtHandler.createMembershipType(mt, true);
      mt = mtHandler.findMembershipType("linkType");

      // link membership
      mHandler.linkMembership(u, g, mt, false);

      Membership m = mHandler.findMembershipByUserGroupAndType("linkUser", "/linkGroup", "linkType");
      assertNotNull(m);

      mHandler.removeMembership(m.getId(), true);

      mHandler.createMembership(m, true);
      m = mHandler.findMembershipByUserGroupAndType("linkUser", "/linkGroup", "linkType");
      assertNotNull(m);

      g = gHandler.createGroupInstance();
      g.setGroupName("not-existed-group");
      mHandler.linkMembership(u, g, mt, true);
      assertNull(mHandler.findMembershipByUserGroupAndType(u.getUserName(), g.getId(), mt.getName()));

      u = uHandler.createUserInstance("not-existed-user");
      mHandler.linkMembership(u, g, mt, true);
      assertNull(mHandler.findMembershipByUserGroupAndType(u.getUserName(), g.getId(), mt.getName()));

      try {
        mHandler.linkMembership(u, null, mt, true);
        fail("Exception should be thrown");
      } catch (Exception e) {
      }

      try {
        mHandler.linkMembership(u, g, null, true);
        fail("Exception should be thrown");
      } catch (Exception e) {
      }

    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown");
    } finally {
      Membership m = mHandler.findMembershipByUserGroupAndType("linkUser", "/linkGroup", "linkType");
      mHandler.removeMembership(m.getId(), true);
      uHandler.removeUser("linkUser", true);
      gHandler.removeGroup(gHandler.findGroupById("/linkGroup"), true);
      mtHandler.removeMembershipType("linkType", true);
    }
  }

  /**
   * Create new membeship and try to remove it.
   */
  public void testRemoveMembership() throws Exception {
    try {
      createMembership("user", "group", "type");

      Membership m = mHandler.findMembershipByUserGroupAndType("user", "/group", "type");
      assertNotNull(m);

      m = mHandler.removeMembership(m.getId(), true);
      assertEquals(m.getGroupId(), "/group");
      assertEquals(m.getMembershipType(), "type");
      assertEquals(m.getUserName(), "user");

      assertNull(mHandler.findMembershipByUserGroupAndType("user", "/group", "type"));

      try {
        assertNull(mHandler.removeMembership("not-existed-id", true));
      } catch (Exception e) {
        e.printStackTrace();
        fail("Exception should not be thrown");
      }

    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown");
    } finally {
      uHandler.removeUser("user", true);
      gHandler.removeGroup(gHandler.findGroupById("/group"), true);
      mtHandler.removeMembershipType("type", true);
    }
  }

  /**
   * Create membership and than try to remove it by specific user.
   */
  public void testRemoveMembershipByUser() throws Exception {
    try {
      createMembership("user", "group", "type");

      assertEquals(mHandler.removeMembershipByUser("user", true).size(), 1);
      assertNull(mHandler.findMembershipByUserGroupAndType("user", "/group", "type"));

      try {
        mHandler.removeMembershipByUser("not-existed-user", true);
      } catch (Exception e) {
        e.printStackTrace();
        fail("Exception should not be thrown");
      }

    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown");
    } finally {
      uHandler.removeUser("user", true);
      gHandler.removeGroup(gHandler.findGroupById("/group"), true);
      mtHandler.removeMembershipType("type", true);
    }
  }

  /**
   * Find groups by membership and check it count.
   */
  public void testFindGroupByMembership() throws Exception {
    try {
      assertEquals(gHandler.findGroupByMembership("john", "manager").size(), 2);
      assertEquals(gHandler.findGroupByMembership("not-existed-john", "manager").size(), 0);
      assertEquals(gHandler.findGroupByMembership("john", "not-existed-manager").size(), 0);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown");
    }
  }

  /**
   * Find groups and check it count.
   */
  public void testFindGroupsOfUser() throws Exception {
    try {
      assertEquals(gHandler.findGroupsOfUser("james").size(), 2);
      assertEquals(gHandler.findGroupsOfUser("not-existed-james").size(), 0);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown");
    }
  }

  /**
   * Find users by group and check it count.
   */
  public void testFindUsersByGroup() throws Exception {
    try {
      assertEquals(uHandler.findUsersByGroup("/platform/users").getAll().size(), 5);
      assertEquals(uHandler.findUsersByGroup("/not-existed-group").getAll().size(), 0);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown");
    }

  }

  /**
   * Create new membership type and try to remove it.
   */
  public void testRemoveMembershipType() throws Exception {
    try {
      createMembership("user", "group", "type");
      mtHandler.removeMembershipType("type", true);
      assertNull(mtHandler.findMembershipType("type"));
      assertNull(mHandler.findMembershipByUserGroupAndType("user", "/group", "type"));

    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown");
    } finally {
      uHandler.removeUser("user", true);
      gHandler.removeGroup(gHandler.findGroupById("/group"), true);
    }
  }

  /**
   * Create new group and try to remove it.
   */
  public void testRemoveGroup() throws Exception {
    try {
      createMembership("user", "group", "type");
      gHandler.removeGroup(gHandler.findGroupById("/group"), true);

      assertNull(gHandler.findGroupById("/group"));
      assertNull(mHandler.findMembershipByUserGroupAndType("user", "/group", "type"));

      try {
        Group g = gHandler.createGroupInstance();
        g.setGroupName("not-existed-group");
        gHandler.removeGroup(g, true);

        fail("Exception should be thrown");
      } catch (Exception e) {
      }

      try {
        gHandler.removeGroup(null, true);
        fail("Exception should be thrown");
      } catch (Exception e) {
      }

    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown");
    } finally {
      uHandler.removeUser("user", true);
      mtHandler.removeMembershipType("type", true);
    }
  }

  /**
   * {@inheritDoc}
   */
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  private void createMembership(String userName, String groupName, String type) {
    try {
      // create users
      User u = uHandler.createUserInstance(userName);
      u.setEmail("email");
      u.setFirstName("first");
      u.setLastName("last");
      u.setPassword("pwd");
      uHandler.createUser(u, true);
      u = uHandler.findUserByName(userName);

      // create groups
      Group g = gHandler.createGroupInstance();
      g.setGroupName(groupName);
      g.setLabel("label");
      g.setDescription("desc");
      gHandler.createGroup(g, true);
      g = gHandler.findGroupById("/" + groupName);

      // Create membership types
      MembershipType mt = mtHandler.createMembershipTypeInstance();
      mt.setName(type);
      mt.setDescription("desc");
      mtHandler.createMembershipType(mt, true);
      mt = mtHandler.findMembershipType(type);

      // link membership
      mHandler.linkMembership(u, g, mt, true);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown");
    }
  }
}

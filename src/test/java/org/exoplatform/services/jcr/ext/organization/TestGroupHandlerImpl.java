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

import org.exoplatform.services.jcr.ext.BaseStandaloneTest;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.OrganizationService;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: TestOrganizationService.java 111 2008-11-11 11:11:11Z $
 */
public class TestGroupHandlerImpl extends BaseStandaloneTest {

  private GroupHandler        gHandler;

  private OrganizationService organizationService;

  /**
   * {@inheritDoc}
   */
  public void setUp() throws Exception {
    super.setUp();
    organizationService = (OrganizationService) container.getComponentInstanceOfType(OrganizationService.class);
    gHandler = organizationService.getGroupHandler();
  }

  /**
   * Find group by id and check it properties.
   */
  public void testFindGroupById() throws Exception {
    try {
      Group g = gHandler.findGroupById("/platform/administrators");
      assertNotNull(g);
      assertEquals(g.getDescription(), "the /platform/administrators group");
      assertEquals(g.getGroupName(), "administrators");
      assertEquals(g.getId(), "/platform/administrators");
      assertEquals(g.getLabel(), "Administrators");
      assertEquals(g.getParentId(), "/platform");

      assertNull(gHandler.findGroupById("/not-existed-group"));

    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown.");
    }
  }

  /**
   * Find group by id and check it properties.
   */
  public void testFindGroupsByUser() throws Exception {
    try {
      // Hibernate org service returns
      // [Group[/organization/management/executive-board|executive-board],
      // Group[/platform/administrators|administrators], Group[/platform/users|users]]
      // JCR returns
      // [[groupId=/platform/administrators][groupName=administrators][parentId=/platform],
      // [groupId=/platform/users][groupName=users][parentId=/platform],
      // [groupId=/organization/management/executive-board][groupName=executive-board][parentId=/
      // organization/management],
      // [groupId=/organization/management/executive-board][groupName=executive-board][parentId=/
      // organization/management],
      // [groupId=/organization/management/executive-board][groupName=executive-board][parentId=/
      // organization/management]]
      assertEquals(gHandler.findGroupsOfUser("john").size(), 3);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown.");
    }
  }

  /**
   * Find groups by specific parent and check it count.
   */
  public void testFindGroups() throws Exception {
    try {
      assertEquals(gHandler.findGroups(null).size(), 4);
      assertEquals(gHandler.findGroups(gHandler.findGroupById("/organization/operations")).size(),
                   2);
      assertEquals(gHandler.findGroups(gHandler.findGroupById("/organization/management/executive-board"))
                           .size(),
                   0);

      // find from not existed group
      createGroup("/organization/management/executive-board", "group1", "label", "desc");
      Group g = gHandler.findGroupById("/organization/management/executive-board/group1");
      gHandler.removeGroup(g, true);
      assertEquals(gHandler.findGroups(g).size(), 0);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown.");
    }
  }

  /**
   * Get all groups and check it count.
   */
  public void testGetAllGroups() throws Exception {
    try {
      assertEquals(gHandler.getAllGroups().size(), 16);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown.");
    }
  }

  /**
   * Create new group and try to remove it.
   */
  public void testRemoveGroup() throws Exception {
    try {
      createGroup("/organization", "group1", "label", "desc");
      createGroup("/organization/group1", "group2", "label", "desc");

      gHandler.removeGroup(gHandler.findGroupById("/organization/group1"), true);
      assertNull(gHandler.findGroupById("/organization/group1"));
      assertNull(gHandler.findGroupById("/organization/group1/group2"));

      // create in root
      createGroup(null, "group1", "label", "desc");
      createGroup("/group1", "group2", "label", "desc");

      gHandler.removeGroup(gHandler.findGroupById("/group1"), true);
      assertNull(gHandler.findGroupById("/group1"));
      assertNull(gHandler.findGroupById("/group1/group2"));

    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown.");
    }

    try {
      createGroup("/organization", "group1", "label", "desc");
      gHandler.removeGroup(gHandler.findGroupById("/organization/group1"), true);
      gHandler.removeGroup(gHandler.findGroupById("/organization/group1"), true);
      fail("Exception should be thrown");
    } catch (Exception e) {
    }
  }

  /**
   * Add child group.
   */
  public void testAddChild() throws Exception {
    try {
      Group parent = gHandler.createGroupInstance();
      parent.setGroupName("parentGroup");

      Group child = gHandler.createGroupInstance();
      child.setGroupName("group");

      try {
        gHandler.addChild(parent, child, false);
        fail("Exception should be thrown.");
      } catch (Exception e) {
      }

      // add parent group
      gHandler.addChild(null, parent, false);
      assertNotNull(gHandler.findGroupById("/parentGroup"));

      // add child group
      gHandler.addChild(parent, child, false);
      assertNotNull(gHandler.findGroupById("/parentGroup/group"));

    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown.");
    } finally {
      gHandler.removeGroup(gHandler.findGroupById("/parentGroup"), true);
    }
  }

  /**
   * Create group.
   */
  public void testCreateGroup() throws Exception {
    try {
      Group group = gHandler.createGroupInstance();
      group.setGroupName("group");
      gHandler.createGroup(group, true);
      assertNotNull(gHandler.findGroupById("/group"));

    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown.");
    } finally {
      gHandler.removeGroup(gHandler.findGroupById("/group"), true);
    }
  }

  /**
   * Create new group, change properties and save. Then try to check it.
   */
  public void testSaveGroup() throws Exception {
    try {
      createGroup("/organization", "group1", "label", "desc");

      Group g = gHandler.findGroupById("/organization/group1");
      g.setDescription("newDesc");
      gHandler.saveGroup(g, true);

      // check
      g = gHandler.findGroupById("/organization/group1");
      assertEquals(g.getDescription(), "newDesc");

    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown.");
    } finally {
      gHandler.removeGroup(gHandler.findGroupById("/organization/group1"), true);
    }
  }

  /**
   * Create new group.
   */
  private void createGroup(String parentId, String name, String label, String desc) {
    try {
      Group parent = gHandler.findGroupById(parentId);

      Group child = gHandler.createGroupInstance();
      child.setGroupName(name);
      child.setLabel(label);
      child.setDescription(desc);
      gHandler.addChild(parent, child, true);
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

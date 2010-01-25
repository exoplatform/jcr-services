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

import java.util.Collection;
import java.util.List;

import org.exoplatform.commons.utils.PageList;
import org.exoplatform.services.jcr.ext.BaseStandaloneTest;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipHandler;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.MembershipTypeHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.Query;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;

/**
 * Created by The eXo Platform SAS Author : Hoa Pham hoapham@exoplatform.com,phamvuxuanhoa@yahoo.com
 * Oct 27, 2005
 */

public class TestOrganizationService extends BaseStandaloneTest {

  static String                 Group1 = "Group1";

  static String                 Group2 = "Group2";

  static String                 Benj   = "Benj";

  static String                 Tuan   = "Tuan";

  private GroupHandler          gHandler;

  private MembershipHandler     mHandler;

  private UserHandler           uHandler;

  private MembershipTypeHandler mtHandler;

  private OrganizationService   organizationService;

  public void setUp() throws Exception {
    super.setUp();

    organizationService = (OrganizationService) container.getComponentInstanceOfType(OrganizationService.class);

    gHandler = organizationService.getGroupHandler();
    uHandler = organizationService.getUserHandler();
    mHandler = organizationService.getMembershipHandler();
    mtHandler = organizationService.getMembershipTypeHandler();

  }

  public void tearDown() throws Exception {
    System.err.println("##############################################################");

    super.tearDown();
  }

  public void testUserPageSize() throws Exception {
    /* Create an user with UserName: test */
    String USER = "test";
    int s = 20;

    for (int i = 0; i < s; i++)
      createUser(USER + "_" + String.valueOf(i));
    Query query = new Query();
    PageList users = uHandler.findUsers(query);
    System.out.println("\n\n\n\n\n\n size: " + users.getAvailablePage());

    List list = users.getPage(1);
    for (Object ele : list) {
      User u = (User) ele;
      System.out.println(u.getUserName() + " and " + u.getEmail());
    }
    System.out.println("\n\n\n\n page 2:");
    list = users.getPage(2);
    System.out.println("size : " + list.size());
    for (Object ele : list) {
      User u = (User) ele;
      System.out.println(u.getUserName() + " and " + u.getEmail());
    }
    System.out.println("\n\n\n\n");

    for (int i = 0; i < s; i++) {
      uHandler.removeUser(USER + "_" + String.valueOf(i), true);
    }
  }

  public void testUser() throws Exception {
    /* Create an user with UserName: test */
    String USER = "testUser";

    createUser(USER);

    User u = uHandler.findUserByName(USER);
    assertTrue("Found user instance", u != null);
    assertEquals("Expect user name is: ", USER, u.getUserName());

    // UserProfile up = profileHandler_.findUserProfileByName(USER);
    // assertTrue("Expect user profile is found: ", up != null);

    Query query = new Query();
    PageList users = uHandler.findUsers(query);
    assertTrue("Expect 5 user found ", users.getAvailable() == 5);

    /* Update user's information */
    u.setFirstName("Exo(Update)");
    uHandler.saveUser(u, false);
    // up.getUserInfoMap().put("user.gender", "male");
    // profileHandler_.saveUserProfile(up, true);
    // up = profileHandler_.findUserProfileByName(USER);
    assertEquals("expect first name is", "Exo(Update)", u.getFirstName());
    // assertEquals("Expect profile is updated: user.gender is ", "male", up.getUserInfoMap()
    // .get("user.gender"));

    PageList piterator = uHandler.getUserPageList(10);
    // assertTrue (piterator.currentPage().size() == 2) ;
    assertEquals(5, piterator.currentPage().size()); // [PN] was 2, but from
    // where?

    /*
     * Remove a user: Expect result: user and it's profile will be removed
     */
    uHandler.removeUser(USER, true);
    assertEquals("User: USER is removed: ", null, uHandler.findUserByName(USER));
  }

  public void testGroup() throws Exception {
    /* Create a parent group with name is: GroupParent */
    String parentName = "GroupParent";
    Group groupParent = gHandler.createGroupInstance();
    groupParent.setGroupName(parentName);
    groupParent.setDescription("This is description");
    gHandler.createGroup(groupParent, true);
    assertTrue(((Group) groupParent).getId() != null); // [PN] was GroupImpl of
    // jdbc, caused a class
    // cast exc.
    groupParent = gHandler.findGroupById(groupParent.getId());
    assertEquals(groupParent.getGroupName(), "GroupParent");

    /* Create a child group with name: Group1 */
    Group groupChild = gHandler.createGroupInstance();
    groupChild.setGroupName(Group1);
    gHandler.addChild(groupParent, groupChild, true);
    groupChild = gHandler.findGroupById(groupParent.getId() + "/" + groupChild.getGroupName());
    assertEquals(groupChild.getParentId(), groupParent.getId());
    assertEquals("Expect group child's name is: ", Group1, groupChild.getGroupName());

    /* Update groupChild's information */
    groupChild.setLabel("GroupRenamed");
    groupChild.setDescription("new description ");
    gHandler.saveGroup(groupChild, true);
    assertEquals(gHandler.findGroupById(groupChild.getId()).getLabel(), "GroupRenamed");

    /* Create a group child with name is: Group2 */
    groupChild = gHandler.createGroupInstance();
    groupChild.setGroupName(Group2);
    gHandler.addChild(groupParent, groupChild, true);
    groupChild = gHandler.findGroupById(groupParent.getId() + "/" + groupChild.getGroupName());
    assertEquals(groupChild.getParentId(), groupParent.getId());
    assertEquals("Expect group child's name is: ", Group2, groupChild.getGroupName());

    /*
     * find all child group in groupParent Expect result: 2 child group: group1,
     * group2
     */
    Collection groups = gHandler.findGroups(groupParent);
    assertEquals("Expect number of child group in parent group is: ", 2, groups.size());
    Object arraygroups[] = groups.toArray();
    assertEquals("Expect child group's name is: ", Group1, ((Group) arraygroups[0]).getGroupName());
    assertEquals("Expect child group's name is: ", Group2, ((Group) arraygroups[1]).getGroupName());

    /* Remove a groupchild */
    gHandler.removeGroup(gHandler.findGroupById("/" + parentName + "/" + Group1), true);
    assertEquals("Expect child group has been removed: ",
                 null,
                 gHandler.findGroupById("/" + Group1));
    assertEquals("Expect only 1 child group in parent group", 1, gHandler.findGroups(groupParent)
                                                                         .size());

    /* Remove Parent group, all it's group child will be removed */
    gHandler.removeGroup(groupParent, true);
    assertEquals("Expect ParentGroup is removed:",
                 null,
                 gHandler.findGroupById(groupParent.getId()));
    assertEquals("Expect all child group is removed: ", 0, gHandler.findGroups(groupParent).size());
  }

  public void testMembershipType() throws Exception {
    /* Create a membershipType */
    String testType = "testType";
    MembershipType mt = mtHandler.createMembershipTypeInstance();
    mt.setName(testType);
    mt.setDescription("This is a test");
    mt.setOwner("exo");
    mtHandler.createMembershipType(mt, true);
    assertEquals("Expect mebershiptype is:", testType, mtHandler.findMembershipType(testType)
                                                                .getName());

    /* Update MembershipType's information */
    String desc = "This is a test (update)";
    mt.setDescription(desc);
    mtHandler.saveMembershipType(mt, true);
    assertEquals("Expect membershiptype's description",
                 desc,
                 mtHandler.findMembershipType(testType).getDescription());

    /* create another membershipType */
    mt = mtHandler.createMembershipTypeInstance();
    mt.setName("anothertype");
    mt.setOwner("exo");
    mtHandler.createMembershipType(mt, true);

    /*
     * find all membership type Expect result: 3 membershipType:
     * "testmembership", "anothertype" and "member"(default membership type)
     */
    Collection ms = mtHandler.findMembershipTypes();
    assertEquals("Expect 5 membership in collection: ", 5, ms.size());

    /* remove "testmembership" */
    mtHandler.removeMembershipType(testType, true);
    assertEquals("Membership type has been removed:", null, mtHandler.findMembershipType(testType));
    assertEquals("Expect 4 membership in collection: ", 4, mtHandler.findMembershipTypes().size());

    /* remove "anothertype" */
    mtHandler.removeMembershipType("anothertype", true);
    assertEquals("Membership type has been removed:",
                 null,
                 mtHandler.findMembershipType("anothertype"));
    assertEquals("Expect 3 membership in collection: ", 3, mtHandler.findMembershipTypes().size());
    /* All membershipType was removed(except default membership) */
  }

  public void testMembership() throws Exception {
    /* Create 2 user: benj and tuan */
    createUser(Benj);
    createUser(Tuan);
    User user = uHandler.findUserByName(Benj);
    User user2 = uHandler.findUserByName(Tuan);

    /* Create "Group1" */
    Group group = gHandler.createGroupInstance();
    group.setGroupName(Group1);
    gHandler.createGroup(group, true);
    /* Create "Group2" */
    group = gHandler.createGroupInstance();
    group.setGroupName(Group2);
    gHandler.createGroup(group, true);

    /* Create membership1 and assign Benj to "Group1" with this membership */
    String testType = "testmembership";
    MembershipType mt = mtHandler.createMembershipTypeInstance();
    mt.setName(testType);
    mtHandler.createMembershipType(mt, true);

    mHandler.linkMembership(user, gHandler.findGroupById("/" + Group1), mt, true);
    mHandler.linkMembership(user, gHandler.findGroupById("/" + Group2), mt, true);
    mHandler.linkMembership(user2, gHandler.findGroupById("/" + Group2), mt, true);

    mt = mtHandler.createMembershipTypeInstance();
    mt.setName("membershipType2");
    mtHandler.createMembershipType(mt, true);
    mHandler.linkMembership(user, gHandler.findGroupById("/" + Group2), mt, true);

    mt = mtHandler.createMembershipTypeInstance();
    mt.setName("membershipType3");
    mtHandler.createMembershipType(mt, true);
    mHandler.linkMembership(user, gHandler.findGroupById("/" + Group2), mt, true);

    /*
     * find all memberships in group2 Expect result: 4 membership: 3 for
     * Benj(testmebership, membershipType2, membershipType3) : 1 for
     * Tuan(testmembership)
     */
    System.out.println(" --------- find memberships by group -------------");
    Collection<Membership> mems = mHandler.findMembershipsByGroup(gHandler.findGroupById("/"
        + Group2));
    assertEquals("Expect number of membership in group 4 is: ", 4, mems.size());

    /*
     * find all memberships in "Group2" relate with Benj Expect result: 3
     * membership
     */
    System.out.println(" --------- find memberships by user and group--------------");
    mems = mHandler.findMembershipsByUserAndGroup(Benj, "/" + Group2);
    assertEquals("Expect number of membership in " + Group2 + " relate with benj is: ",
                 3,
                 mems.size());

    /*
     * find all memberships of Benj in all group Expect result: 5 membership: 3
     * memberships in "Group2", 1 membership in "Users" (default) : 1 membership
     * in "group1"
     */
    System.out.println(" --------- find memberships by user-------------");
    mems = mHandler.findMembershipsByUser(Benj);
    assertEquals("expect membership is: ", 4, mems.size());

    /*
     * find memberships of Benj in "Group2" with membership type: testType
     * Expect result: 1 membership with membershipType is "testType"
     * (testmembership)
     */
    System.out.println("---------- find membership by User, Group and Type-----------");
    Membership membership = mHandler.findMembershipByUserGroupAndType(Benj, "/" + Group2, testType);
    assertTrue("Expect membership is found:", membership != null);
    assertEquals("Expect membership type is: ", testType, membership.getMembershipType());
    assertEquals("Expect groupId of this membership is: ", "/" + Group2, membership.getGroupId());
    assertEquals("Expect user of this membership is: ", Benj, membership.getUserName());

    /*
     * find all groups of Benj Expect result: 3 group: "Group1", "Group2" and
     * "user" ("user" is default group)
     */
    System.out.println(" --------- find groups by user -------------");
    Collection<Group> groups = gHandler.findGroupsOfUser(Benj);
    assertEquals("expect group is: ", 2, groups.size()); // PN 28.11.2008, fix to 2 was 4

    /*
     * find all groups has membership type "TYPE" relate with Benj expect
     * result: 2 group: "Group1" and "Group2"
     */
    System.out.println("---------- find group of a user by membership-----------");
    groups = gHandler.findGroupByMembership(Benj, testType);
    assertEquals("expect group is: ", 2, groups.size());

    /* remove a membership */
    System.out.println("----------------- removed a membership ---------------------");
    String memId = mHandler.findMembershipByUserGroupAndType(Benj, "/" + Group2, "membershipType3")
                           .getId();
    mHandler.removeMembership(memId, true);
    assertTrue("Membership was removed: ", mHandler.findMembershipByUserGroupAndType(Benj, "/"
        + Group2, "membershipType3") == null);

    /*
     * remove a user Expect result: all membership related with user will be
     * remove
     */
    System.out.println("----------------- removed a user----------------------");
    uHandler.removeUser(Tuan, true);
    assertTrue("This user was removed", uHandler.findUserByName(Tuan) == null);
    mems = mHandler.findMembershipsByUser(Tuan);
    // assertTrue("All membership related with this user was removed:", mems.isEmpty());

    /*
     * Remove a group Expect result: all membership associate with this group
     * will be removed
     */
    System.out.println("----------------- removed a group------------");
    gHandler.removeGroup(gHandler.findGroupById("/" + Group1), true);
    assertTrue("This group was removed", gHandler.findGroupById("/" + Group1) == null);

    /*
     * Remove a MembershipType Expect result: All membership have this type will
     * be removed
     */

    System.out.println("----------------- removed a membershipType------------");
    mtHandler.removeMembershipType(testType, true);
    assertTrue("This membershipType was removed: ", mtHandler.findMembershipType(testType) == null);
    // Check all memberships associate with all groups
    // * to guarantee that no membership associate with removed membershipType
    groups = gHandler.getAllGroups();
    for (Group g : groups) {
      mems = mHandler.findMembershipsByGroup(g);
      for (Membership m : mems) {
        assertFalse("MembershipType of this membership is not: " + testType,
                    m.getMembershipType().equalsIgnoreCase(testType));
      }
    }

    System.out.println("----------------- removed a othes entities------------");
    mtHandler.removeMembershipType("membershipType3", true);
    mtHandler.removeMembershipType("membershipType2", true);
    gHandler.removeGroup(gHandler.findGroupById("/" + Group2), true);
    uHandler.removeUser(Benj, true);
  }

  private void createUser(String userName) throws Exception {
    try {
      User u = uHandler.createUserInstance(userName);
      u.setEmail("email@test");
      u.setFirstName("first");
      u.setLastName("last");
      u.setPassword("pwd");
      uHandler.createUser(u, true);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown.");
    }
  }
}

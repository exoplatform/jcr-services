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

import org.exoplatform.services.jcr.ext.BaseStandaloneTest;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.MembershipTypeHandler;
import org.exoplatform.services.organization.OrganizationService;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: TestMembershipTypeHandlerImpl.java 111 2008-11-11 11:11:11Z $
 */
public class TestMembershipTypeHandlerImpl extends BaseStandaloneTest {

  private MembershipTypeHandler mtHandler;

  private OrganizationService   organizationService;

  /**
   * {@inheritDoc}
   */
  public void setUp() throws Exception {
    super.setUp();

    organizationService = (OrganizationService) container.getComponentInstance(OrganizationService.class);
    mtHandler = organizationService.getMembershipTypeHandler();
  }

  /**
   * Find membership type with specific name.
   */
  public void testFindMembershipType() throws Exception {
    MembershipType mt;
    try {
      mt = mtHandler.findMembershipType("manager");
      assertNotNull(mt);
      assertEquals(mt.getName(), "manager");
      assertEquals(mt.getDescription(), "manager membership type");

      assertNull(mtHandler.findMembershipType("manager_"));
    } catch (Exception e1) {
      e1.printStackTrace();
      fail("Exception should not be thrown");
    }
  }

  /**
   * Find all membership types in the storage and check count.
   */
  public void testFindMembershipTypes() throws Exception {
    try {
      // manager
      // member
      // validator
      assertEquals(mtHandler.findMembershipTypes().size(), 3);
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
      createMembershipType("type", "desc");
      MembershipType mt = mtHandler.removeMembershipType("type", true);
      assertEquals(mt.getName(), "type");
      assertNull(mtHandler.findMembershipType("type"));

    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown");
    }

    try {
      assertNull(mtHandler.removeMembershipType("not-existed-mt", true));
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown");
    }
  }

  /**
   * Create new membership type and try to save with new name and than with new description.
   */
  public void testSaveMembershipType() throws Exception {
    try {
      createMembershipType("type", "desc");
      MembershipType mt = mtHandler.findMembershipType("type");

      // change description
      mt.setDescription("newDesc");
      mtHandler.saveMembershipType(mt, true);
      mt = mtHandler.findMembershipType("type");
      assertEquals(mt.getDescription(), "newDesc");

    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown");
    } finally {
      mtHandler.removeMembershipType("type", true);
    }
  }

  /**
   * Create new membership type.
   * 
   * @param type
   *          The name of new type
   * @param desc
   *          The description of membership type
   * @throws Exception
   */
  private void createMembershipType(String type, String desc) {
    try {
      MembershipType mt = mtHandler.createMembershipTypeInstance();
      mt.setName(type);
      mt.setDescription(desc);
      mtHandler.createMembershipType(mt, true);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception should not be thrown");
    }
  }

  /**
   * {@inheritDoc}
   */
  protected void tearDown() throws Exception {
    super.tearDown();
  }

}

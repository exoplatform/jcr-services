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

import org.exoplatform.services.organization.Membership;

/**
 * Created by The eXo Platform SAS.
 * 
 * Date: 24.07.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id$
 */
public class MembershipImpl implements Membership, Cloneable
{

   /**
    * The group id
    */
   private String groupId;

   /**
    * The membership type id
    */
   private String membershipType;

   /**
    * The user name
    */
   private String userName;

   /**
    * The id of the membership
    */
   private String id;

   /**
    * MembershipImpl constructor.
    */
   MembershipImpl()
   {
   }

   /**
    * MembershipImpl constructor.
    * 
    * @param id
    *          The membership record identifier
    * @param userName
    *          The user name
    * @param groupId
    *          The group id
    * @param membershipType
    *          The membership type
    */
   MembershipImpl(String id, String userName, String groupId, String membershipType)
   {
      this.id = id;
      this.userName = userName;
      this.groupId = groupId;
      this.membershipType = membershipType;
   }

   /**
    * {@inheritDoc}
    */
   public String getGroupId()
   {
      return groupId;
   }

   /**
    * {@inheritDoc}
    */
   public String getId()
   {
      return id;
   }

   /**
    * {@inheritDoc}
    */
   public String getMembershipType()
   {
      return membershipType;
   }

   /**
    * {@inheritDoc}
    */
   public String getUserName()
   {
      return userName;
   }

   /**
    * Set membership id.
    */
   void setId(String id)
   {
      this.id = id;
   }

   /**
    * Set group id.
    */
   void setGroupId(String groupId)
   {
      this.groupId = groupId;
   }

   /**
    * Set user name.
    */
   void setUserName(String userName)
   {
      this.userName = userName;
   }

   /**
    * {@inheritDoc}
    */
   public void setMembershipType(String type)
   {
      membershipType = type;
   }

   /**
    * {@inheritDoc}
    */
   public String toString()
   {
      return "[groupId=" + getGroupId() + "][type=" + getMembershipType() + "][user=" + getUserName() + "]";
   }

   /**
    * {@inheritDoc}
    */
   public Object clone()
   {
      MembershipImpl membership = new MembershipImpl();
      membership.setId(id);
      membership.setMembershipType(membershipType);
      membership.setGroupId(groupId);
      membership.setUserName(userName);

      return membership;
   }
}

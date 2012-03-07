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

import org.exoplatform.services.organization.ExtendedCloneable;
import org.exoplatform.services.organization.User;

import java.util.Date;

/**
 * Created by The eXo Platform SAS.
 * 
 * Date: 24.07.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id$
 */
public class UserImpl implements User, ExtendedCloneable
{

   /**
    * The user's created date
    */
   private Date createdDate;

   /**
    * The email of the user
    */
   private String email;

   /**
    * The first name of the user
    */
   private String firstName;

   /**
    * The last login time of the user
    */
   private Date lastLoginTime;

   /**
    * The last name of the user
    */
   private String lastName;

   /**
    * The display name
    */
   private String displayName;

   /**
    * The password of the user
    */
   private transient String password;

   /**
    * The user name
    */
   private String userName;

   /**
    * The UUId of the user in the storage
    */
   private String UUId;

   /**
    * UserImpl constructor.
    */
   UserImpl()
   {
      this.UUId = null;
   }

   /**
    * UserImpl constructor.
    * 
    * @param name
    *          The user name
    */
   UserImpl(String name)
   {
      this.userName = name;
      this.UUId = null;
   }

   /**
    * UserImpl constructor.
    * 
    * @param name
    *          The user name
    * @param UUId
    *          The UUId of the use in the storage
    */
   UserImpl(String name, String UUId)
   {
      this.userName = name;
      this.UUId = UUId;
   }

   /**
    * {@inheritDoc}
    */
   public Date getCreatedDate()
   {
      return createdDate;
   }

   /**
    * {@inheritDoc}
    */
   public String getEmail()
   {
      return email;
   }

   /**
    * {@inheritDoc}
    */
   public String getFirstName()
   {
      return firstName;
   }

   /**
    * {@inheritDoc}
    */
   public String getDisplayName()
   {
      return displayName != null ? displayName : getFirstName() + " " + getLastName();
   }

   /**
    * {@inheritDoc}
    */
   public String getFullName()
   {
      return getDisplayName();
   }

   /**
    * {@inheritDoc}
    */
   public Date getLastLoginTime()
   {
      return lastLoginTime;
   }

   /**
    * {@inheritDoc}
    */
   public String getLastName()
   {
      return lastName;
   }

   /**
    * {@inheritDoc}
    */
   public String getOrganizationId()
   {
      return null;
   }

   /**
    * {@inheritDoc}
    */
   public String getPassword()
   {
      return password;
   }

   /**
    * {@inheritDoc}
    */
   public String getUserName()
   {
      return userName;
   }

   /**
    * {@inheritDoc}
    */
   public void setCreatedDate(Date t)
   {
      createdDate = t;
   }

   /**
    * {@inheritDoc}
    */
   public void setEmail(String s)
   {
      email = s;
   }

   /**
    * {@inheritDoc}
    */
   public void setDisplayName(String displayName)
   {
      this.displayName = displayName;
   }

   /**
    * {@inheritDoc}
    */
   public void setFirstName(String s)
   {
      firstName = s;
   }

   /**
    * {@inheritDoc}
    */
   public void setFullName(String s)
   {
      setDisplayName(s);
   }

   /**
    * {@inheritDoc}
    */
   public void setLastLoginTime(Date t)
   {
      lastLoginTime = t;
   }

   /**
    * {@inheritDoc}
    */
   public void setLastName(String s)
   {
      lastName = s;
   }

   /**
    * {@inheritDoc}
    */
   public void setOrganizationId(String s)
   {
   }

   /**
    * {@inheritDoc}
    */
   public void setPassword(String s)
   {
      password = s;
   }

   /**
    * {@inheritDoc}
    */
   public void setUserName(String s)
   {
      userName = s;
   }

   /**
    * Set user UUId.
    */
   void setUUId(String UUid)
   {
      this.UUId = UUid;
   }

   /**
    * Get user UUId.
    * 
    * @return UUId of the user in the storage
    */
   public String getUUId()
   {
      return UUId;
   }

   /**
    * {@inheritDoc}
    */
   public String toString()
   {
      return "[user=" + getUserName() + "]";
   }

   /**
    * {@inheritDoc}
    */
   public UserImpl clone()
   {
      UserImpl ui;
      try
      {
         ui = (UserImpl)super.clone();
         if (createdDate != null)
         {
            ui.createdDate = (Date)createdDate.clone();
         }
         if (lastLoginTime != null)
         {
            ui.lastLoginTime = (Date)lastLoginTime.clone();
         }
      }
      catch (CloneNotSupportedException e)
      {
         return this;
      }

      return ui;
   }

}

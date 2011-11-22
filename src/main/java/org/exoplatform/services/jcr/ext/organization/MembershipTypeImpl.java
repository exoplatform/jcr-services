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
import org.exoplatform.services.organization.MembershipType;

import java.util.Date;

/**
 * Created by The eXo Platform SAS.
 * 
 * Date: 24.07.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id$
 */
public class MembershipTypeImpl implements MembershipType, ExtendedCloneable
{

   /**
    * The description of the membership type
    */
   private String description;

   /**
    * The name of the membership type
    */
   private String name;

   /**
    * The UUID of the membership type in the storage
    */
   private String UUId;

   /**
    * MembershipTypeImpl constructor.
    */
   MembershipTypeImpl()
   {
      this.UUId = null;
   }

   /**
    * MembershipTypeImpl constructor.
    * 
    * @param UUId
    *          - membership node id
    */
   MembershipTypeImpl(String UUId)
   {
      this.UUId = UUId;
   }

   /**
    * MembershipTypeImpl constructor.
    */
   MembershipTypeImpl(String UUId, String name, String description)
   {
      this.UUId = UUId;
      this.name = name;
      this.description = description;
   }

   /**
    * Get the date when the membership type was created to the database.
    * 
    * @deprecated This method is not used.
    * @return The date that the membership type was created to the database
    */
   public Date getCreatedDate()
   {
      return null;
   }

   /**
    * {@inheritDoc}
    */
   public String getDescription()
   {
      return description;
   }

   /**
    * Get the date when membership type was modified last time.
    * 
    * @deprecated This method is not used.
    * @return The last time that an user modify the data of the membership type.
    */
   public Date getModifiedDate()
   {
      return null;
   }

   /**
    * {@inheritDoc}
    */
   public String getName()
   {
      return name;
   }

   /**
    * Get owner of the membership type.
    * 
    * @deprecated This method is not used.
    * @return The owner of the membership type
    */
   public String getOwner()
   {
      return null;
   }

   /**
    * Get UUId of the membership type in the storage.
    * 
    * @return The UUID of the membership type in the storage
    */
   public String getUUId()
   {
      return UUId;
   }

   /**
    * Set UUId of the membership type.
    */
   void setUUId(String UUId)
   {
      this.UUId = UUId;
   }

   /**
    * Set date creation of the membership type.
    * 
    * @deprecated This method is not used.
    * @param d
    *          The created date
    */
   public void setCreatedDate(Date d)
   {
   }

   /**
    * {@inheritDoc}
    */
   public void setDescription(String s)
   {
      description = s;
   }

   /**
    * Set date modification of the membership type.
    * 
    * @deprecated This method is not used.
    * @param d
    *          The modified date
    */
   public void setModifiedDate(Date d)
   {
   }

   /**
    * Set the new name for membership type.
    * 
    * @param s
    *          The name of the membership type
    */
   public void setName(String s)
   {
      name = s;
   }

   /**
    * Set the owner for membership type.
    * 
    * @deprecated This method is not used.
    * @param s
    *          The new owner of the membership type
    */
   public void setOwner(String s)
   {
   }

   /**
    * {@inheritDoc}
    */
   public String toString()
   {
      return "[type=" + getName() + "]";
   }

   /**
    * {@inheritDoc}
    */
   public MembershipTypeImpl clone()
   {
      try
      {
         return (MembershipTypeImpl)super.clone();
      }
      catch (CloneNotSupportedException e)
      {
         return this;
      }
   }
}

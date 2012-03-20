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

import java.util.Date;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 7 10 2008
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: ReadHandler.java 111 2008-11-11 11:11:11Z $
 */
public abstract class CommonHandler {

  /**
   * This method read property data from node in the storage.
   * 
   * @param node
   *          The node to read from
   * @param prop
   *          The property name which need to read
   * @return The property data as date if exist and null if not
   * @throws Exception
   *           An exception is thrown if the method cannot access the database
   */
  public Date readDateProperty(Node node, String prop) throws Exception {
    try {
      return node.getProperty(prop).getDate().getTime();
    } catch (PathNotFoundException e) {
      return null;
    } catch (RepositoryException e) {
      throw new OrganizationServiceException("Can not read property " + prop, e);
    }
  }

  /**
   * This method read property data.
   * 
   * @param node
   *          The node to read from
   * @param prop
   *          The property name which need to read
   * @return The property data as string if exist and null if not
   * @throws Exception
   *           An exception is thrown if the method cannot access the database
   */
  public String readStringProperty(Node node, String prop) throws Exception {
    try {
      return node.getProperty(prop).getString();
    } catch (PathNotFoundException e) {
      return null;
    } catch (RepositoryException e) {
      throw new OrganizationServiceException("Can not read property " + prop, e);
    }
  }
}

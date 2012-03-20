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

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 22.08.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id$
 */
public class OrganizationServiceException extends Exception {

  /**
   * OrganizationServiceException constructor.
   * 
   * @param message
   *          The message to display
   * @param cause
   *          The cause of exception
   */
  public OrganizationServiceException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * OrganizationServiceException constructor.
   * 
   * @param message
   *          The message to display
   */
  public OrganizationServiceException(String message) {
    super(message);
  }

}

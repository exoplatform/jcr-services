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
package org.exoplatform.services.jcr.ext.audit;

import javax.jcr.Node;

import org.apache.commons.chain.Context;
import org.exoplatform.services.log.Log;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.services.command.action.Action;
import org.exoplatform.services.jcr.impl.core.ItemImpl;
import org.exoplatform.services.jcr.observation.ExtendedEventType;
import org.exoplatform.services.log.ExoLogger;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class AddAuditableAction implements Action {

  private final Log log = ExoLogger.getLogger("jcr.AddAuditableAction");

  public boolean execute(Context ctx) throws Exception {

    ItemImpl item = (ItemImpl) ctx.get("currentItem");
    int event = (Integer) ctx.get("event");

    Node node;
    if (item.isNode())
      node = (Node) item;
    else
      node = item.getParent();

    AuditService auditService = (AuditService) ((ExoContainer) ctx.get("exocontainer")).getComponentInstanceOfType(AuditService.class);
    if (node.canAddMixin("exo:auditable")) {
      node.addMixin("exo:auditable");
      if (log.isDebugEnabled()) {
        log.debug("exo:auditable adedd for " + node.getPath());
      }
    }
    if (node.isNodeType("exo:auditable")) {
      if (!auditService.hasHistory(node)) {

        auditService.createHistory(node);
        if (log.isDebugEnabled()) {
          log.debug("Audit history created for " + node.getPath());
        }

      }

      auditService.addRecord(item, event);
      if (log.isDebugEnabled()) {
        log.debug("Record '" + ExtendedEventType.nameFromValue(event) + "' added for "
            + item.getPath());
      }
      return true;
    }
    return false;
  }

}

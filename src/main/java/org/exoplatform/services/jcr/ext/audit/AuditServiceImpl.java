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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.StringTokenizer;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.exoplatform.services.log.Log;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.access.SystemIdentity;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ExtendedPropertyType;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedPropertyData;
import org.exoplatform.services.jcr.datamodel.Identifier;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.registry.RegistryEntry;
import org.exoplatform.services.jcr.ext.registry.RegistryService;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.ItemImpl;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.dataflow.ValueDataConvertor;
import org.exoplatform.services.jcr.impl.dataflow.session.SessionChangesLog;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.picocontainer.Startable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author Gennady Azarenkov
 * @version $Id: AuditServiceImpl.java 14416 2008-05-16 13:04:06Z pnedonosko $
 */

public class AuditServiceImpl implements AuditService, Startable {

  /**
   * The name of parameter that contain admin indentity.
   */
  private static final String ADMIN_INDENTITY = "adminIdentity";

  /**
   * Contain passed value of admin indentity in parameters.
   */
  private String              adminIdentity;

  /**
   * Initialization parameters.
   */
  private InitParams          initParams;

  /**
   * RegistryService.
   */
  private RegistryService     registryService;

  /**
   * RepositoryService.
   */
  private RepositoryService   repositoryService;

  /**
   * Logger.
   */
  private static Log          log             = ExoLogger.getLogger("jcr.AuditService");

  private List<String>        adminIdentitys  = null;

  /**
   * The service's name.
   */
  private static final String SERVICE_NAME    = "Audit";

  /**
   * AuditServiceImpl constructor.
   * 
   * @param initParams
   * @param repService
   * @throws RepositoryConfigurationException
   */
  public AuditServiceImpl(InitParams initParams, RepositoryService repService) throws RepositoryConfigurationException {
    this(initParams, repService, null);
  }

  /**
   * AuditServiceImpl constructor.
   * 
   * @param initParams
   * @param repService
   * @param registryService
   * @throws RepositoryConfigurationException
   */
  public AuditServiceImpl(InitParams initParams,
                          RepositoryService repService,
                          RegistryService registryService) throws RepositoryConfigurationException {
    this.initParams = initParams;
    this.registryService = registryService;
    this.repositoryService = repService;
  }

  /**
   * {@inheritDoc}
   */
  public void start() {
    if (registryService != null && !registryService.getForceXMLConfigurationValue(initParams)) {
      SessionProvider sessionProvider = SessionProvider.createSystemProvider();
      try {
        readParamsFromRegistryService(sessionProvider);
      } catch (Exception e) {
        readParamsFromFile();
        try {
          writeParamsToRegistryService(sessionProvider);
        } catch (Exception exc) {
          log.error("Cannot write init configuration to RegistryService.", exc);
        }
      } finally {
        sessionProvider.close();
      }
    } else {
      readParamsFromFile();
    }
  }

  public void addRecord(Item item, int eventType) throws RepositoryException {

    checkIfAuditable(item);

    AuditSession auditSession = new AuditSession(item);
    SessionImpl session = (SessionImpl) item.getSession();

    SessionDataManager dataManager = auditSession.getDataManager();

    NodeData auditHistory = auditSession.getAuditHistoryNodeData();
    if (auditHistory == null)
      throw new PathNotFoundException("Audit history not found for " + item.getPath());

    if (auditHistory == null) {
      throw new RepositoryException("Audit history for  " + item.getPath() + "not found");
    }

    // make path to the AUDITHISTORY_LASTRECORD property
    QPath path = QPath.makeChildPath(auditHistory.getQPath(),
                                     AuditService.EXO_AUDITHISTORY_LASTRECORD);
    // searching last name of node
    PropertyData pData = (PropertyData) dataManager.getItemData(path);
    String auditRecordName;
    try {
      auditRecordName = String.valueOf((int) ValueDataConvertor.readLong(pData.getValues().get(0)) + 1);
    } catch (Exception e) {
      throw new RepositoryException("Error on add audit record. Problem in calculating new record name. "
          + e.getLocalizedMessage());
    }

    // exo:auditRecord
    List<AccessControlEntry> access = new ArrayList<AccessControlEntry>();
    access.add(new AccessControlEntry(SystemIdentity.ANY, PermissionType.SET_PROPERTY));
    access.add(new AccessControlEntry(SystemIdentity.ANY, PermissionType.READ));

    for (String identity : adminIdentitys) {
      access.add(new AccessControlEntry(identity, PermissionType.REMOVE));
    }

    AccessControlList exoAuditRecordAccessControlList = new AccessControlList(session.getUserID(),
                                                                              access);

    TransientNodeData arNode = new TransientNodeData(QPath.makeChildPath(auditHistory.getQPath(),
                                                                         new InternalQName(null,
                                                                                           auditRecordName)),
                                                     IdGenerator.generate(),
                                                     -1,
                                                     AuditService.EXO_AUDITRECORD,
                                                     new InternalQName[0],
                                                     Integer.parseInt(auditRecordName),
                                                     auditHistory.getIdentifier(),
                                                     exoAuditRecordAccessControlList);

    // exo:auditRecord
    dataManager.update(new ItemState(arNode,
                                     ItemState.ADDED,
                                     true,
                                     ((ItemImpl) item).getInternalPath()), true);

    // jcr:primaryType
    TransientPropertyData arPrType = TransientPropertyData.createPropertyData(arNode,
                                                                              Constants.JCR_PRIMARYTYPE,
                                                                              PropertyType.NAME,
                                                                              false,
                                                                              new TransientValueData(arNode.getPrimaryTypeName()));
    // exo:user
    TransientPropertyData arUser = TransientPropertyData.createPropertyData(arNode,
                                                                            AuditService.EXO_AUDITRECORD_USER,
                                                                            PropertyType.STRING,
                                                                            false,
                                                                            new TransientValueData(session.getUserID()));
    // exo:created
    TransientPropertyData arCreated = TransientPropertyData.createPropertyData(arNode,
                                                                               AuditService.EXO_AUDITRECORD_CREATED,
                                                                               PropertyType.DATE,
                                                                               false,
                                                                               new TransientValueData(dataManager.getTransactManager()
                                                                                                                 .getStorageDataManager()
                                                                                                                 .getCurrentTime()));
    // exo:eventType
    TransientPropertyData arEventType = TransientPropertyData.createPropertyData(arNode,
                                                                                 AuditService.EXO_AUDITRECORD_EVENTTYPE,
                                                                                 PropertyType.LONG,
                                                                                 false,
                                                                                 new TransientValueData(eventType));

    // jcr:primaryType
    dataManager.update(new ItemState(arPrType,
                                     ItemState.ADDED,
                                     true,
                                     ((ItemImpl) item).getInternalPath()), true);

    // exo:user
    dataManager.update(new ItemState(arUser,
                                     ItemState.ADDED,
                                     true,
                                     ((ItemImpl) item).getInternalPath()), true);

    // exo:created
    dataManager.update(new ItemState(arCreated,
                                     ItemState.ADDED,
                                     true,
                                     ((ItemImpl) item).getInternalPath()), true);

    // exo:eventType
    dataManager.update(new ItemState(arEventType,
                                     ItemState.ADDED,
                                     true,
                                     ((ItemImpl) item).getInternalPath()), true);

    NodeData vancestor; // nearest versionable ancestor
    if (item.isNode()) {
      vancestor = ((NodeImpl) item).getVersionableAncestor();
    } else {
      vancestor = ((NodeImpl) ((Property) item).getParent()).getVersionableAncestor();

      // exo:propertyName
      TransientPropertyData propertyNameData = TransientPropertyData.createPropertyData(arNode,
                                                                                        EXO_AUDITRECORD_PROPERTYNAME,
                                                                                        PropertyType.STRING,
                                                                                        false,
                                                                                        new TransientValueData(((ItemImpl) item).getInternalName()));
      dataManager.update(new ItemState(propertyNameData,
                                       ItemState.ADDED,
                                       true,
                                       ((ItemImpl) item).getInternalPath()), true);
    }

    if (vancestor != null) {
      // auditale node under a version control, set related properties to the
      // audit record

      String versionUUID; // currebt base version UUID
      String versionName; // currebt base version name + labels

      PropertyData bvProp = (PropertyData) dataManager.getItemData(vancestor,
                                                                   new QPathEntry(Constants.JCR_BASEVERSION,
                                                                                  1));
      try {
        versionUUID = ValueDataConvertor.readString(bvProp.getValues().get(0));

        // using JCR API objects
        Version version = (Version) dataManager.getItemByIdentifier(versionUUID, false);
        versionName = version.getName();

        VersionHistory versionHistory = (VersionHistory) dataManager.getItemByIdentifier(version.getParent()
                                                                                                .getUUID(),
                                                                                         false);
        String[] labels = versionHistory.getVersionLabels(version);
        for (int i = 0; i < labels.length; i++) {
          String vl = labels[i];
          if (i == 0)
            versionName += " ";

          versionName += "'" + vl + "' ";
        }
      } catch (IOException e) {
        throw new RepositoryException("Can't read jcr:baseVersion property, error " + e, e);
      }

      TransientPropertyData auditVersion = TransientPropertyData.createPropertyData(arNode,
                                                                                    EXO_AUDITRECORD_AUDITVERSION,
                                                                                    PropertyType.STRING,
                                                                                    false,
                                                                                    new TransientValueData(versionUUID));

      TransientPropertyData auditVersionName = TransientPropertyData.createPropertyData(arNode,
                                                                                        EXO_AUDITRECORD_AUDITVERSIONNAME,
                                                                                        PropertyType.STRING,
                                                                                        false,
                                                                                        new TransientValueData(versionName));

      dataManager.update(new ItemState(auditVersion,
                                       ItemState.ADDED,
                                       true,
                                       ((ItemImpl) item).getInternalPath()), true);
      dataManager.update(new ItemState(auditVersionName,
                                       ItemState.ADDED,
                                       true,
                                       ((ItemImpl) item).getInternalPath()), true);
    }

    // Update lastRecord
    PropertyData pLastRecord = (PropertyData) auditSession.getDataManager()
                                                          .getItemData(QPath.makeChildPath(auditHistory.getQPath(),
                                                                                           EXO_AUDITHISTORY_LASTRECORD));

    pLastRecord = new TransientPropertyData(pLastRecord.getQPath(),
                                            pLastRecord.getIdentifier(),
                                            pLastRecord.getPersistedVersion(),
                                            pLastRecord.getType(),
                                            pLastRecord.getParentIdentifier(),
                                            pLastRecord.isMultiValued(),
                                            new TransientValueData(String.valueOf(auditRecordName)));

    dataManager.update(new ItemState(pLastRecord,
                                     ItemState.UPDATED,
                                     true,
                                     ((ItemImpl) item).getInternalPath()), true);

    if (log.isDebugEnabled())
      log.debug("Add audit record: " + " Item path="
          + ((ItemImpl) item).getLocation().getInternalPath().getAsString() + " User="
          + session.getUserID() + " EventType=" + eventType);
  }

  public void createHistory(Node node) throws RepositoryException {

    checkIfAuditable(node);

    AuditSession auditSession = new AuditSession(node);
    NodeData storage = auditSession.getAuditStorage();

    // nodeData: /exo:audit/itemUUID
    // its primaryType exo:auditHistory
    // exo:targetNode (ref to item)
    // exo:lastRecord = "0"
    // in itemData/auditHistory - pointer to history (UUID)

    SessionImpl session = (SessionImpl) node.getSession();

    InternalQName aiName = new InternalQName(null, ((ItemImpl) node).getData().getIdentifier());
    // exo:auditHistory
    List<AccessControlEntry> access = new ArrayList<AccessControlEntry>();
    access.add(new AccessControlEntry(SystemIdentity.ANY, PermissionType.ADD_NODE));
    access.add(new AccessControlEntry(SystemIdentity.ANY, PermissionType.READ));
    access.add(new AccessControlEntry(SystemIdentity.ANY, PermissionType.SET_PROPERTY));

    for (String identity : adminIdentitys) {
      access.add(new AccessControlEntry(identity, PermissionType.REMOVE));
    }

    AccessControlList exoAuditHistoryAccessControlList = new AccessControlList(session.getUserID(),
                                                                               access);

    TransientNodeData ahNode = new TransientNodeData(QPath.makeChildPath(storage.getQPath(), aiName),
                                                     IdGenerator.generate(),
                                                     -1,
                                                     AuditService.EXO_AUDITHISTORY,
                                                     new InternalQName[] {
                                                         Constants.MIX_REFERENCEABLE,
                                                         Constants.EXO_PRIVILEGEABLE },
                                                     0,
                                                     storage.getIdentifier(),
                                                     exoAuditHistoryAccessControlList);

    // jcr:primaryType
    TransientPropertyData aPrType = TransientPropertyData.createPropertyData(ahNode,
                                                                             Constants.JCR_PRIMARYTYPE,
                                                                             PropertyType.NAME,
                                                                             false,
                                                                             new TransientValueData(ahNode.getPrimaryTypeName()));
    // jcr:uuid
    TransientPropertyData ahUuid = TransientPropertyData.createPropertyData(ahNode,
                                                                            Constants.JCR_UUID,
                                                                            PropertyType.STRING,
                                                                            false,
                                                                            new TransientValueData(ahNode.getIdentifier()));
    // jcr:mixinTypes
    List<ValueData> mixValues = new ArrayList<ValueData>();
    mixValues.add(new TransientValueData(Constants.MIX_REFERENCEABLE));
    mixValues.add(new TransientValueData(Constants.EXO_PRIVILEGEABLE));

    TransientPropertyData ahMixinTypes = TransientPropertyData.createPropertyData(ahNode,
                                                                                  Constants.JCR_MIXINTYPES,
                                                                                  PropertyType.NAME,
                                                                                  true,
                                                                                  mixValues);

    // EXO_PERMISSIONS
    List<ValueData> permsValues = new ArrayList<ValueData>();
    for (int i = 0; i < ahNode.getACL().getPermissionEntries().size(); i++) {
      AccessControlEntry entry = ahNode.getACL().getPermissionEntries().get(i);
      permsValues.add(new TransientValueData(entry));
    }
    TransientPropertyData exoAuditPerms = TransientPropertyData.createPropertyData(ahNode,
                                                                                   Constants.EXO_PERMISSIONS,
                                                                                   ExtendedPropertyType.PERMISSION,
                                                                                   true,
                                                                                   permsValues);

    // exo:targetNode
    TransientPropertyData ahTargetNode = TransientPropertyData.createPropertyData(ahNode,
                                                                                  AuditService.EXO_AUDITHISTORY_TARGETNODE,
                                                                                  PropertyType.REFERENCE,
                                                                                  false,
                                                                                  new TransientValueData(((ItemImpl) node).getData()
                                                                                                                          .getIdentifier()));
    // exo:lastRecord
    TransientPropertyData ahLastRecord = TransientPropertyData.createPropertyData(ahNode,
                                                                                  AuditService.EXO_AUDITHISTORY_LASTRECORD,
                                                                                  PropertyType.STRING,
                                                                                  false,
                                                                                  new TransientValueData("0"));
    // node exo:auditHistory
    TransientPropertyData pAuditHistory = TransientPropertyData.createPropertyData((NodeData) ((ItemImpl) node).getData(),
                                                                                   AuditService.EXO_AUDITHISTORY,
                                                                                   PropertyType.STRING,
                                                                                   false,
                                                                                   new TransientValueData(new Identifier(ahNode.getIdentifier())));
    session.getTransientNodesManager().update(new ItemState(ahNode,
                                                            ItemState.ADDED,
                                                            true,
                                                            ((ItemImpl) node).getInternalPath()),
                                              true);

    session.getTransientNodesManager().update(new ItemState(aPrType,
                                                            ItemState.ADDED,
                                                            true,
                                                            ((ItemImpl) node).getInternalPath()),
                                              true);

    session.getTransientNodesManager().update(new ItemState(ahUuid,
                                                            ItemState.ADDED,
                                                            true,
                                                            ((ItemImpl) node).getInternalPath()),
                                              true);

    session.getTransientNodesManager().update(new ItemState(ahMixinTypes,
                                                            ItemState.ADDED,
                                                            true,
                                                            ((ItemImpl) node).getInternalPath()),
                                              true);

    session.getTransientNodesManager().update(new ItemState(exoAuditPerms,
                                                            ItemState.ADDED,
                                                            true,
                                                            ((ItemImpl) node).getInternalPath()),
                                              true);

    session.getTransientNodesManager().update(new ItemState(ahTargetNode,
                                                            ItemState.ADDED,
                                                            true,
                                                            ((ItemImpl) node).getInternalPath()),
                                              true);

    session.getTransientNodesManager().update(new ItemState(ahLastRecord,
                                                            ItemState.ADDED,
                                                            true,
                                                            ((ItemImpl) node).getInternalPath()),
                                              true);

    session.getTransientNodesManager().update(new ItemState(pAuditHistory,
                                                            ItemState.ADDED,
                                                            true,
                                                            ((ItemImpl) node).getInternalPath()),
                                              true);

  }

  public AuditHistory getHistory(Node node) throws RepositoryException,
                                           UnsupportedOperationException {

    // get history for this item and create AuditHistory object
    AuditSession auditSession = new AuditSession(node);
    SessionDataManager dm = auditSession.getDataManager();
    NodeData auditHistory = auditSession.getAuditHistoryNodeData();
    if (auditHistory != null) {

      List<AuditRecord> auditRecords = new ArrayList<AuditRecord>();
      // AuditRecord aRecord = null;
      ValueFactoryImpl vf = (ValueFactoryImpl) node.getSession().getValueFactory();
      // Search all auditRecords
      List<NodeData> auditRecordsNodeData = dm.getChildNodesData(auditHistory);
      for (NodeData nodeData : auditRecordsNodeData) {
        // Serching properties
        List<PropertyData> auditRecordNodeData = dm.getChildPropertiesData(nodeData);
        // define variables
        String user = null;
        InternalQName propertyName = null;
        int eventType = -1;
        Calendar date = null;
        // version stuff
        String version = null;
        String versionName = null;
        // loading data
        try {
          for (PropertyData propertyData : auditRecordNodeData) {
            ValueData value = propertyData.getValues().get(0);
            if (propertyData.getQPath().getName().equals(AuditService.EXO_AUDITRECORD_USER)) {
              user = ValueDataConvertor.readString(value);
            } else if (propertyData.getQPath()
                                   .getName()
                                   .equals(AuditService.EXO_AUDITRECORD_EVENTTYPE)) {
              eventType = (int) ValueDataConvertor.readLong(value);
            } else if (propertyData.getQPath()
                                   .getName()
                                   .equals(AuditService.EXO_AUDITRECORD_CREATED)) {
              date = ValueDataConvertor.readDate(value);
            } else if (propertyData.getQPath()
                                   .getName()
                                   .equals(AuditService.EXO_AUDITRECORD_PROPERTYNAME)) {
              propertyName = InternalQName.parse(ValueDataConvertor.readString(value));
            } else if (propertyData.getQPath()
                                   .getName()
                                   .equals(AuditService.EXO_AUDITRECORD_AUDITVERSION)) {
              version = ValueDataConvertor.readString(value);
            } else if (propertyData.getQPath()
                                   .getName()
                                   .equals(AuditService.EXO_AUDITRECORD_AUDITVERSIONNAME)) {
              versionName = ValueDataConvertor.readString(value);
            }
          }
        } catch (UnsupportedEncodingException e) {
          throw new RepositoryException(e);
        } catch (IllegalStateException e) {
          throw new RepositoryException(e);
        } catch (IOException e) {
          throw new RepositoryException(e);
        } catch (IllegalNameException e) {
          throw new RepositoryException(e);
        }
        // add audit record
        auditRecords.add(new AuditRecord(user, eventType, date, propertyName, version, versionName));
      }
      return new AuditHistory(node, auditRecords);

    } else
      throw new PathNotFoundException("Audit history not found for " + node.getPath());
  }

  public boolean hasHistory(Node node) {
    NodeData data;
    try {
      AuditSession auditSession = new AuditSession(node);
      data = auditSession.getAuditHistoryNodeData();
    } catch (RepositoryException e) {
      try {
        if (log.isDebugEnabled())
          log.debug("Audit history for " + node.getPath() + " not accessible due to error " + e, e);
      } catch (RepositoryException e1) {
        log.error("Can't read node path for " + node, e1);
      }
      return false;
    }
    return (data == null) ? false : true;
  }

  public void removeHistory(Node node) throws RepositoryException {
    AuditSession auditSession = new AuditSession(node);
    NodeData auditHistory = auditSession.getAuditHistoryNodeData();
    // remove /jcr:system/exo:auditStorage/itemID
    // (delete in SessionDataManager)
    if (auditHistory != null) {
      SessionImpl session = (SessionImpl) node.getSession();
      session.getTransientNodesManager().delete(auditHistory);
    } else
      throw new PathNotFoundException("Audit history not found for " + node.getPath());
  }

  private void checkIfAuditable(Item item) throws RepositoryException,
                                          UnsupportedOperationException {
    NodeImpl node = (item.isNode()) ? (NodeImpl) item : (NodeImpl) item.getParent();
    if (!node.isNodeType("exo:auditable"))
      throw new ConstraintViolationException("exo:auditable node expected at: " + node.getPath());
  }

  private class AuditSession {

    private final SessionImpl        session;

    private final SessionDataManager dataManager;

    private ExtendedNode             node;

    private AuditSession(Item item) throws RepositoryException {
      session = (SessionImpl) item.getSession();
      if (item.isNode())
        node = (ExtendedNode) item;
      else
        node = (ExtendedNode) item.getParent();
      if (!node.isNodeType(AuditService.EXO_AUDITABLE))
        throw new RepositoryException("Node is not exo:auditable " + node.getPath());

      dataManager = session.getTransientNodesManager();
    }

    private NodeData getAuditHistoryNodeData() throws RepositoryException {
      // searching uuid of corresponding EXO_AUDITHISTORY node
      PropertyData pData = (PropertyData) dataManager.getItemData((NodeData) ((NodeImpl) node).getData(),
                                                                  new QPathEntry(AuditService.EXO_AUDITHISTORY,
                                                                                 0));
      if (pData != null)
        try {
          String ahUuid = ValueDataConvertor.readString(pData.getValues().get(0));
          return (NodeData) dataManager.getItemData(ahUuid);
        } catch (UnsupportedEncodingException e) {
          throw new RepositoryException("Error of exo:auditHistory read", e);
        } catch (IllegalStateException e) {
          throw new RepositoryException("Error of exo:auditHistory read", e);
        } catch (IOException e) {
          throw new RepositoryException("Error of exo:auditHistory read", e);
        }

      return null;
    }

    private NodeData getAuditStorage() throws RepositoryException {

      ItemData storage = session.getTransientNodesManager().getItemData(AUDIT_STORAGE_ID);

      if (storage == null) {
        SessionChangesLog changesLog = new SessionChangesLog(session.getId());

        // here should be added to TransactionalDataManager (i.e. saved
        // immediatelly!
        // nodeData: /exo:audit with UUID = AUDIT_STORAGE_ID
        // its primaryType exo:auditStorage
        List<AccessControlEntry> access = new ArrayList<AccessControlEntry>();
        access.add(new AccessControlEntry(SystemIdentity.ANY, PermissionType.ADD_NODE));

        for (String identity : adminIdentitys) {
          access.add(new AccessControlEntry(identity, PermissionType.READ));
        }

        access.add(new AccessControlEntry(SystemIdentity.ANY, PermissionType.REMOVE));

        AccessControlList exoAuditAccessControlList = new AccessControlList(SystemIdentity.SYSTEM,
                                                                            access);

        InternalQName[] mixins = new InternalQName[] { Constants.EXO_PRIVILEGEABLE,
            Constants.MIX_REFERENCEABLE };

        TransientNodeData exoAuditNode = new TransientNodeData(QPath.makeChildPath(Constants.ROOT_PATH,
                                                                                   AuditService.EXO_AUDIT),
                                                               AuditService.AUDIT_STORAGE_ID,
                                                               -1,
                                                               AuditService.EXO_AUDITSTORAGE,
                                                               mixins,
                                                               0,
                                                               Constants.ROOT_UUID,
                                                               exoAuditAccessControlList);

        // jcr:primaryType
        TransientPropertyData exoAuditPrType = TransientPropertyData.createPropertyData(exoAuditNode,
                                                                                        Constants.JCR_PRIMARYTYPE,
                                                                                        PropertyType.NAME,
                                                                                        false,
                                                                                        new TransientValueData(exoAuditNode.getPrimaryTypeName()));
        // jcr:uuid
        TransientPropertyData exoAuditUuid = TransientPropertyData.createPropertyData(exoAuditNode,
                                                                                      Constants.JCR_UUID,
                                                                                      PropertyType.STRING,
                                                                                      false,
                                                                                      new TransientValueData(exoAuditNode.getIdentifier()));
        // jcr:mixinTypes
        List<ValueData> mixValues = new ArrayList<ValueData>();
        mixValues.add(new TransientValueData(Constants.MIX_REFERENCEABLE));
        mixValues.add(new TransientValueData(Constants.EXO_PRIVILEGEABLE));

        TransientPropertyData exoAuditMixinTypes = TransientPropertyData.createPropertyData(exoAuditNode,
                                                                                            Constants.JCR_MIXINTYPES,
                                                                                            PropertyType.NAME,
                                                                                            true,
                                                                                            mixValues);
        // EXO_PERMISSIONS

        List<ValueData> permsValues = new ArrayList<ValueData>();
        for (int i = 0; i < exoAuditNode.getACL().getPermissionEntries().size(); i++) {
          AccessControlEntry entry = exoAuditNode.getACL().getPermissionEntries().get(i);
          permsValues.add(new TransientValueData(entry));
        }
        TransientPropertyData exoAuditPerms = TransientPropertyData.createPropertyData(exoAuditNode,
                                                                                       Constants.EXO_PERMISSIONS,
                                                                                       ExtendedPropertyType.PERMISSION,
                                                                                       true,
                                                                                       permsValues);

        changesLog.add(ItemState.createAddedState(exoAuditNode));
        changesLog.add(ItemState.createAddedState(exoAuditPrType));
        changesLog.add(ItemState.createAddedState(exoAuditUuid));
        changesLog.add(ItemState.createAddedState(exoAuditMixinTypes));
        changesLog.add(ItemState.createAddedState(exoAuditPerms));

        session.getTransientNodesManager().getTransactManager().save(changesLog);
        storage = session.getTransientNodesManager().getItemData(AUDIT_STORAGE_ID);
      }
      if (!storage.isNode())
        throw new RepositoryException("Item with uuid " + AUDIT_STORAGE_ID + " should be node  ");
      return (NodeData) storage;
    }

    private SessionDataManager getDataManager() {
      return dataManager;
    }
  }

  /**
   * Write parameters to RegistryService.
   * 
   * @param sessionProvider The SessionProvider
   * @throws ParserConfigurationException
   * @throws SAXException
   * @throws IOException
   * @throws RepositoryException
   */
  private void writeParamsToRegistryService(SessionProvider sessionProvider) throws IOException,
                                                                            SAXException,
                                                                            ParserConfigurationException,
                                                                            RepositoryException {

    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    Element root = doc.createElement(SERVICE_NAME);
    doc.appendChild(root);

    Element element = doc.createElement(ADMIN_INDENTITY);
    setAttributeSmart(element, "value", adminIdentity);
    root.appendChild(element);

    RegistryEntry serviceEntry = new RegistryEntry(doc);
    registryService.createEntry(sessionProvider, RegistryService.EXO_SERVICES, serviceEntry);
  }

  /**
   * Read parameters from RegistryService.
   * 
   * @param sessionProvider The SessionProvider
   * @throws RepositoryException
   * @throws PathNotFoundException
   * @throws RepositoryConfigurationException
   */
  private void readParamsFromRegistryService(SessionProvider sessionProvider) throws PathNotFoundException,
                                                                             RepositoryException,
                                                                             RepositoryConfigurationException {

    String entryPath = RegistryService.EXO_SERVICES + "/" + SERVICE_NAME + "/" + ADMIN_INDENTITY;
    RegistryEntry registryEntry = registryService.getEntry(sessionProvider, entryPath);
    Document doc = registryEntry.getDocument();
    Element element = doc.getDocumentElement();
    adminIdentity = getAttributeSmart(element, "value");

    log.info("Admin identity is read from RegistryService");

    checkParams();
  }

  /**
   * Get attribute value.
   * 
   * @param element The element to get attribute value
   * @param attr The attribute name
   * @return Value of attribute if present and null in other case
   */
  private String getAttributeSmart(Element element, String attr) {
    return element.hasAttribute(attr) ? element.getAttribute(attr) : null;
  }

  /**
   * Set attribute value. If value is null the attribute will be removed.
   * 
   * @param element The element to set attribute value
   * @param attr The attribute name
   * @param value The value of attribute
   */
  private void setAttributeSmart(Element element, String attr, String value) {
    if (value == null) {
      element.removeAttribute(attr);
    } else {
      element.setAttribute(attr, value);
    }
  }

  /**
   * Get parameters which passed from the configuration file.
   * 
   * @throws RepositoryConfigurationException
   */
  private void readParamsFromFile() {
    if (initParams != null) {
      ValueParam valParam = initParams.getValueParam(ADMIN_INDENTITY);
      if (valParam != null)
        adminIdentity = valParam.getValue();
    }

    log.info("Admin identity is read from configuration file");

    checkParams();
  }

  /**
   * Check read params and initialize.
   * 
   * @throws RepositoryConfigurationException
   */
  private void checkParams() {
    if (adminIdentity == null)
      throw new RuntimeException("Admin identity is not configured");

    StringTokenizer listTokenizer = new StringTokenizer(adminIdentity, AccessControlList.DELIMITER);

    if (listTokenizer.countTokens() < 1)
      throw new RuntimeException("AccessControlList " + adminIdentity
          + " is empty or have a bad format");

    adminIdentitys = new ArrayList<String>(listTokenizer.countTokens());

    while (listTokenizer.hasMoreTokens()) {
      adminIdentitys.add(listTokenizer.nextToken());
    }

  }

  /**
   * {@inheritDoc}
   */
  public void stop() {
  }

}

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

import org.exoplatform.container.configuration.ConfigurationException;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.registry.RegistryEntry;
import org.exoplatform.services.jcr.ext.registry.RegistryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.BaseOrganizationService;
import org.picocontainer.Startable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by The eXo Platform SAS. <br/>
 * Initialization will be performed via OrganizationServiceJCRInitializer. <br/>
 * Date: 24.07.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter
 *         Nedonosko</a>
 * @version $Id: JCROrganizationServiceImpl.java 33732 2009-07-08 15:00:43Z
 *          pnedonosko $
 */
public class JCROrganizationServiceImpl extends BaseOrganizationService implements Startable
{

   /**
    * The name of parameter that contain repository name.
    */
   public static final String REPOSITORY_NAME = "repository";

   /**
    * The name of parameter that contain storage path.
    */
   public static final String STORAGE_PATH = "storage-path";

   /**
    * The name of parameter that contain workspace name.
    */
   public static final String STORAGE_WORKSPACE = "storage-workspace";

   /**
    * Default storage path.
    */
   public static final String STORAGE_PATH_DEFAULT = "/exo:organization";

   /**
    * The service's name.
    */
   private static final String SERVICE_NAME = "JCROrganization";

   /**
    * Repository service.
    */
   protected RepositoryService repositoryService;

   /**
    * Registry service.
    */
   protected RegistryService registryService;

   /**
    * Contain passed value of storage path in parameters.
    */
   protected String storagePath;

   /**
    * Contain passed value of repository name in parameters.
    */
   protected String repositoryName;

   /**
    * Contain passed value of workspace name in parameters.
    */
   protected String storageWorkspace;

   /**
    * Cache for organization service entities.
    */
   protected final JCRCacheHandler cacheHandler;

   /**
    * Initialization parameters.
    */
   protected InitParams initParams;

   /**
    * Logger.
    */
   private static Log log = ExoLogger.getLogger("jcr.JCROrganizationService");

   /**
    * JCROrganizationServiceImpl constructor. Without registry service.
    * 
    * @param params The initialization parameters
    * @param repositoryService The repository service
    * @throws ConfigurationException The exception is thrown if can not
    *           initialize service
    */
   public JCROrganizationServiceImpl(InitParams params, RepositoryService repositoryService, CacheService cservice)
      throws ConfigurationException
   {
      this(params, repositoryService, null, cservice);
   }

   /**
    * JCROrganizationServiceImpl constructor.
    * 
    * @param initParams The initialization parameters
    * @param repositoryService The repository service
    * @param registryService The registry service
    * @throws ConfigurationException The exception is thrown if can not
    *           initialize service
    */
   public JCROrganizationServiceImpl(InitParams initParams, RepositoryService repositoryService,
      RegistryService registryService, CacheService cservice) throws ConfigurationException
   {
      this.repositoryService = repositoryService;
      this.registryService = registryService;
      this.cacheHandler = new JCRCacheHandler(cservice);

      if (initParams == null)
      {
         throw new ConfigurationException("Initialization parameters expected !!!");
      }

      this.initParams = initParams;

      // create DAO object
      userDAO_ = new UserHandlerImpl(this);
      userProfileDAO_ = new UserProfileHandlerImpl(this);
      groupDAO_ = new GroupHandlerImpl(this);
      membershipDAO_ = new MembershipHandlerImpl(this);
      membershipTypeDAO_ = new MembershipTypeHandlerImpl(this);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void start()
   {
      if (log.isDebugEnabled())
      {
         log.debug("Starting JCROrganizationService");
      }

      if (registryService != null && !registryService.getForceXMLConfigurationValue(initParams))
      {
         SessionProvider sessionProvider = SessionProvider.createSystemProvider();
         try
         {
            readParamsFromRegistryService(sessionProvider);
         }
         catch (Exception e)
         {
            readParamsFromFile();
            try
            {
               writeParamsToRegistryService(sessionProvider);
            }
            catch (Exception exc)
            {
               log.error("Cannot write init configuration to RegistryService.", exc);
            }
         }
         finally
         {
            sessionProvider.close();
         }
      }
      else
      {
         readParamsFromFile();
      }

      checkParams();

      // create /exo:organization
      try
      {
         Session session = getStorageSession();
         try
         {
            session.getItem(this.storagePath);
            // if found do nothing, the storage was initialized before.
         }
         catch (PathNotFoundException e)
         {
            // will create new
            Node storage = session.getRootNode().addNode(storagePath.substring(1), "jos:organizationStorage");

            storage.addNode(UserHandlerImpl.STORAGE_JOS_USERS, "jos:organizationUsers");
            storage.addNode(GroupHandlerImpl.STORAGE_JOS_GROUPS, "jos:organizationGroups");
            storage.addNode(MembershipTypeHandlerImpl.STORAGE_JOS_MEMBERSHIP_TYPES, "jos:organizationMembershipTypes");

            session.save(); // storage done configure

         }
         finally
         {
            session.logout();
         }
      }
      catch (Exception e)
      {
         throw new RuntimeException("Can not configure storage", e);
      }

      super.start();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void stop()
   {
      super.stop();
   }

   /**
    * Returns cache.
    */
   JCRCacheHandler getCacheHandler()
   {
      return cacheHandler;
   }

   /**
    * Return org-service actual storage path.
    * 
    * @return org-service storage path
    * @throws RepositoryException if any Exception is occurred
    */
   String getStoragePath() throws RepositoryException
   {
      if (storagePath == null)
      {
         throw new RepositoryException("Can not get storage path because JCROrganizationService is not started");
      }

      return storagePath;
   }

   /**
    * Return system Session to org-service storage workspace. For internal use
    * only.
    * 
    * @return system session
    * @throws RepositoryException if any Exception is occurred
    */
   Session getStorageSession() throws RepositoryException
   {
      try
      {
         ManageableRepository repository = getWorkingRepository();

         String workspaceName = storageWorkspace;
         if (workspaceName == null)
         {
            workspaceName = repository.getConfiguration().getDefaultWorkspaceName();
         }

         return repository.getSystemSession(workspaceName);
      }
      catch (NullPointerException e)
      {
         throw new RepositoryException("Can not get system session because JCROrganizationService is not started", e);
      }
      catch (RepositoryConfigurationException e)
      {
         throw new RepositoryException("Can not get system session", e);
      }
   }

   /**
    * Read parameters from RegistryService.
    * 
    * @param sessionProvider The SessionProvider
    * @throws RepositoryException if any Exception is occurred
    */
   private void readParamsFromRegistryService(SessionProvider sessionProvider) throws PathNotFoundException,
      RepositoryException
   {

      String entryPath = RegistryService.EXO_SERVICES + "/" + SERVICE_NAME + "/" + REPOSITORY_NAME;
      RegistryEntry registryEntry = registryService.getEntry(sessionProvider, entryPath);
      Document doc = registryEntry.getDocument();
      Element element = doc.getDocumentElement();
      repositoryName = getAttributeSmart(element, "value");

      entryPath = RegistryService.EXO_SERVICES + "/" + SERVICE_NAME + "/" + STORAGE_PATH;
      registryEntry = registryService.getEntry(sessionProvider, entryPath);
      doc = registryEntry.getDocument();
      element = doc.getDocumentElement();
      storagePath = getAttributeSmart(element, "value");

      entryPath = RegistryService.EXO_SERVICES + "/" + SERVICE_NAME + "/" + STORAGE_WORKSPACE;
      registryEntry = registryService.getEntry(sessionProvider, entryPath);
      doc = registryEntry.getDocument();
      element = doc.getDocumentElement();
      storageWorkspace = getAttributeSmart(element, "value");

      if (repositoryName != null)
      {
         log.info("Repository from RegistryService: " + repositoryName);
      }

      if (storageWorkspace != null)
      {
         log.info("Workspace from RegistryService: " + storageWorkspace);
      }

      if (storagePath != null)
      {
         log.info("Root node from RegistryService: " + storagePath);
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
   private void writeParamsToRegistryService(SessionProvider sessionProvider) throws IOException, SAXException,
      ParserConfigurationException, RepositoryException
   {

      Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      Element root = doc.createElement(SERVICE_NAME);
      doc.appendChild(root);

      Element element = doc.createElement(REPOSITORY_NAME);
      setAttributeSmart(element, "value", repositoryName);
      root.appendChild(element);

      element = doc.createElement(STORAGE_PATH);
      setAttributeSmart(element, "value", storagePath);
      root.appendChild(element);

      element = doc.createElement(STORAGE_WORKSPACE);
      setAttributeSmart(element, "value", storageWorkspace);
      root.appendChild(element);

      RegistryEntry serviceEntry = new RegistryEntry(doc);
      registryService.createEntry(sessionProvider, RegistryService.EXO_SERVICES, serviceEntry);
   }

   /**
    * Read parameters from file.
    */
   private void readParamsFromFile()
   {
      ValueParam paramRepository = initParams.getValueParam(REPOSITORY_NAME);
      repositoryName = paramRepository != null ? paramRepository.getValue() : null;

      ValueParam paramStoragePath = initParams.getValueParam(STORAGE_PATH);
      storagePath = paramStoragePath != null ? paramStoragePath.getValue() : null;

      ValueParam paramStorageWorkspace = initParams.getValueParam(STORAGE_WORKSPACE);
      storageWorkspace = paramStorageWorkspace != null ? paramStorageWorkspace.getValue() : null;

      if (repositoryName != null)
      {
         log.info("Repository from configuration file: " + repositoryName);
      }

      if (storageWorkspace != null)
      {
         log.info("Workspace from configuration file: " + storageWorkspace);
      }

      if (storagePath != null)
      {
         log.info("Root node from configuration file: " + storagePath);
      }
   }

   /**
    * Get attribute value.
    * 
    * @param element The element to get attribute value
    * @param attr The attribute name
    * @return Value of attribute if present and null in other case
    */
   private String getAttributeSmart(Element element, String attr)
   {
      return element.hasAttribute(attr) ? element.getAttribute(attr) : null;
   }

   /**
    * Set attribute value. If value is null the attribute will be removed.
    * 
    * @param element The element to set attribute value
    * @param attr The attribute name
    * @param value The value of attribute
    */
   private void setAttributeSmart(Element element, String attr, String value)
   {
      if (value == null)
      {
         element.removeAttribute(attr);
      }
      else
      {
         element.setAttribute(attr, value);
      }
   }

   /**
    * Check read params and initialize.
    */
   private void checkParams()
   {
      // path
      if (storagePath != null)
      {
         if (storagePath.equals("/"))
         {
            throw new RuntimeException("Storage path can not be a root node");
         }
      }
      else
      {
         this.storagePath = STORAGE_PATH_DEFAULT;
      }
   }

   private ManageableRepository getWorkingRepository() throws RepositoryException, RepositoryConfigurationException
   {
      return repositoryName != null ? repositoryService.getRepository(repositoryName) : repositoryService
         .getCurrentRepository();
   }
}

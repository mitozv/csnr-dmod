package ca.bc.nrs.dm.microservice.api.impl;

import ca.bc.nrs.dm.microservice.api.DocumentsApiService;
import java.io.InputStream;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import ca.bc.gov.nrs.dm.rest.client.v1.DocumentManagementException;
import ca.bc.gov.nrs.dm.rest.client.v1.DocumentManagementService;
import ca.bc.gov.nrs.dm.rest.client.v1.ForbiddenAccessException;
import ca.bc.gov.nrs.dm.rest.client.v1.impl.DocumentManagementServiceImpl;
import ca.bc.gov.nrs.dm.rest.v1.resource.AbstractFolderResource;
import ca.bc.gov.nrs.dm.rest.v1.resource.FileResource;
import ca.bc.gov.nrs.dm.rest.v1.resource.FilesResource;
import ca.bc.gov.nrs.dm.rest.v1.resource.FolderResource;
import ca.bc.gov.nrs.dm.rest.v1.resource.RevisionsResource;
import ca.bc.gov.webade.oauth2.rest.v1.token.client.Oauth2ClientException;
import ca.bc.gov.webade.oauth2.rest.v1.token.client.TokenService;
import ca.bc.gov.webade.oauth2.rest.v1.token.client.impl.TokenServiceImpl;
import ca.bc.gov.webade.oauth2.rest.v1.token.client.resource.AccessToken;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.core.MediaType;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;


@RequestScoped
@javax.annotation.Generated(value = "class com.quartech.codegen.FuseGenerator", date = "2017-04-24T09:07:23.579-07:00")
public class DocumentsApiServiceImpl implements DocumentsApiService {
    
      private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(DocumentsApiServiceImpl.class);
      
      private static DocumentManagementService dmsService = null;      
    
      private String authorizeUrl = "";
      private String tokenUrl = "";
      private static final String SCOPES = "DMS.*"; 
      private static String redirectUri = "http://www.redirecturi.com";
      private static final String USER_SCOPES ="DMS.CLIENT_USER";
      private static String siteMinderUserId = "NOT\\USED";
      private static String documentManagementTopLevelRestURL;
      private static String oauth2ResourceName;   
      private static Gson gson;
      private static final String OAUTH_BEARER = "Bearer ";
      private static String serviceId;
      private static String serviceSecret;
      private static String baseRemotePath;
           
      private OAuth2ProtectedResourceDetails oAuth2ProtectedResourceDetails = null;
      
      protected String dmsServiceOAuthToken = ""; 
      
      public DocumentsApiServiceImpl()
      {
        // read values from the secret.
        FileSystemXmlApplicationContext applicationContext = new FileSystemXmlApplicationContext(new String[] { "//tmp/oauth.xml" });
        
        // credentials
        serviceId = (String) applicationContext.getBean("serviceId");  
        serviceSecret = (String) applicationContext.getBean("serviceSecret");          
        oAuth2ProtectedResourceDetails = (OAuth2ProtectedResourceDetails) applicationContext.getBean("documentManagementUserResource");          
        
        // URLS
        documentManagementTopLevelRestURL = (String) applicationContext.getBean("documentManagementTopLevelRestUrl");  
        authorizeUrl = (String) applicationContext.getBean("authorizeUrl");    
        tokenUrl = (String) applicationContext.getBean("tokenUrl");    
        
        // Remote info
        baseRemotePath = (String) applicationContext.getBean("baseRemotePath");    
        
        try {
            // create an instance of the service.
            dmsService = buildDMSClientService (serviceId, serviceSecret);
        } catch (Oauth2ClientException ex) {
            LOG.error(null, ex);
        }
        
        // create a Gson object for use by the various services.  
        gson = new Gson();
                  
    }
      
    public DocumentManagementService buildDMSClientService(String serviceId, String serviceSecret) throws Oauth2ClientException
    {
            TokenService tokenService = new TokenServiceImpl(serviceId, serviceSecret, null, tokenUrl);		
            AccessToken token = tokenService.getToken(SCOPES);
            DocumentManagementService clientService = createDMSClientService(token);
            dmsServiceOAuthToken = OAUTH_BEARER + token.getAccessToken();
            return clientService;
    }
              
    private DocumentManagementService createDMSClientService(AccessToken token)
    {                            
            OAuth2RestTemplate restTemplate = new OAuth2RestTemplate(oAuth2ProtectedResourceDetails);
            OAuth2ClientContext context = restTemplate.getOAuth2ClientContext();
            context.setAccessToken(new DefaultOAuth2AccessToken(token.getAccessToken()));

            DocumentManagementService clientService = new DocumentManagementServiceImpl(false);
            ((DocumentManagementServiceImpl) clientService).setRestTemplate(restTemplate);
            ((DocumentManagementServiceImpl) clientService).setTopLevelRestURL(documentManagementTopLevelRestURL);

            return clientService;
    }
      
    @Override
    public Response documentsGet(SecurityContext securityContext, ServletContext servletContext) {
        // setup dms
        String jsonString = "";       

        try {
            // get available documents.
            AbstractFolderResource folderContents = dmsService.getFolderByPath("baseRemotePath");
            jsonString = gson.toJson(folderContents);

        } catch (DocumentManagementException ex) {
            LOG.error(null, ex);
        } catch (ForbiddenAccessException ex) {
            LOG.error(null, ex);
        }
        return Response.ok().entity(jsonString).build();      
    }
    
    @Override
    public Response documentsIdDownloadGet(String id, SecurityContext securityContext)
    {
        // first get the meta data for the file.                
          try {
              FileResource fileResource;
              fileResource = dmsService.getFileByID(id);
              java.io.File temp = java.io.File.createTempFile("temp-", ".tmp");
              byte[] data = dmsService.getFileContent(id);
              // write the bytes to the temporary file.
              java.nio.file.Files.write(temp.toPath(), data);
              
              return Response.ok(temp, MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + fileResource.getFilename() + "\"")
                .build();
          } catch (DocumentManagementException ex) {
              Logger.getLogger(DocumentsApiServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
          } catch (ForbiddenAccessException ex) {
              Logger.getLogger(DocumentsApiServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
          } catch (IOException ex) {
              Logger.getLogger(DocumentsApiServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
          }
        // save the data to a temporary file.
        return Response.serverError().build();               
    }
    
    @Override
    public Response documentsIdDeletePost(String id, SecurityContext securityContext) {
        // delete a document
        String jsonString = "";
        // get the file.        
        FileResource fileResource;
        try {
            dmsService.deleteFile(id);            
        } catch (DocumentManagementException ex) {
            LOG.error(null, ex);
        } catch (ForbiddenAccessException ex) {
            LOG.error(null, ex);
        }        
        return Response.ok().entity("").build();
    }
    
    @Override
    public Response documentsIdGet(String id, SecurityContext securityContext) {      
        // get the meta data
        String jsonString = "";
        // get the file.        
        FileResource fileResource;
        try {
            fileResource = dmsService.getFileByID(id);
            jsonString = gson.toJson(fileResource);
        } catch (DocumentManagementException ex) {
            LOG.error(null, ex);
        } catch (ForbiddenAccessException ex) {
            LOG.error(null, ex);
        }        
        return Response.ok().entity(jsonString).build();   
    }
    
    @Override
    public Response documentsIdHistoryGet(String id, SecurityContext securityContext) {
        String jsonString = "";
        // get the file.        
        FileResource fileResource;
        try {
            fileResource = dmsService.getFileByID(id);
            // get the history.
            RevisionsResource revisions = dmsService.getRevisions(fileResource, 1, 1000);
            jsonString = gson.toJson(revisions);
        } catch (DocumentManagementException ex) {
            LOG.error(null, ex);
        } catch (ForbiddenAccessException ex) {
            LOG.error(null, ex);
        }
        
        return Response.ok().entity(jsonString).build();   
    }
    
    // update the meta data.    
    @Override
    public Response documentsIdPut(String id, Attachment file, SecurityContext securityContext) {
        // replace a file        
        String jsonString = "";
        // get the file.        
        FileResource fileResource;
        try {
            fileResource = dmsService.getFileByID(id);
            
            InputStream fileStream = file.getObject(InputStream.class);
            java.io.File temp = java.io.File.createTempFile("temp-", ".tmp");
            // write the bytes to the temporary file.
            java.nio.file.Files.copy(fileStream, temp.toPath());
            
            FileResource updateFile = dmsService.checkinFile(id, temp.getAbsolutePath(), 
						"NRSDocument", "Created by DMOD", 
						"Public","TRAN-102901",
						null, null, null, "ExternallyVisible", null, null, null, null);
            // cleanup the temporary file.
            temp.delete();
            
            jsonString = gson.toJson(fileResource);
            
            
        } catch (DocumentManagementException ex) {
            LOG.error(null, ex);
        } catch (ForbiddenAccessException ex) {
            LOG.error(null, ex);
        } catch (IOException ex) {        
            LOG.error(null, ex);
        }
        return Response.ok().entity(jsonString).build();   
    }
    
    @Override
    public Response documentsPost(Attachment file, SecurityContext securityContext) {
        String jsonString = "";
        // accept the file upload.
        
        try {
            // get the folder where files will be stored.
            AbstractFolderResource uploadFolder = dmsService.getFolderByPath(baseRemotePath);            
            
            InputStream fileStream = file.getObject(InputStream.class);
            java.io.File temp = java.io.File.createTempFile("temp-", ".tmp");
            
            // write the bytes to the temporary file.
            java.nio.file.Files.copy(fileStream, temp.toPath());
            FileResource newFile = dmsService.createFile( temp.getAbsolutePath(), uploadFolder, "NRSDocument", "Created by DMOD", 
                                              "Public","TRAN-102901",
                                              null, null, null, "ExternallyVisible", null, null, null, null);
            // cleanup
            temp.delete();
            
        } catch (IOException ex) {
            LOG.error(null, ex);
        } catch (DocumentManagementException ex) {        
              LOG.error(null, ex);
        } catch (ForbiddenAccessException ex) {
            LOG.error(null, ex);
        }
        
        // process the fileStream.      
        return Response.ok().entity(jsonString).build();
    }
    
    @Override
    public Response documentsSearch(String id, SecurityContext securityContext) {
        String jsonString = "";
        // get the file.        
        FolderResource folderResource;
        FilesResource searchFiles = null;
        try {
            // change to the dmod folder.
            folderResource = null;
            // get the history.
            
	
            searchFiles = dmsService.searchFiles(folderResource, null, null, null, "Created", null, null, null, null, null, null, null, null, null, 
						null, null, null, true, null, null, null, null, null, null, null, 1, 20, null);
            jsonString = gson.toJson(searchFiles);
        } catch (DocumentManagementException ex) {
            LOG.error(null, ex);
        } 
        
        return Response.ok().entity(jsonString).build();   
    }
}

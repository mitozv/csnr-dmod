package ca.bc.gov.nrs.dm.microservice.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;

import ca.bc.gov.nrs.dm.rest.client.v1.DocumentManagementService;
import ca.bc.gov.nrs.dm.rest.client.v1.impl.DocumentManagementServiceImpl;
import ca.bc.gov.webade.oauth2.rest.v1.token.client.TokenService;
import ca.bc.gov.webade.oauth2.rest.v1.token.client.impl.TokenServiceImpl;
import ca.bc.gov.webade.oauth2.rest.v1.token.client.resource.AccessToken;

public class ServiceUtil {
	
	static final String SERVICE_CLIENT_ID = "SERVICE_CLIENT_ID";
	static final String SERVICE_CLIENT_SECRET = "SERVICE_CLIENT_SECRET";
	static final String APPLICATION_ROOT_DIRECTORY = "APPLICATION_ROOT_DIRECTORY";
	static final String APPLICATION_ACRONYM = "APPLICATION_ACRONYM";
	static final String ISSS_SERVER = "ISSS_SERVER";
	static final String OAUTH_ACCESS_TOKEN_URI_PATH = "OAUTH_ACCESS_TOKEN_URI_PATH";
	static final String OAUTH_AUTHORIZATION_URI_PATH = "OAUTH_AUTHORIZATION_URI_PATH";
	static final String DMS_SERVICE_URI_PATH = "DMS_SERVICE_URI_PATH";
	static final String DMS_DEFAULT_DOCUMENT_VISIBILITY = "DMS_DEFAULT_DOCUMENT_VISIBILITY";
	static final String DMS_DEFAULT_OCIO_CLASSIFICATION = "DMS_DEFAULT_OCIO_CLASSIFICATION";

	
	private static ServiceUtil INSTANCE = new ServiceUtil();
	private Logger LOG = LoggerFactory.getLogger(ServiceUtil.class);
	
	public static final String OAUTH_BEARER = "Bearer";
	public static final String DMS_SCOPES = "DMS.*"; 
	public static final String READ_WRITE_SCOPES = "UPDATE_FILES";
	public static final String READ_SCOPES = "READ_FILES";
    public static final int MAX_ENTRY = 1000;
    public static final int PAGE = 1;
    public static final String READ_WRITE = "RW";
    public static final String READ = "R";
    
    
    private String applicationAcronym = null;
    private String applicationRootDir = null;
    
    private String serviceId = null;
    private String serviceSecret = null;
    private String defaultDocumentVisibility = null;
    private String defaultOCIOClassification = null;
    private String dmsServiceBaseUri = null;
    private String isssServer = null;
    private String tokenUri = null;
    private String userAuthorizationUri = null;
    private AuthorizationCodeResourceDetails oauthResourceDetails = null;
    
    private Map<String, String> securityScopes = null;

    private ServiceUtil() {
 
    	serviceId = System.getenv(SERVICE_CLIENT_ID);
    	serviceSecret = System.getenv(SERVICE_CLIENT_SECRET);
    	applicationRootDir = System.getenv(APPLICATION_ROOT_DIRECTORY);
    	applicationAcronym = System.getenv(APPLICATION_ACRONYM);
    	isssServer = System.getenv(ISSS_SERVER);
    	tokenUri = isssServer + System.getenv(OAUTH_ACCESS_TOKEN_URI_PATH);
    	userAuthorizationUri =  isssServer + System.getenv(OAUTH_AUTHORIZATION_URI_PATH);
    	dmsServiceBaseUri = isssServer + System.getenv(DMS_SERVICE_URI_PATH);
    	defaultDocumentVisibility = System.getenv(DMS_DEFAULT_DOCUMENT_VISIBILITY);
    	defaultOCIOClassification = System.getenv(DMS_DEFAULT_OCIO_CLASSIFICATION);
    	
    	
	    securityScopes = new HashMap<String, String>();
	    String readWriteSecurity = applicationAcronym + "." + ServiceUtil.READ_WRITE_SCOPES;
	    String readSecurity = applicationAcronym + "." + ServiceUtil.READ_SCOPES;
	    securityScopes.put(READ_WRITE, readWriteSecurity);
	    securityScopes.put(READ, readSecurity);
    		    
        oauthResourceDetails = new AuthorizationCodeResourceDetails();
        List<String> scopeList = new ArrayList<String>();
        scopeList.add(DMS_SCOPES);
        scopeList.add(applicationAcronym + ".*");
        
        oauthResourceDetails.setClientId(serviceId);
        oauthResourceDetails.setClientSecret(serviceSecret);
        oauthResourceDetails.setScope(scopeList);
        oauthResourceDetails.setUserAuthorizationUri(userAuthorizationUri);
        oauthResourceDetails.setAccessTokenUri(tokenUri);
        
        LOG.info("SERVICE_CLIENT_ID: " +  serviceId);
        LOG.info("APPLICATION_ROOT_DIRECTORY: " + applicationRootDir);
        LOG.info("APPLICATION_ACRONYM: " + applicationAcronym);
        LOG.info("ISSS_SERVER: " + isssServer);
        LOG.info("OAUTH_ACCESS_TOKEN_URI_PATH: " + tokenUri);
        LOG.info("OAUTH_AUTHORIZATION_URI_PATH: " + userAuthorizationUri);
        LOG.info("DMS_SERVICE_URI_PATH: " + dmsServiceBaseUri);
        LOG.info("DMS_DEFAULT_DOCUMENT_VISIBILITY: " + defaultDocumentVisibility);
        LOG.info("DMS_DEFAULT_OCIO_CLASSIFICATION: " + defaultOCIOClassification);
    }
    
    
	ServiceUtil(Map<String, String> environmentVariables) {
		
    	serviceId = environmentVariables.get(SERVICE_CLIENT_ID);
	    serviceSecret = environmentVariables.get(SERVICE_CLIENT_SECRET);
	    
	    applicationAcronym =  environmentVariables.get(APPLICATION_ACRONYM);
	    applicationRootDir =  environmentVariables.get(APPLICATION_ROOT_DIRECTORY); 
	    
	    isssServer = environmentVariables.get(ISSS_SERVER);
	    dmsServiceBaseUri = isssServer + environmentVariables.get(DMS_SERVICE_URI_PATH);
	    tokenUri = isssServer + environmentVariables.get(OAUTH_ACCESS_TOKEN_URI_PATH);
	    
        oauthResourceDetails = new AuthorizationCodeResourceDetails();
        List<String> scopeList = new ArrayList<String>();
        scopeList.add(DMS_SCOPES);
        scopeList.add(applicationAcronym + ".*");
        
        oauthResourceDetails.setClientId(serviceId);
        oauthResourceDetails.setClientSecret(serviceSecret);
        oauthResourceDetails.setScope(scopeList);
        oauthResourceDetails.setUserAuthorizationUri(userAuthorizationUri);
        oauthResourceDetails.setAccessTokenUri(tokenUri);
        	    
    }
    
    public static ServiceUtil getInstance() {
    	return INSTANCE;
    }
    
    public String getRootDirectory() throws Exception {
    	if(applicationRootDir == null || applicationRootDir.trim().isEmpty()) {
    		throw new Exception("Application Root Dir is not properly setup!");
    	}
    	
    	if(applicationAcronym == null || applicationAcronym.trim().isEmpty()) {
    		throw new Exception("Application Acronym is not properly setup!");
    	}
    	
    	StringBuffer rootDir = new StringBuffer("/")
    			.append(applicationRootDir.trim())
    			.append("/")
    			.append(applicationAcronym.trim());
    	
    	return rootDir.toString();
    }
    
    public String getScopes() throws Exception {
    	if(applicationAcronym == null || applicationAcronym.trim().isEmpty()) {
    		throw new Exception("Application Acronym is not properly setup!");
    	}
    	
    	StringBuffer scopes = new StringBuffer(DMS_SCOPES)
    			.append(",")
    			.append(applicationAcronym.trim())
    			.append(".*");
    	
    	return scopes.toString();
    }
    
    public String getSecurityScopes(String permission) throws Exception {
    	if(applicationAcronym == null || applicationAcronym.trim().isEmpty()) {
    		throw new Exception("Application Acronym is not properly setup!");
    	}
    	
    	return securityScopes.get(permission);
    }
    
    public String getApplicationAcronym() {
    	return this.applicationAcronym;
    }
    
    public String getDefaultDocumentVisibility() {
    	return this.defaultDocumentVisibility;
    }
    
    public String getDefaultOCIOClassification() {
    	return this.defaultOCIOClassification;
    }
    
    public DocumentManagementService getServiceClient(HttpHeaders headers) {
		String accessToken = getAccessToken(headers);
		DocumentManagementService service = null;
		
		if(accessToken != null) {
			AccessToken token = new AccessToken();
    		token.setAccessToken(accessToken);
    		service = createDMSClientService(token);
		} else {
			try {
				TokenService tokenService = new TokenServiceImpl(serviceId, serviceSecret, null, tokenUri);
		        AccessToken token = tokenService.getToken(getScopes());
		        service = createDMSClientService(token);
		        
			}catch(Exception ex) {
				 LOG.error("getServiceClient", ex);
			}
			
		}
		return service;
	}
    
    
    public DocumentManagementService getServiceClient() {
    	DocumentManagementService service  = null;
    	
        try {
        	TokenService tokenService = new TokenServiceImpl(serviceId, serviceSecret, null, tokenUri);
            AccessToken token = tokenService.getToken(getScopes());
        	service = createDMSClientService(token);
        }catch(Exception ex) {
        	LOG.error("getServiceClient", ex);
        }
        
        
        return service;
	}
    
    

	private String getAccessToken(HttpHeaders headers) {
    	String token = null;
    	List<String> authList = headers.getRequestHeader("Authorization");
    	
    	if(authList != null && authList.size() > 0) {
    		String bearerToken = authList.get(0).trim();
    		if(bearerToken.startsWith(OAUTH_BEARER) && bearerToken.trim().length() > OAUTH_BEARER.length()) {
    			token = bearerToken.substring(OAUTH_BEARER.length() + 1, bearerToken.length());
    			token = token.trim();
    		}
    	}
    	
    	return token;
    }
	
	private DocumentManagementService createDMSClientService(AccessToken token) {
		  OAuth2RestTemplate restTemplate = new OAuth2RestTemplate(oauthResourceDetails);
		  OAuth2ClientContext context = restTemplate.getOAuth2ClientContext();
		  context.setAccessToken(new DefaultOAuth2AccessToken(token.getAccessToken()));
		
		  DocumentManagementService clientService = new DocumentManagementServiceImpl(false);
		  ((DocumentManagementServiceImpl) clientService).setRestTemplate(restTemplate);
		  ((DocumentManagementServiceImpl) clientService).setTopLevelRestURL(dmsServiceBaseUri);
	
	  return clientService;
	}
	
	
	
}

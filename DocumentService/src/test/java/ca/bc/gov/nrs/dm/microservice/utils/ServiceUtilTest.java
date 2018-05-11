package ca.bc.gov.nrs.dm.microservice.utils;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ca.bc.gov.nrs.dm.rest.client.v1.DocumentManagementService;

@RunWith(MockitoJUnitRunner.class)
public class ServiceUtilTest {
	
	
	@Mock
	private HttpHeaders headers;
	
	@Mock
	private DocumentManagementService dmsService;
	
	Map<String, String> envData = null;
	

	@Before
	public void setup() {
		envData = new HashMap<String, String>();
		envData.put(ServiceUtil.SERVICE_CLIENT_ID, "TEST");
		envData.put(ServiceUtil.SERVICE_CLIENT_SECRET, "TEST");
		envData.put(ServiceUtil.APPLICATION_ACRONYM, "TEST");
		envData.put(ServiceUtil.APPLICATION_ROOT_DIRECTORY, "NRS");
		envData.put(ServiceUtil.ISSS_SERVER, "http://localhost:8080");
		envData.put(ServiceUtil.DMS_SERVICE_URI_PATH, "/dms/v1/");
		envData.put(ServiceUtil.DMS_SERVICE_URI_PATH, "/oauth/test");
		envData.put(ServiceUtil.OAUTH_AUTHORIZATION_URI_PATH, "/authorization");
		envData.put(ServiceUtil.OAUTH_ACCESS_TOKEN_URI_PATH, "/oauth");
		
		
	}
	
//Temporarily commented out
//	@Test
//	public void testNoHeader() {
//		
//		when(headers.getRequestHeader("Authorization")).thenReturn(null);
//		
//		ServiceUtil serviceUtil = new ServiceUtil(envData);
//		serviceUtil.setDmsServiceClient(dmsService);
//		
//		DocumentManagementService defaultServiceClient = serviceUtil.getServiceClient();
//		Assert.assertNotNull(defaultServiceClient);
//		Assert.assertEquals(dmsService, defaultServiceClient);
//	}
//	
//	@Test
//	public void testNoBearerAuthorization() {
//		ServiceUtil serviceUtil = new ServiceUtil(envData);
//		
//		List<String> authorizationList = new ArrayList<String>();
//		authorizationList.add("test");
//		authorizationList.add("12345");
//		
//		when(headers.getRequestHeader("Authorization")).thenReturn(authorizationList);
//		serviceUtil.setDmsServiceClient(dmsService);
//		
//		DocumentManagementService defaultServiceClient = serviceUtil.getServiceClient();
//		Assert.assertNotNull(defaultServiceClient);
//		Assert.assertEquals(dmsService, defaultServiceClient);
//	}
//	
//	@Test
//	public void testBearerWithoutToken() {
//
//		ServiceUtil serviceUtil = new ServiceUtil(envData);
//		
//		List<String> authorizationList = new ArrayList<String>();
//		authorizationList.add("Bearer");
//		authorizationList.add("34567");
//		
//		when(headers.getRequestHeader("Authorization")).thenReturn(authorizationList);
//		serviceUtil.setDmsServiceClient(dmsService);
//		
//		DocumentManagementService service = serviceUtil.getServiceClient(headers);
//		Assert.assertNotNull(service);
//		
//		Assert.assertEquals(dmsService, service);
//	}
//	
//	@Test
//	public void testUserTokenAuthorization() {
//		ServiceUtil serviceUtil = new ServiceUtil(envData);
//		
//		List<String> authorizationList = new ArrayList<String>();
//		authorizationList.add("Bearer 12345");
//		authorizationList.add("34567");
//		
//		when(headers.getRequestHeader("Authorization")).thenReturn(authorizationList);
//
//		serviceUtil.setDmsServiceClient(dmsService);
//		
//		DocumentManagementService service = serviceUtil.getServiceClient(headers);
//		Assert.assertNotNull(service);
//
//		
//
//		Assert.assertNotEquals(dmsService, service);
//		
//	}
//	
//	@Test
//	public void testUserTokenAuthorizationWithWhitespace() {
//		ServiceUtil serviceUtil = new ServiceUtil(envData);
//		
//		List<String> authorizationList = new ArrayList<String>();
//		authorizationList.add("   Bearer    12345       ");
//		authorizationList.add("34567");
//		
//		when(headers.getRequestHeader("Authorization")).thenReturn(authorizationList);
//		serviceUtil.setDmsServiceClient(dmsService);
//		DocumentManagementService service = serviceUtil.getServiceClient(headers);
//		Assert.assertNotNull(service);
//		
//		Assert.assertNotEquals(dmsService, service);
//	}
//	
	
	@Test
	public void testGetScopes() {
		
		ServiceUtil serviceUtil = new ServiceUtil(envData);

		try {
			String scope = serviceUtil.getScopes();
			Assert.assertTrue(scope.contains("DMS.*"));
			Assert.assertTrue(scope.contains("TEST.*"));
		}catch(Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test(expected = Exception.class)
	public void testGetScopesNull()throws Exception {
		Map<String, String> testData = new HashMap<String, String>();
		testData.putAll(envData);
		testData.put(ServiceUtil.APPLICATION_ACRONYM, null);
		

		ServiceUtil serviceUtil = new ServiceUtil(testData);
		serviceUtil.getScopes();
	}
	
	@Test
	public void testGetRootDir() {

		Map<String, String> testData = new HashMap<String, String>();
		testData.putAll(envData);
		testData.put(ServiceUtil.APPLICATION_ACRONYM, "TEST");
		testData.put(ServiceUtil.APPLICATION_ROOT_DIRECTORY, "ROOT");
		
		
		ServiceUtil serviceUtil = new ServiceUtil(testData);

		try {
			String rootDir = serviceUtil.getRootDirectory();
			Assert.assertEquals(rootDir, "/ROOT/TEST");
		}catch(Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test(expected = Exception.class)
	public void testGetRootNull()throws Exception {
		Map<String, String> testData = new HashMap<String, String>();
		testData.putAll(envData);
		testData.put(ServiceUtil.APPLICATION_ACRONYM, "TEST");
		testData.put(ServiceUtil.APPLICATION_ROOT_DIRECTORY, null);
		
		ServiceUtil serviceUtil = new ServiceUtil(testData);
		serviceUtil.getRootDirectory();
	}
	
	@Test(expected = Exception.class)
	public void testGetRootNullAcronym()throws Exception {
		Map<String, String> testData = new HashMap<String, String>();
		testData.putAll(envData);
		testData.put(ServiceUtil.APPLICATION_ACRONYM, null);
		testData.put(ServiceUtil.APPLICATION_ROOT_DIRECTORY, "ROOT");
		

		ServiceUtil serviceUtil = new ServiceUtil(testData);
		serviceUtil.getRootDirectory();
	}
}

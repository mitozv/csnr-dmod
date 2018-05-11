package ca.bc.gov.nrs.dm.microservice.api.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ca.bc.gov.nrs.dm.microservice.utils.ServiceUtil;
import ca.bc.gov.nrs.dm.rest.v1.resource.FileResource;
import ca.bc.gov.nrs.dm.rest.v1.resource.FolderContentResource;
import ca.bc.gov.nrs.dm.rest.v1.resource.FolderResource;
import ca.bc.gov.nrs.dm.rest.v1.resource.RevisionResource;

@RunWith(MockitoJUnitRunner.class)
public class DocumentsApiServiceImplTest {

	private RandomDataGenerator randomGenerator = new RandomDataGenerator();
	
	@Mock
	private ServiceUtil serviceUtil;
	
	@Mock
	private Attachment attachment;
	
	
	@Before
	public void setup() throws Exception {
		when(serviceUtil.getApplicationAcronym()).thenReturn("TEST");
		when(serviceUtil.getRootDirectory()).thenReturn("/NRS/TEST");
	}

	@Test
	public void testDocumentsGet()throws Exception {
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
		when(serviceUtil.getServiceClient()).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);
		
		FolderContentResource folderContentResource = randomGenerator.createAndFill(FolderContentResource.class);
		FolderResource folderResource = randomGenerator.createAndFill(FolderResource.class);
		
		mockDMS.setFolderResource(folderResource);
		mockDMS.setFolderContentResource(folderContentResource);
		Response response = dmsService.documentsGet(null);
		Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		Gson gson = new GsonBuilder().create();
		Assert.assertEquals(gson.toJson(folderContentResource), response.getEntity().toString());
	}
	

	@Test
	public void testDocumentsGetDMSError() throws Exception {
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
		when(serviceUtil.getServiceClient()).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);
		
		FolderResource folderResource = randomGenerator.createAndFill(FolderResource.class);
		mockDMS.setFolderResource(folderResource);
		
		mockDMS.setDmsError(true);
		Response response = dmsService.documentsGet(null);
		Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		Assert.assertTrue(response.getEntity().toString().contains("Document Management Error"));
	}
	
	@Test
	public void testDocumentsGetForbiddenError() throws Exception {
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
		when(serviceUtil.getServiceClient()).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);
		
		FolderResource folderResource = randomGenerator.createAndFill(FolderResource.class);
		mockDMS.setFolderResource(folderResource);
		
		mockDMS.setForbiddenError(true);
		Response response = dmsService.documentsGet(null);
		Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		Assert.assertTrue(response.getEntity().toString().contains("Access Denied"));
	}
	
	@Test
	public void testDocumentsDownloadFile()throws Exception {
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
		when(serviceUtil.getServiceClient((HttpHeaders) any())).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);
		
		FileResource fileResource = new FileResource();
		fileResource.setFilename("TEST.txt");
		mockDMS.setFileResource(fileResource);
		
		String content = "This is the content of the file";
		byte[] bytes = content.getBytes();
		mockDMS.setFileContent(bytes);
		
		Response response = dmsService.documentsDownloadFile("1", null);
		Assert.assertNotNull(response);
		Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
		String contentDisposition = response.getHeaderString("Content-Disposition");
		Assert.assertTrue(contentDisposition.contains(fileResource.getFilename()));
		
		ByteArrayInputStream inputStream = (ByteArrayInputStream)response.getEntity(); 
		byte[] expected = new byte[inputStream.available()];
	    inputStream.read(expected);
	    
	    Assert.assertTrue(Arrays.equals(bytes, expected));
	}
	
	
	@Test
	public void testDocumentsDownloadFileDMSError() throws Exception {
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
		when(serviceUtil.getServiceClient((HttpHeaders) any())).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);
		
		FileResource fileResource = new FileResource();
		fileResource.setFilename("TEST.txt");
		mockDMS.setFileResource(fileResource);
		
		mockDMS.setDmsError(true);
		Response response = dmsService.documentsDownloadFile("1", null);
		Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		Assert.assertTrue(response.getEntity().toString().contains("Document Management Error"));
	}
	
	@Test
	public void testDocumentsDownloadFileForbiddenError() throws Exception {
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
		when(serviceUtil.getServiceClient((HttpHeaders) any())).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);
		
		FileResource fileResource = new FileResource();
		fileResource.setFilename("TEST.txt");
		mockDMS.setFileResource(fileResource);
		
		mockDMS.setForbiddenError(true);
		Response response = dmsService.documentsDownloadFile("1", null);
		Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		Assert.assertTrue(response.getEntity().toString().contains("Access Denied"));
	}
	
	@Test
	public void testDocumentsGetFile() throws Exception {
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
		when(serviceUtil.getServiceClient((HttpHeaders) any())).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);
		
		FileResource fileResource = randomGenerator.createAndFill(FileResource.class);		
		mockDMS.setFileResource(fileResource);
		
		Response response = dmsService.documentsGetFile("1", null);
		Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
		Gson gson = new GsonBuilder().create();
		Assert.assertEquals(gson.toJson(fileResource), response.getEntity().toString());
	}
	
	@Test
	public void testDocumentsGetFileDMSError() throws Exception {
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
		when(serviceUtil.getServiceClient((HttpHeaders) any())).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);
		
		FileResource fileResource = new FileResource();
		fileResource.setFilename("TEST.txt");
		mockDMS.setFileResource(fileResource);
		
		mockDMS.setDmsError(true);
		Response response = dmsService.documentsGetFile("1", null);
		Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		Assert.assertTrue(response.getEntity().toString().contains("Document Management Error"));
	}
	
	@Test
	public void testDocumentsGetFileForbiddenError() throws Exception {
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
		when(serviceUtil.getServiceClient((HttpHeaders) any())).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);
		
		FileResource fileResource = new FileResource();
		fileResource.setFilename("TEST.txt");
		mockDMS.setFileResource(fileResource);
		
		mockDMS.setForbiddenError(true);
		Response response = dmsService.documentsGetFile("1", null);
		Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		Assert.assertTrue(response.getEntity().toString().contains("Access Denied"));
	}
	
	
	@Test
	public void testDocumentsGetFolder() throws Exception {
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
		when(serviceUtil.getServiceClient((HttpHeaders) any())).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);
		
		FolderContentResource folderContentResource = randomGenerator.createAndFill(FolderContentResource.class);		
		mockDMS.setFolderContentResource(folderContentResource);
		
		Response response = dmsService.documentsGetFolder("test", null);
		Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
		Gson gson = new GsonBuilder().create();
		Assert.assertEquals(gson.toJson(folderContentResource), response.getEntity().toString());
	}
	
	@Test
	public void testDocumentsGetFolderDMSError() throws Exception {
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
		when(serviceUtil.getServiceClient((HttpHeaders) any())).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);
		
		FolderContentResource folderContentResource = randomGenerator.createAndFill(FolderContentResource.class);		
		mockDMS.setFolderContentResource(folderContentResource);
		mockDMS.setDmsError(true);
		
		Response response = dmsService.documentsGetFolder("test", null);
		Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		Assert.assertTrue(response.getEntity().toString().contains("Document Management Error"));
	}
	
	@Test
	public void testDocumentsGetFolderForbiddenError() throws Exception {
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
		when(serviceUtil.getServiceClient((HttpHeaders) any())).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);
		
		FolderContentResource folderContentResource = randomGenerator.createAndFill(FolderContentResource.class);		
		mockDMS.setFolderContentResource(folderContentResource);
		mockDMS.setForbiddenError(true);
		
		Response response = dmsService.documentsGetFolder("test", null);
		Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		Assert.assertTrue(response.getEntity().toString().contains("Access Denied"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDocumentsPutFile()throws Exception {
		String testData = "Test data";
		when(attachment.getObject((Class<InputStream>) any())).thenReturn(new ByteArrayInputStream(testData.getBytes()));
		
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
		when(serviceUtil.getServiceClient((HttpHeaders) any())).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);

		FolderResource folderResource = new FolderResource();
		folderResource.setItemID("111111");
		mockDMS.setFolderResource(folderResource);
		
		FileResource fileResource = randomGenerator.createAndFill(FileResource.class);		
		mockDMS.setFileResource(fileResource);
		
		Response response = dmsService.documentsPutFile(null, "Test.txt", attachment, null);
		Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
		Gson gson = new GsonBuilder().create();
		Assert.assertEquals(gson.toJson(fileResource), response.getEntity().toString());
	}
	
	
	@SuppressWarnings("unchecked")
	@Test
	public void testDocumentsPutFileForbiddenError()throws Exception {
		String testData = "Test data";
		when(attachment.getObject((Class<InputStream>) any())).thenReturn(new ByteArrayInputStream(testData.getBytes()));
		
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
		when(serviceUtil.getServiceClient((HttpHeaders) any())).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);
		
		mockDMS.setForbiddenError(true);
		
		Response response = dmsService.documentsPutFile(null, "Test.txt", attachment, null);
		Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		Assert.assertTrue(response.getEntity().toString().contains("Access Denied"));;
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testDocumentsPutFileDMSError()throws Exception {
		String testData = "Test data";
		when(attachment.getObject((Class<InputStream>) any())).thenReturn(new ByteArrayInputStream(testData.getBytes()));
		
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
		when(serviceUtil.getServiceClient((HttpHeaders) any())).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);

		mockDMS.setDmsError(true);
		
		Response response = dmsService.documentsPutFile(null, "Test.txt", attachment, null);
		Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		Assert.assertTrue(response.getEntity().toString().contains("Document Management Error"));;
	}
	
	@Test
	public void testDocumentPutFileMetadataExpiryDate() throws Exception {
		
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
		when(serviceUtil.getServiceClient((HttpHeaders) any())).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);
		
		FileResource fileResource = randomGenerator.createAndFill(FileResource.class);		
		mockDMS.setFileResource(fileResource);
		
		Map<String, String> dataMap = new HashMap<String, String>();
		dataMap.put("expiryDate", "2018-01-31T00:00:00.000Z");
		Gson gson = new GsonBuilder().create();
		Response response = dmsService.documentsPutFileMetadata("1", gson.toJson(dataMap),null);
		Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
	}
	
	@Test
	public void testDocumentPutFileMetadataInvalidFormatExpiryDate() throws Exception {
		
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
		when(serviceUtil.getServiceClient((HttpHeaders) any())).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);
		
		FileResource fileResource = randomGenerator.createAndFill(FileResource.class);		
		mockDMS.setFileResource(fileResource);
		
		Map<String, String> dataMap = new HashMap<String, String>();
		dataMap.put("expiryDate", "2018-01-31 00:00:00");
		Gson gson = new GsonBuilder().create();
		Response response = dmsService.documentsPutFileMetadata("1", gson.toJson(dataMap),null);
		Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		
		Assert.assertTrue(response.getEntity().toString().contains("Validation Error"));
	}
	
	@Test
	public void testDocumentPutFileMetadataSecurity() throws Exception {
		
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
		when(serviceUtil.getServiceClient((HttpHeaders) any())).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);
		
		FileResource fileResource = randomGenerator.createAndFill(FileResource.class);		
		fileResource.setIsCheckedOut(Boolean.TRUE);
		mockDMS.setFileResource(fileResource);
		
		Map<String, String> dataMap = new HashMap<String, String>();
		dataMap.put("generalVisibility", "ExternallyVisible");
		dataMap.put("ocioSecurityClassification", "Public");
		Gson gson = new GsonBuilder().create();
		Response response = dmsService.documentsPutFileMetadata("1", gson.toJson(dataMap),null);
		Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
	}
	
	@Test
	public void testDocumentPutFileMetadataDMSError() throws Exception {
		
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
		when(serviceUtil.getServiceClient((HttpHeaders) any())).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);
			
		mockDMS.setDmsError(true);
		
		Map<String, String> dataMap = new HashMap<String, String>();
		dataMap.put("generalVisibility", "ExternallyVisible");
		dataMap.put("ocioSecurityClassification", "Public");
		Gson gson = new GsonBuilder().create();
		Response response = dmsService.documentsPutFileMetadata("1", gson.toJson(dataMap),null);
		Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		Assert.assertTrue(response.getEntity().toString().contains("Document Management Error"));
	}
	
	@Test
	public void testDocumentPutFileMetadataForbiddenError() throws Exception {
		
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
		when(serviceUtil.getServiceClient((HttpHeaders) any())).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);
			
		mockDMS.setForbiddenError(true);
		
		Map<String, String> dataMap = new HashMap<String, String>();
		dataMap.put("generalVisibility", "ExternallyVisible");
		dataMap.put("ocioSecurityClassification", "Public");
		Gson gson = new GsonBuilder().create();
		Response response = dmsService.documentsPutFileMetadata("1", gson.toJson(dataMap),null);
		Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		Assert.assertTrue(response.getEntity().toString().contains("Access Denied"));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testDocumentsPostFile()throws Exception {
		String testData = "Test data";
		when(attachment.getObject((Class<InputStream>) any())).thenReturn(new ByteArrayInputStream(testData.getBytes()));
		
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
		when(serviceUtil.getServiceClient((HttpHeaders) any())).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);

		FolderResource folderResource = new FolderResource();
		folderResource.setItemID("111111");
		mockDMS.setFolderResource(folderResource);
		
		FileResource fileResource = randomGenerator.createAndFill(FileResource.class);		
		mockDMS.setFileResource(fileResource);
		
		Response response = dmsService.documentsPostFile(null, "Test.txt", attachment, null);
		Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
		Gson gson = new GsonBuilder().create();
		Assert.assertEquals(gson.toJson(fileResource), response.getEntity().toString());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testDocumentsPostFileWithId()throws Exception {
		String testData = "Test data";
		when(attachment.getObject((Class<InputStream>) any())).thenReturn(new ByteArrayInputStream(testData.getBytes()));
		
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
		when(serviceUtil.getServiceClient((HttpHeaders) any())).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);

		FolderResource folderResource = new FolderResource();
		folderResource.setItemID("111111");
		mockDMS.setFolderResource(folderResource);
		
		FileResource fileResource = randomGenerator.createAndFill(FileResource.class);		
		mockDMS.setFileResource(fileResource);
		
		Response response = dmsService.documentsPostFile("111111", "Test.txt", attachment, null);
		Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
		Gson gson = new GsonBuilder().create();
		Assert.assertEquals(gson.toJson(fileResource), response.getEntity().toString());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testDocumentsPostFileDMSError()throws Exception {
		String testData = "Test data";
		when(attachment.getObject((Class<InputStream>) any())).thenReturn(new ByteArrayInputStream(testData.getBytes()));
		
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
		when(serviceUtil.getServiceClient((HttpHeaders) any())).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);

		mockDMS.setDmsError(true);
		
		Response response = dmsService.documentsPostFile(null, "Test.txt", attachment, null);
		Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		Assert.assertTrue(response.getEntity().toString().contains("Document Management Error"));
	}
	
	@Test
	public void testDeleteFolder() throws Exception {
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
		
		when(serviceUtil.getServiceClient((HttpHeaders) any())).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);
		
		FolderResource folderResource = new FolderResource();
		folderResource.setItemID("111111");
		mockDMS.setFolderResource(folderResource);
		
		Response response = dmsService.documentsDeleteFolder(folderResource.getItemID(), null);
		Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
	}
	
	@Test
	public void testDeleteFolderDMSError() throws Exception {
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
		
		when(serviceUtil.getServiceClient((HttpHeaders) any())).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);
		
		FolderResource folderResource = new FolderResource();
		folderResource.setItemID("111111");
		mockDMS.setFolderResource(folderResource);
		mockDMS.setDmsError(true);
		Response response = dmsService.documentsDeleteFolder(folderResource.getItemID(), null);
		Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		Assert.assertTrue(response.getEntity().toString().contains("Document Management Error"));
	}
	
	@Test
	public void testDeleteFolderForbiddenError() throws Exception {
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
		
		when(serviceUtil.getServiceClient((HttpHeaders) any())).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);
		
		FolderResource folderResource = new FolderResource();
		folderResource.setItemID("111111");
		mockDMS.setFolderResource(folderResource);
		mockDMS.setForbiddenError(true);
		Response response = dmsService.documentsDeleteFolder(folderResource.getItemID(), null);
		Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		Assert.assertTrue(response.getEntity().toString().contains("Access Denied"));
	}
	
	@Test
	public void testDocumentsSearchGet() {
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
		
		when(serviceUtil.getServiceClient((HttpHeaders) any())).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);
		
		FileResource fileResource = new FileResource();
		fileResource.setFilename("TEST.txt");
		mockDMS.setFileResource(fileResource);
		
		Response response = dmsService.documentsSearchGet("sample", null);
		Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
		Assert.assertNotNull(response.getEntity());
	}
	
	@Test
	public void testDocumentsSearchGetDMSError() {
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
		
		when(serviceUtil.getServiceClient((HttpHeaders) any())).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);
		
		mockDMS.setDmsError(true);
		
		Response response = dmsService.documentsSearchGet("sample", null);
		Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		Assert.assertTrue(response.getEntity().toString().contains("Document Management Error"));
	}
	
	@Test
	public void testDocumentsGetFileHistory()throws Exception {
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
		
		when(serviceUtil.getServiceClient((HttpHeaders) any())).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);

		RevisionResource revisionResource = randomGenerator.createAndFill(RevisionResource.class);		
		mockDMS.setRevisionResource(revisionResource);
		
		Response response = dmsService.documentsGetFileHistory("1", null);
		Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
		Assert.assertNotNull(response.getEntity());
	}
	
	@Test
	public void testDocumentsGetFileHistoryDMSError()throws Exception {
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
		
		when(serviceUtil.getServiceClient((HttpHeaders) any())).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);

		mockDMS.setDmsError(true);
		
		Response response = dmsService.documentsGetFileHistory("1", null);
		Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		Assert.assertTrue(response.getEntity().toString().contains("Document Management Error"));
	}
	
	@Test
	public void testDocumentsPostFolder()throws Exception {
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
	
		when(serviceUtil.getServiceClient((HttpHeaders) any())).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);
		
		FolderResource folderResource = new FolderResource();
		folderResource.setItemID("111111");
		mockDMS.setFolderResource(folderResource);
		
		Map<String, String> dataMap = new HashMap<String, String>();
		dataMap.put("name", "testFolder");
		
		Gson gson = new GsonBuilder().create();
		Response response = dmsService.documentsPostFolder(null, gson.toJson(dataMap), null);
		Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
	}
	
	@Test
	public void testDocumentsPostFolderForbiddenError()throws Exception {
		MockDocumentManagementService mockDMS = new MockDocumentManagementService();
	
		when(serviceUtil.getServiceClient((HttpHeaders) any())).thenReturn(mockDMS);
		DocumentsApiServiceImpl dmsService = new DocumentsApiServiceImpl(serviceUtil);
		
		Map<String, String> dataMap = new HashMap<String, String>();
		dataMap.put("name", "testFolder");
		
		FolderResource folderResource = new FolderResource();
		folderResource.setItemID("111111");
		mockDMS.setFolderResource(folderResource);
		
		mockDMS.setForbiddenError(true);
		
		Gson gson = new GsonBuilder().create();
		Response response = dmsService.documentsPostFolder("1", gson.toJson(dataMap), null);
		Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		Assert.assertTrue(response.getEntity().toString().contains("Access Denied"));
	}
}

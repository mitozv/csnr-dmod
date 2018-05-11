package ca.bc.gov.nrs.dm.microservice.api.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ca.bc.gov.nrs.dm.microservice.api.DocumentsApiService;
import ca.bc.gov.nrs.dm.microservice.model.MessageHelper;
import ca.bc.gov.nrs.dm.microservice.utils.DateUtil;
import ca.bc.gov.nrs.dm.microservice.utils.ServiceUtil;
import ca.bc.gov.nrs.dm.model.v1.DefaultFileMetadata.DocType;
import ca.bc.gov.nrs.dm.rest.client.v1.DocumentManagementException;
import ca.bc.gov.nrs.dm.rest.client.v1.DocumentManagementService;
import ca.bc.gov.nrs.dm.rest.client.v1.ForbiddenAccessException;
import ca.bc.gov.nrs.dm.rest.client.v1.ValidationException;
import ca.bc.gov.nrs.dm.rest.v1.resource.ACLResource;
import ca.bc.gov.nrs.dm.rest.v1.resource.AbstractFolderResource;
import ca.bc.gov.nrs.dm.rest.v1.resource.FileResource;
import ca.bc.gov.nrs.dm.rest.v1.resource.FilesResource;
import ca.bc.gov.nrs.dm.rest.v1.resource.FolderContentResource;
import ca.bc.gov.nrs.dm.rest.v1.resource.FolderResource;
import ca.bc.gov.nrs.dm.rest.v1.resource.RevisionsResource;
import ca.bc.gov.nrs.dm.rest.v1.resource.SecurityMetadataResource;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;


@RequestScoped
@javax.annotation.Generated(value = "class com.quartech.codegen.FuseGenerator", date = "2017-04-24T09:07:23.579-07:00")
public class DocumentsApiServiceImpl implements DocumentsApiService {

    
    private static final Logger LOG = LoggerFactory.getLogger(DocumentsApiServiceImpl.class);
    private static final Counter requestsCounter = Counter.build().name("dmod_http_request_total").labelNames("method").help("Number of call for document service").register();
    private static final Histogram requestLatency = Histogram.build().name("dmod_http_request_latency_seconds").labelNames("method").help("Request latency in seconds.").register();
    
    private String uploadDirectory = "/uploads";
    private ServiceUtil serviceUtil = null;
    
    public DocumentsApiServiceImpl() {
    	serviceUtil = ServiceUtil.getInstance();
    }
    
    public DocumentsApiServiceImpl(ServiceUtil serviceUtil) {
    	this.serviceUtil = serviceUtil;
    }
    
    public void setUploadDirectory(String uploadDirectory) {
    	this.uploadDirectory = uploadDirectory;
    }

    public String getUploadDirectory() {
    	return this.uploadDirectory;
    }
	@Override
    public Response documentsGet(HttpHeaders headers) {
		requestsCounter.labels("documentsGet").inc();
	
		Histogram.Timer requestTimer = requestLatency.labels("documentsGet").startTimer();
    	Response response = null;
        try {
        	//always use the default service account when getting the folders and files
        	//so that even if login user is not assign to have permission, it will still be able to view documents
        	DocumentManagementService dmsService = serviceUtil.getServiceClient();
        	
            AbstractFolderResource folderContents = dmsService.getFolderByPath(serviceUtil.getRootDirectory());
            
            FolderContentResource results = null;
            if(folderContents != null) {
            	results = dmsService.browseFolder(folderContents, ServiceUtil.PAGE, ServiceUtil.MAX_ENTRY);
            } else {
            	results = new FolderContentResource();
            }
            
            Gson gson = new GsonBuilder().create();
            String jsonString = gson.toJson(results);
            response =  Response.ok().entity(jsonString).build();

        } catch (ForbiddenAccessException ex) {
            LOG.error("documentsGet", ex);
            response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Access Denied", ex.getMessage());
        } catch(DocumentManagementException ex) {
        	 LOG.error("documentsGet", ex);
        	response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Document Management Error", ex.getMessage());
        } catch(Exception ex) {
        	 LOG.error("documentsGet", ex);
        	response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Server Error", "Unhandled Server error.");
        } finally {
        	requestTimer.observeDuration();
        }
        
        return response;
    }

    @Override
    public Response documentsDownloadFile(String id, HttpHeaders headers) {
    	requestsCounter.labels("documentsDownloadFile").inc();
    	Histogram.Timer requestTimer = requestLatency.labels("documentsDownloadFile").startTimer();
        
    	Response response = null;
        try {
            FileResource fileResource;
            DocumentManagementService dmsService = serviceUtil.getServiceClient(headers);
            fileResource = dmsService.getFileByID(id);

            byte[] data = dmsService.getFileContent(id);
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            
            ResponseBuilder responseBuilder = Response.status(Status.OK);
            responseBuilder.type( MediaType.APPLICATION_OCTET_STREAM);
    		responseBuilder.header("Content-Disposition", "attachment; filename=\"" + fileResource.getFilename() + "\"");
    		responseBuilder.header("Content-Length", fileResource.getFileSize());
    		responseBuilder.entity(bis);
    		
    		
            response = responseBuilder.build();
            
        } catch (ForbiddenAccessException ex) {
            LOG.error("documentsDownloadFile", ex);
            response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Access Denied", ex.getMessage());
        } catch(DocumentManagementException ex) {
        	 LOG.error("documentsDownloadFile", ex);
        	response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Document Management Error", ex.getMessage());
        } catch(Exception ex) {
        	 LOG.error("documentsDownloadFile", ex);
        	response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Server Error", "Unhandled Server error.");
        } finally {
        	requestTimer.observeDuration();
        }
        
        return response;
    }

    @Override
    public Response documentsPutFileMetadata(String id, String data, HttpHeaders headers) {
    	requestsCounter.labels("documentsPutFileMetadata").inc();
    	
    	Histogram.Timer requestTimer = requestLatency.labels("documentsPutFileMetadata").startTimer();
    	
       Response response = null;
       boolean validationFailure = false;
       try {
        	DocumentManagementService dmsService = serviceUtil.getServiceClient(headers);
        	Gson gson = new GsonBuilder().create();
        	
        	@SuppressWarnings("unchecked")
    		Map<String, String> dataMap = gson.fromJson(data, Map.class);
    		
    		String expiryDate = dataMap.get("expiryDate");
    		String generalVisibility = dataMap.get("generalVisibility");
    		String ocioSecurityClassification = dataMap.get("ocioSecurityClassification");
            FileResource fr = dmsService.getFileByID(id);
            
            
            if(expiryDate != null && !expiryDate.trim().isEmpty()) {
            	if(DateUtil.isValidDateFormat(expiryDate)) {
            		String dmsDate = DateUtil.convertISOStringToDMSFormat(expiryDate);
            		fr.setExpireyDate(dmsDate);
            	} else {
            		validationFailure = true;
            	}
            }
            
            if(generalVisibility != null && !generalVisibility.trim().isEmpty()) {
            	fr.getFileMetadata().getSecurityMetadata().setGeneralVisibility(generalVisibility);
            }
            
            if(ocioSecurityClassification != null && !ocioSecurityClassification.trim().isEmpty()) {
            	fr.getFileMetadata().setOCIOSecurityClassification(ocioSecurityClassification);
            }
            
            if(!validationFailure) {
            	if(!fr.getIsCheckedOut()) {
            		dmsService.checkoutFile(id);
            	}
            	
            	FileResource fileResource = dmsService.updateFileMetadata(fr);
            	
                String jsonString = gson.toJson(fileResource);
                response =  Response.ok().entity(jsonString).build();
            } else {
            	response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Validation Error", "Invalid Date Format");
            }
            
        } catch (ForbiddenAccessException ex) {
            LOG.error("documentsPutFileMetadata", ex);
            response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Access Denied", ex.getMessage());
        } catch(DocumentManagementException ex) {
        	LOG.error("documentsPutFileMetadata", ex);
        	response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Document Management Error", ex.getMessage());
        } catch(ValidationException ex) {
        	LOG.error("documentsPutFileMetadata", ex);
        	response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Validation Error", ex.getMessage());
        } finally {
        	requestTimer.observeDuration();
        }
        
        return response;
    }

    
    @Override
    public Response documentsPutFolderMetadata(String id, String data, HttpHeaders headers) {
    	requestsCounter.labels("documentsPutFolderMetadata").inc();
    	Histogram.Timer requestTimer = requestLatency.labels("documentsPutFolderMetadata").startTimer();
    	
       Response response = null;
       boolean validationFailure = false;
       try {
        	DocumentManagementService dmsService = serviceUtil.getServiceClient(headers);
        	Gson gson = new GsonBuilder().create();
        	
        	@SuppressWarnings("unchecked")
    		Map<String, String> dataMap = gson.fromJson(data, Map.class);
    		
    		String generalVisibility = dataMap.get("generalVisibility");
    		String ocioSecurityClassification = dataMap.get("ocioSecurityClassification");
    		AbstractFolderResource fr = dmsService.getFolderByID(id);
            
            if(generalVisibility != null && !generalVisibility.trim().isEmpty()) {
            	fr.getSecurityMetadata().setGeneralVisibility(generalVisibility);
            	fr.getDefaultFileMetadata().getSecurityMetadata().setGeneralVisibility(generalVisibility);
            }
            
            if(ocioSecurityClassification != null && !ocioSecurityClassification.trim().isEmpty()) {
            	fr.getDefaultFileMetadata().setOCIOSecurityClassification(ocioSecurityClassification);
            }
            
            if(!validationFailure) {
            	
            	AbstractFolderResource folderResource = dmsService.updateFolderMetadata(fr);
                String jsonString = gson.toJson(folderResource);
                response =  Response.ok().entity(jsonString).build();
            } else {
            	response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Validation Error", "Invalid Date Format");
            }
            
        } catch (ForbiddenAccessException ex) {
            LOG.error("documentsPutFolderMetadata", ex);
            response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Access Denied", ex.getMessage());
        } catch(DocumentManagementException ex) {
        	LOG.error("documentsPutFolderMetadata", ex);
        	response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Document Management Error", ex.getMessage());
        } catch(ValidationException ex) {
        	LOG.error("documentsPutFolderMetadata", ex);
        	response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Validation Error", ex.getMessage());
        } finally {
        	requestTimer.observeDuration();
        }
        
        return response;
    }
    
    @Override
    public Response documentsGetFile(String id, HttpHeaders headers) {
    	requestsCounter.labels("documentsGetFile").inc();
    	Histogram.Timer requestTimer = requestLatency.labels("documentsGetFile").startTimer();

    	Response response = null;
        try {
        	DocumentManagementService dmsService = serviceUtil.getServiceClient(headers);
        	FileResource fileResource = dmsService.getFileByID(id);
            Gson gson = new GsonBuilder().create();
            String jsonString = gson.toJson(fileResource);
            response = Response.ok().entity(jsonString).build();
        }  catch (ForbiddenAccessException ex) {
            LOG.error("documentsGetFile", ex);
            response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Access Denied", ex.getMessage());
        } catch(DocumentManagementException ex) {
        	 LOG.error("documentsGetFile", ex);
        	response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Document Management Error", ex.getMessage());
        } catch(Exception ex) {
        	 LOG.error("documentsGetFile", ex);
        	response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Server Error", "Unhandled Server error.");
        } finally {
        	requestTimer.observeDuration();
        }
        
        return response;
    }
    
    @Override
    public Response documentsGetFolder(String id, HttpHeaders headers) {
    	requestsCounter.labels("documentsGetFolder").inc();
    	Histogram.Timer requestTimer = requestLatency.labels("documentsGetFolder").startTimer();
    	
    	Response response = null;
        try {
        	DocumentManagementService dmsService = serviceUtil.getServiceClient(headers);
        	FolderContentResource folderResource = dmsService.browseFolderByID(id, ServiceUtil.PAGE, ServiceUtil.MAX_ENTRY);
            Gson gson = new GsonBuilder().create();
            String jsonString = gson.toJson(folderResource);
            response = Response.ok().entity(jsonString).build();
        }  catch (ForbiddenAccessException ex) {
            LOG.error("documentsGet", ex);
            response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Access Denied", ex.getMessage());
        } catch(DocumentManagementException ex) {
        	 LOG.error("documentsGet", ex);
        	response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Document Management Error", ex.getMessage());
        } catch(Exception ex) {
        	 LOG.error("documentsGet", ex);
        	response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Server Error", "Unhandled Server error.");
        } finally {
        	requestTimer.observeDuration();
        }
        
        return response;
    }
    
    @Override
    public Response documentsGetFileHistory(String id, HttpHeaders headers) {
    	requestsCounter.labels("documentsGetFileHistory").inc();
    	Histogram.Timer requestTimer = requestLatency.labels("documentsGetFileHistory").startTimer();
    	
        Response response = null;
        try {
        	DocumentManagementService dmsService = serviceUtil.getServiceClient(headers);
        	
        	FileResource fileResource = dmsService.getFileByID(id);
            // get the history.
            RevisionsResource revisions = dmsService.getRevisions(fileResource, 1, 1000);
            Gson gson = new GsonBuilder().create();
            String jsonString = gson.toJson(revisions);
            response = Response.ok().entity(jsonString).build();
        } catch (ForbiddenAccessException ex) {
            LOG.error("documentsGetFileHistory", ex);
            response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Access Denied", ex.getMessage());
        } catch(DocumentManagementException ex) {
        	 LOG.error("documentsGetFileHistory", ex);
        	response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Document Management Error", ex.getMessage());
        } catch(Exception ex) {
        	 LOG.error("documentsGetFileHistory", ex);
        	response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Server Error", "Unhandled Server error.");
        } finally {
        	requestTimer.observeDuration();
        }

        return response;
    }


    @Override
    public Response documentsPutFile(String id, String filename, Attachment file, HttpHeaders headers) {
    	requestsCounter.labels("documentsPutFile").inc();
    	Histogram.Timer requestTimer = requestLatency.labels("documentsPutFile").startTimer();
    	
    	Response response = null;

        try {
        	DocumentManagementService dmsService = serviceUtil.getServiceClient(headers);
            FileResource fileResource = dmsService.getFileByID(id);

            InputStream fileStream = file.getObject(InputStream.class);
            File uploadDir = new File(this.uploadDirectory);
            File localFile = new File(uploadDir, filename);
            localFile.mkdirs();
            
            java.nio.file.Files.copy(fileStream, localFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            dmsService.checkoutFile(id);
            fileResource = dmsService.checkinFile(id, 
            		localFile.getAbsolutePath(),
            		fileResource.getDGDocType(), 
            		filename,
                    fileResource.getFileMetadata().getOCIOSecurityClassification(), 
                    null, 
                    fileResource.getFileMetadata().getSecurityMetadata().getGeneralVisibility(), 
                    null, null, null, null, null, null, null);
            
            localFile.delete();

            Gson gson = new GsonBuilder().create();
            String jsonString = gson.toJson(fileResource);
            response = Response.ok().entity(jsonString).build();

        } catch (ForbiddenAccessException ex) {
            LOG.error("documentsPutFile", ex);
            response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Access Denied", ex.getMessage());
        } catch(DocumentManagementException ex) {
        	 LOG.error("documentsPutFile", ex);
        	response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Document Management Error", ex.getMessage());
        } catch(Exception ex) {
        	 LOG.error("documentsPutFile", ex);
        	response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Server Error", "Unhandled Server error.");
        } finally {
        	requestTimer.observeDuration();
        }
        
        return response;
    }

    @Override
    public Response documentsPostFile(String id, String filename, Attachment file, HttpHeaders headers) {
    	requestsCounter.labels("documentsPostFile").inc();
    	Histogram.Timer requestTimer = requestLatency.labels("documentsPostFile").startTimer();
    	
    	Response response = null;
        try {
        	DocumentManagementService dmsService = serviceUtil.getServiceClient(headers);
        	AbstractFolderResource destinationFolder = null;
        	Gson gson = new GsonBuilder().create();
        
        	if(id != null) {
        		destinationFolder = dmsService.getFolderByID(id);
        	}else {
        		String filepath = serviceUtil.getRootDirectory();
            	AbstractFolderResource rootFolder = dmsService.getFolderByPath(filepath);
            	destinationFolder = rootFolder;
        	}
        	
        	
            InputStream fileStream = file.getObject(InputStream.class);
            File uploadDir = new File(this.uploadDirectory);
            File localFile = new File(uploadDir, filename);
            localFile.mkdirs();
            
            java.nio.file.Files.copy(fileStream, localFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            FileResource newFile = dmsService.createFile(
            		localFile.getAbsolutePath(), destinationFolder, 
            		DocType.NRSDocument.name(), 
            		filename,
            		serviceUtil.getDefaultOCIOClassification(), null, null, null, null, null, null, null, null, null);

            String jsonString = gson.toJson(newFile);

            response = Response.ok().entity(jsonString).build();
            localFile.delete();

        } catch (ForbiddenAccessException ex) {
            LOG.error("documentsPostFile", ex);
            response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Access Denied", ex.getMessage());
        } catch(DocumentManagementException ex) {
        	 LOG.error("documentsPostFile", ex);
        	response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Document Management Error", ex.getMessage());
        } catch(Exception ex) {
        	 LOG.error("documentsPostFile", ex);
        	response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Server Error", "Unhandled Server error.");
        } finally {
        	requestTimer.observeDuration();
        }
      
       return response;
    }

    @Override
    public Response documentsSearchGet(String fullTextWordsSearch, HttpHeaders headers) {
    	requestsCounter.labels("documentsSearchGet").inc();
    	Histogram.Timer requestTimer = requestLatency.labels("documentsSearchGet").startTimer();
    	
        Response response = null;

        try {
        	DocumentManagementService dmsService = serviceUtil.getServiceClient(headers);
            
        	AbstractFolderResource folderResource = dmsService.getFolderByPath(serviceUtil.getRootDirectory());
        	FilesResource searchFiles = dmsService.searchFiles(folderResource, null, null, null, null, null, null, null, null, null, null, null, null, null,
                    null, null, fullTextWordsSearch, true, null, null, null, null, null, null, null, 1, 30, null);
            Gson gson = new GsonBuilder().create();
            String jsonString = gson.toJson(searchFiles);
            response = Response.ok().entity(jsonString).build();
        } catch(DocumentManagementException ex) {
        	 LOG.error("documentsSearchGet", ex);
        	response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Document Management Error", ex.getMessage());
        } catch(Exception ex) {
        	 LOG.error("documentsSearchGet", ex);
        	response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Server Error", "Unhandled Server error.");
        } finally {
        	requestTimer.observeDuration();
        }

        return response;
    }

	@Override
	public Response documentsPostFolder(String id, String data, HttpHeaders headers) {
		requestsCounter.labels("documentsPostFolder").inc();
		Histogram.Timer requestTimer = requestLatency.labels("documentsPostFolder").startTimer();
		
		Response response = null;
		Gson gson = new GsonBuilder().create();
		@SuppressWarnings("unchecked")
		Map<String, String> dataMap = gson.fromJson(data, Map.class);
		
		String foldername = dataMap.get("name");
		String parentid = id;
		DocumentManagementService dmsService = serviceUtil.getServiceClient(headers);
        try {
        	AbstractFolderResource rootFolder = dmsService.getFolderByPath(serviceUtil.getRootDirectory());
        	if(rootFolder == null) {
        		response = MessageHelper.generateMessage(Status.NOT_FOUND, "Not Found", "Application root directory does not exists.");
        	} else {
        		AbstractFolderResource parentFolder = null;
        		
        		if(parentid != null && !parentid.trim().isEmpty()) {
        			parentFolder = dmsService.getFolderByID(parentid);
        		} else {
        			parentFolder = rootFolder;
        		}
        		
        		AbstractFolderResource newFolder = new FolderResource();
        		newFolder.setName(foldername);
        		
                SecurityMetadataResource securityMetadata = new SecurityMetadataResource();
                securityMetadata.setGeneralVisibility(serviceUtil.getDefaultDocumentVisibility());
             
                List<ACLResource> staffVisibility = new ArrayList<ACLResource>();
                ACLResource alc = new ACLResource();
                alc.setPermission(ServiceUtil.READ_WRITE);
                alc.setScope(serviceUtil.getSecurityScopes(ServiceUtil.READ_WRITE));
                staffVisibility.add(alc);
                alc.setPermission(ServiceUtil.READ);
                alc.setScope(serviceUtil.getSecurityScopes(ServiceUtil.READ));
                staffVisibility.add(alc);
                securityMetadata.setStaffVisibility(staffVisibility);
              
                newFolder.setParentFolderID(parentFolder.getItemID());
                newFolder.setSecurityMetadata(securityMetadata);
                newFolder.getDefaultFileMetadata().setSecurityMetadata(securityMetadata);
                newFolder.getDefaultFileMetadata().setOCIOSecurityClassification(serviceUtil.getDefaultOCIOClassification());
                                
                AbstractFolderResource result = dmsService.createFolder(parentFolder, newFolder);
                String jsonString = gson.toJson(result);
                response = Response.ok().entity(jsonString).build();
        	}
            
        }catch(ForbiddenAccessException ex) {
			LOG.error("documentsPostFolder", ex);
			response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Access Denied", ex.getMessage());
		}catch(DocumentManagementException ex) {
			LOG.error("documentsPostFolder", ex);
			response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Document Management Error", ex.getMessage());
		}catch (Exception ex) {
			LOG.error("documentsPostFolder", ex);
			response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Server Error", ex.getMessage());
		}finally {
			requestTimer.observeDuration();
		}
        
		return response;
	}

	@Override
	public Response documentsDeleteFolder(String id, HttpHeaders headers) {
		requestsCounter.labels("documentsDeleteFolder").inc();
		Histogram.Timer requestTimer = requestLatency.labels("documentsDeleteFolder").startTimer();
		
		Response response = null;
		try {
			DocumentManagementService dmsService = serviceUtil.getServiceClient(headers);
			AbstractFolderResource folderResource = dmsService.getFolderByID(id);
			dmsService.deleteFolder(folderResource);
			
			response = MessageHelper.generateMessage(Status.OK, "Success", "Folder " + id + " has been successfully deleted.");
			
		}catch(ForbiddenAccessException ex) {
			LOG.error("documentsDeleteFolder", ex);
			response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Access Denied", ex.getMessage());
		}catch(DocumentManagementException ex) {
			LOG.error("documentsDeleteFolder", ex);
			response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Document Management Error", ex.getMessage());
		}catch (Exception ex) {
			LOG.error("documentsDeleteFolder", ex);
			response = MessageHelper.generateMessage(Status.INTERNAL_SERVER_ERROR, "Server Error", ex.getMessage());
		}finally {
			requestTimer.observeDuration();
		}
		
		return response;
	}
    
}

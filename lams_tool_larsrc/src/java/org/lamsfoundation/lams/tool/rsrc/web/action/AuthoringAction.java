/****************************************************************
 * Copyright (C) 2005 LAMS Foundation (http://lamsfoundation.org)
 * =============================================================
 * License Information: http://lamsfoundation.org/licensing/lams/2.0/
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 * USA
 * 
 * http://www.gnu.org/licenses/gpl.txt
 * ****************************************************************
 */
/* $$Id$$ */
package org.lamsfoundation.lams.tool.rsrc.web.action;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.upload.FormFile;
import org.lamsfoundation.lams.authoring.web.AuthoringConstants;
import org.lamsfoundation.lams.contentrepository.client.IToolContentHandler;
import org.lamsfoundation.lams.tool.ToolAccessMode;
import org.lamsfoundation.lams.tool.rsrc.ResourceConstants;
import org.lamsfoundation.lams.tool.rsrc.model.Resource;
import org.lamsfoundation.lams.tool.rsrc.model.ResourceAttachment;
import org.lamsfoundation.lams.tool.rsrc.model.ResourceItem;
import org.lamsfoundation.lams.tool.rsrc.model.ResourceItemInstruction;
import org.lamsfoundation.lams.tool.rsrc.model.ResourceUser;
import org.lamsfoundation.lams.tool.rsrc.service.IResourceService;
import org.lamsfoundation.lams.tool.rsrc.service.ResourceApplicationException;
import org.lamsfoundation.lams.tool.rsrc.service.UploadResourceFileException;
import org.lamsfoundation.lams.tool.rsrc.util.ResourceItemComparator;
import org.lamsfoundation.lams.tool.rsrc.util.ResourceWebUtils;
import org.lamsfoundation.lams.tool.rsrc.web.form.ResourceForm;
import org.lamsfoundation.lams.tool.rsrc.web.form.ResourceItemForm;
import org.lamsfoundation.lams.usermanagement.dto.UserDTO;
import org.lamsfoundation.lams.util.WebUtil;
import org.lamsfoundation.lams.web.session.SessionManager;
import org.lamsfoundation.lams.web.util.AttributeNames;
import org.lamsfoundation.lams.web.util.SessionMap;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;


/**
 * @author Steve.Ni
 * @version $Revision$
 */
public class AuthoringAction extends Action {
	private static final int INIT_INSTRUCTION_COUNT = 2;
	private static final String INSTRUCTION_ITEM_DESC_PREFIX = "instructionItemDesc";
	private static final String INSTRUCTION_ITEM_COUNT = "instructionCount";
	private static final String ITEM_TYPE = "itemType";
	
	private static Logger log = Logger.getLogger(AuthoringAction.class);
	
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws Exception{
		
		String param = mapping.getParameter();
		//-----------------------Resource Author function ---------------------------
		if(param.equals("start")){
			ToolAccessMode mode = getAccessMode(request);
			//teacher mode "check for new" button enter.
			if(mode != null)
				request.setAttribute(AttributeNames.ATTR_MODE,mode.toString());
			else
				request.setAttribute(AttributeNames.ATTR_MODE,ToolAccessMode.AUTHOR.toString());
			return start(mapping, form, request, response);
		}
		if (param.equals("definelater")) {
			//update define later flag to true
			Long contentId = new Long(WebUtil.readLongParam(request,AttributeNames.PARAM_TOOL_CONTENT_ID));
			IResourceService service = getResourceService();
			Resource resource = service.getResourceByContentId(contentId);
			
			boolean isEditable = ResourceWebUtils.isResourceEditable(resource);
			if(!isEditable){
				request.setAttribute(ResourceConstants.PAGE_EDITABLE, new Boolean(isEditable));
				return mapping.findForward("forbidden");
			}
			
			if(!resource.isContentInUse()){
				resource.setDefineLater(true);
				service.saveOrUpdateResource(resource);
			}
			
			request.setAttribute(AttributeNames.ATTR_MODE,ToolAccessMode.TEACHER.toString());
			return start(mapping, form, request, response);
		}		
	  	if (param.equals("initPage")) {
       		return initPage(mapping, form, request, response);
        }

	  	if (param.equals("updateContent")) {
       		return updateContent(mapping, form, request, response);
        }
        if (param.equals("uploadOnlineFile")) {
       		return uploadOnline(mapping, form, request, response);
        }
        if (param.equals("uploadOfflineFile")) {
       		return uploadOffline(mapping, form, request, response);
        }
        if (param.equals("deleteOnlineFile")) {
        	return deleteOnlineFile(mapping, form, request, response);
        }
        if (param.equals("deleteOfflineFile")) {
        	return deleteOfflineFile(mapping, form, request, response);
        }
        //----------------------- Add resource item function ---------------------------
        if (param.equals("newItemInit")) {
        	return newItemlInit(mapping, form, request, response);
        }
        if (param.equals("editItemInit")) {
        	return editItemInit(mapping, form, request, response);
        }
        if (param.equals("saveOrUpdateItem")) {
        	return saveOrUpdateItem(mapping, form, request, response);
        }
        if (param.equals("removeItem")) {
        	return removeItem(mapping, form, request, response);
        }
        //-----------------------Resource Item Instruction function ---------------------------
	  	if (param.equals("newInstruction")) {
       		return newInstruction(mapping, form, request, response);
        }
	  	if (param.equals("removeInstruction")) {
	  		return removeInstruction(mapping, form, request, response);
	  	}
	  	if (param.equals("removeItemAttachment")) {
	  		return removeItemAttachment(mapping, form, request, response);
	  	}

        return mapping.findForward(ResourceConstants.ERROR);
	}

	/**
	 * Remove resource item attachment, such as single file, learning object ect. It is a ajax call and just temporarily 
	 * remove from page, all permenant change will happen only when user sumbit this resource item again. 
	 * 
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 */
	private ActionForward removeItemAttachment(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
		request.setAttribute("itemAttachment", null);
    	return mapping.findForward(ResourceConstants.SUCCESS);
    }

	/**
	 * Remove resource item from HttpSession list and update page display. As authoring rule, all persist only happen when 
	 * user submit whole page. So this remove is just impact HttpSession values.
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 */
	private ActionForward removeItem(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
		
//		get back sessionMAP
		String sessionMapID = WebUtil.readStrParam(request, ResourceConstants.ATTR_SESSION_MAP_ID);
		SessionMap sessionMap = (SessionMap)request.getSession().getAttribute(sessionMapID);
		
		int itemIdx = NumberUtils.stringToInt(request.getParameter(ResourceConstants.PARAM_ITEM_INDEX),-1);
		if(itemIdx != -1){
			SortedSet<ResourceItem> resourceList = getResourceItemList(sessionMap);
			List<ResourceItem> rList = new ArrayList<ResourceItem>(resourceList);
			ResourceItem item = rList.remove(itemIdx);
			resourceList.clear();
			resourceList.addAll(rList);
			//add to delList
			List delList = getDeletedResourceItemList(sessionMap);
			delList.add(item);
		}	
		
		request.setAttribute(ResourceConstants.ATTR_SESSION_MAP_ID, sessionMapID);
		return mapping.findForward(ResourceConstants.SUCCESS);
	}
	
	/**
	 * Display edit page for existed resource item.
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 */
	private ActionForward editItemInit(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
		
//		get back sessionMAP
		String sessionMapID = WebUtil.readStrParam(request, ResourceConstants.ATTR_SESSION_MAP_ID);
		SessionMap sessionMap = (SessionMap)request.getSession().getAttribute(sessionMapID);
		
		int itemIdx = NumberUtils.stringToInt(request.getParameter(ResourceConstants.PARAM_ITEM_INDEX),-1);
		ResourceItem item = null;
		if(itemIdx != -1){
			SortedSet<ResourceItem> resourceList = getResourceItemList(sessionMap);
			List<ResourceItem> rList = new ArrayList<ResourceItem>(resourceList);
			item = rList.get(itemIdx);
			if(item != null){
				populateItemToForm(itemIdx, item,(ResourceItemForm) form,request);
			}
		}		
		return findForward(item==null?-1:item.getType(),mapping);
	}
	/**
	 * Display empty page for new resource item.
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 */
	private ActionForward newItemlInit(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
		String sessionMapID = WebUtil.readStrParam(request, ResourceConstants.ATTR_SESSION_MAP_ID);
		((ResourceItemForm)form).setSessionMapID(sessionMapID);
		
		short type = (short) NumberUtils.stringToInt(request.getParameter(ITEM_TYPE));
		List instructionList = new ArrayList(INIT_INSTRUCTION_COUNT);
		for(int idx=0;idx<INIT_INSTRUCTION_COUNT;idx++){
			instructionList.add("");
		}
		request.setAttribute("instructionList",instructionList);
		return findForward(type,mapping);
	}
	/**
	 * This method will get necessary information from resource item form and save or update into 
	 * <code>HttpSession</code> ResourceItemList. Notice, this save is not persist them into database,  
	 * just save <code>HttpSession</code> temporarily. Only they will be persist when the entire authoring 
	 * page is being persisted.
	 *  
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 * @throws ServletException
	 */
	private ActionForward saveOrUpdateItem(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response){
		//get instructions:
		List<String> instructionList = getInstructionsFromRequest(request);
		
		ResourceItemForm itemForm = (ResourceItemForm)form;
		ActionErrors errors = validateResourceItem(itemForm);
		
		if(!errors.isEmpty()){
			this.addErrors(request,errors);
			request.setAttribute(ResourceConstants.ATTR_INSTRUCTION_LIST,instructionList);
			return findForward(itemForm.getItemType(),mapping);
		}
		
		try {
			extractFormToResourceItem(request, instructionList, itemForm);
		} catch (Exception e) {
			//any upload exception will display as normal error message rather then throw exception directly
			errors.add(ActionMessages.GLOBAL_MESSAGE,new ActionMessage(ResourceConstants.ERROR_MSG_UPLOAD_FAILED,e.getMessage()));
			if(!errors.isEmpty()){
				this.addErrors(request,errors);
				request.setAttribute(ResourceConstants.ATTR_INSTRUCTION_LIST,instructionList);
				return findForward(itemForm.getItemType(),mapping);
			}
		}
		//set session map ID so that itemlist.jsp can get sessionMAP
		request.setAttribute(ResourceConstants.ATTR_SESSION_MAP_ID, itemForm.getSessionMapID());
		//return null to close this window
		return mapping.findForward(ResourceConstants.SUCCESS);
	}

	/**
	 * Ajax call, will add one more input line for new resource item instruction.
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 */
	private ActionForward newInstruction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
		int count = NumberUtils.stringToInt(request.getParameter(INSTRUCTION_ITEM_COUNT),0);
		List instructionList = new ArrayList(++count);
		for(int idx=0;idx<count;idx++){
			String item = request.getParameter(INSTRUCTION_ITEM_DESC_PREFIX+idx);
			if(item == null)
				instructionList.add("");
			else
				instructionList.add(item);
		}
		request.setAttribute(ResourceConstants.ATTR_INSTRUCTION_LIST,instructionList);
		return mapping.findForward(ResourceConstants.SUCCESS);
	}
	/**
	 * Ajax call, remove the given line of instruction of resource item.
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 */
	private ActionForward removeInstruction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
		int count = NumberUtils.stringToInt(request.getParameter(INSTRUCTION_ITEM_COUNT),0);
		int removeIdx = NumberUtils.stringToInt(request.getParameter("removeIdx"),-1);
		List instructionList = new ArrayList(count-1);
		for(int idx=0;idx<count;idx++){
			String item = request.getParameter(INSTRUCTION_ITEM_DESC_PREFIX+idx);
			if(idx == removeIdx)
				continue;
			if(item == null)
				instructionList.add("");
			else
				instructionList.add(item);
		}
		request.setAttribute(ResourceConstants.ATTR_INSTRUCTION_LIST,instructionList);
		return mapping.findForward(ResourceConstants.SUCCESS);
	}

	/**
	 * Read resource data from database and put them into HttpSession. It will redirect to init.do directly after this
	 * method run successfully. 
	 *  
	 * This method will avoid read database again and lost un-saved resouce item lost when user "refresh page",
	 * @throws ServletException 
	 * 
	 */
	private ActionForward start(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws ServletException {
		
		//save toolContentID into HTTPSession
		Long contentId = new Long(WebUtil.readLongParam(request,ResourceConstants.PARAM_TOOL_CONTENT_ID));
		
//		get back the resource and item list and display them on page
		IResourceService service = getResourceService();

		List<ResourceItem> items = null;
		Resource resource = null;
		ResourceForm resourceForm = (ResourceForm)form;
		
		// Get contentFolderID and save to form.
		String contentFolderID = WebUtil.readStrParam(request, AttributeNames.PARAM_CONTENT_FOLDER_ID);
		resourceForm.setContentFolderID(contentFolderID);
				
		//initial Session Map 
		SessionMap sessionMap = new SessionMap();
		request.getSession().setAttribute(sessionMap.getSessionID(), sessionMap);
		resourceForm.setSessionMapID(sessionMap.getSessionID());
		
		try {
			resource = service.getResourceByContentId(contentId);
			//if resource does not exist, try to use default content instead.
			if(resource == null){
				resource = service.getDefaultContent(contentId);
				if(resource.getResourceItems() != null){
					items = new ArrayList<ResourceItem>(resource.getResourceItems());
				}else
					items = null;
			}else
				items = service.getAuthoredItems(resource.getUid());
			
			resourceForm.setResource(resource);

			//initialize instruction attachment list
			List attachmentList = getAttachmentList(sessionMap);
			attachmentList.clear();
			attachmentList.addAll(resource.getAttachments());
		} catch (Exception e) {
			log.error(e);
			throw new ServletException(e);
		}
		
		//init it to avoid null exception in following handling
		if(items == null)
			items = new ArrayList<ResourceItem>();
		else{
			ResourceUser resourceUser = null;
			//handle system default question: createBy is null, now set it to current user
			for (ResourceItem item : items) {
				if(item.getCreateBy() == null){
					if(resourceUser == null){
						//get back login user DTO
						HttpSession ss = SessionManager.getSession();
						UserDTO user = (UserDTO) ss.getAttribute(AttributeNames.USER);
						resourceUser = new ResourceUser(user,resource);
					}
					item.setCreateBy(resourceUser);
				}
			}
		}
		//init resource item list
		SortedSet<ResourceItem> resourceItemList = getResourceItemList(sessionMap);
		resourceItemList.clear();
		resourceItemList.addAll(items);
		
		sessionMap.put(ResourceConstants.ATTR_RESOURCE_FORM, resourceForm);
		return mapping.findForward(ResourceConstants.SUCCESS);
	}


	/**
	 * Display same entire authoring page content from HttpSession variable.
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 * @throws ServletException 
	 */
	private ActionForward initPage(ActionMapping mapping, ActionForm form, HttpServletRequest request,
			HttpServletResponse response) throws ServletException {
		String sessionMapID = WebUtil.readStrParam(request, ResourceConstants.ATTR_SESSION_MAP_ID);
		SessionMap sessionMap = (SessionMap)request.getSession().getAttribute(sessionMapID);
		ResourceForm existForm = (ResourceForm) sessionMap.get(ResourceConstants.ATTR_RESOURCE_FORM);
		
		ResourceForm resourceForm = (ResourceForm )form;
		try {
			PropertyUtils.copyProperties(resourceForm, existForm);
		} catch (Exception e) {
			throw new ServletException(e);
		}
		
		ToolAccessMode mode = getAccessMode(request);
		if(mode.isAuthor())
			return mapping.findForward(ResourceConstants.SUCCESS);
		else
			return mapping.findForward(ResourceConstants.DEFINE_LATER);
	}
	/**
	 * This method will persist all inforamtion in this authoring page, include all resource item, information etc.
	 * 
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 * @throws ServletException 
	 */
	private ActionForward updateContent(ActionMapping mapping, ActionForm form, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		ResourceForm resourceForm = (ResourceForm)(form);
		
		//get back sessionMAP
		SessionMap sessionMap = (SessionMap)request.getSession().getAttribute(resourceForm.getSessionMapID());
		
		ToolAccessMode mode = getAccessMode(request);
    	
		ActionMessages errors = validate(resourceForm, mapping, request);
		if(!errors.isEmpty()){
			saveErrors(request, errors);
			if(mode.isAuthor())
	    		return mapping.findForward("author");
	    	else
	    		return mapping.findForward("monitor");			
		}
			
		
		Resource resource = resourceForm.getResource();
		IResourceService service = getResourceService();
		
		//**********************************Get Resource PO*********************
		Resource resourcePO = service.getResourceByContentId(resourceForm.getResource().getContentId());
		if(resourcePO == null){
			//new Resource, create it.
			resourcePO = resource;
			resourcePO.setCreated(new Timestamp(new Date().getTime()));
			resourcePO.setUpdated(new Timestamp(new Date().getTime()));
		}else{
			if(mode.isAuthor()){
				Long uid = resourcePO.getUid();
				PropertyUtils.copyProperties(resourcePO,resource);
				//get back UID
				resourcePO.setUid(uid);
			}else{ //if it is Teacher, then just update basic tab content (definelater)
				resourcePO.setInstructions(resource.getInstructions());
				resourcePO.setTitle(resource.getTitle());
//				change define later status
				resourcePO.setDefineLater(false);
			}
			resourcePO.setUpdated(new Timestamp(new Date().getTime()));
		}
		
		//*******************************Handle user*******************
		//try to get form system session
		HttpSession ss = SessionManager.getSession();
		//get back login user DTO
		UserDTO user = (UserDTO) ss.getAttribute(AttributeNames.USER);
		ResourceUser resourceUser = service.getUserByIDAndContent(new Long(user.getUserID().intValue())
						,resourceForm.getResource().getContentId());
		if(resourceUser == null){
			resourceUser = new ResourceUser(user,resourcePO);
		}
		
		resourcePO.setCreatedBy(resourceUser);
		
		//**********************************Handle Authoring Instruction Attachement *********************
    	//merge attachment info
		//so far, attPOSet will be empty if content is existed. because PropertyUtils.copyProperties() is executed
		Set attPOSet = resourcePO.getAttachments();
		if(attPOSet == null)
			attPOSet = new HashSet();
		List attachmentList = getAttachmentList(sessionMap);
		List deleteAttachmentList = getDeletedAttachmentList(sessionMap);
		
		//current attachemnt in authoring instruction tab.
		Iterator iter = attachmentList.iterator();
		while(iter.hasNext()){
			ResourceAttachment newAtt = (ResourceAttachment) iter.next();
			attPOSet.add(newAtt);
		}
		attachmentList.clear();
		
		//deleted attachment. 2 possible types: one is persist another is non-persist before.
		iter = deleteAttachmentList.iterator();
		while(iter.hasNext()){
			ResourceAttachment delAtt = (ResourceAttachment) iter.next();
			iter.remove();
			//it is an existed att, then delete it from current attachmentPO
			if(delAtt.getUid() != null){
				Iterator attIter = attPOSet.iterator();
				while(attIter.hasNext()){
					ResourceAttachment att = (ResourceAttachment) attIter.next();
					if(delAtt.getUid().equals(att.getUid())){
						attIter.remove();
						break;
					}
				}
				service.deleteResourceAttachment(delAtt.getUid());
			}//end remove from persist value
		}
		
		//copy back
		resourcePO.setAttachments(attPOSet);
		//************************* Handle resource items *******************
		//Handle resource items
		Set itemList = new LinkedHashSet();
		SortedSet topics = getResourceItemList(sessionMap);
    	iter = topics.iterator();
    	while(iter.hasNext()){
    		ResourceItem item = (ResourceItem) iter.next();
    		if(item != null){
				//This flushs user UID info to message if this user is a new user. 
				item.setCreateBy(resourceUser);
				itemList.add(item);
    		}
    	}
    	resourcePO.setResourceItems(itemList);
    	//delete instructino file from database.
    	List delResourceItemList = getDeletedResourceItemList(sessionMap);
    	iter = delResourceItemList.iterator();
    	while(iter.hasNext()){
    		ResourceItem item = (ResourceItem) iter.next();
    		iter.remove();
    		if(item.getUid() != null)
    			service.deleteResourceItem(item.getUid());
    	}
    	//handle resource item attachment file:
    	List delItemAttList = getDeletedItemAttachmentList(sessionMap);
		iter = delItemAttList.iterator();
		while(iter.hasNext()){
			ResourceItem delAtt = (ResourceItem) iter.next();
			iter.remove();
		}
		
		//if miniview number is bigger than available items, then set it topics size
		if(resourcePO.getMiniViewResourceNumber() > topics.size())
			resourcePO.setMiniViewResourceNumber((topics.size()));
		//**********************************************
		//finally persist resourcePO again
		service.saveOrUpdateResource(resourcePO);
		
		//initialize attachmentList again
		attachmentList = getAttachmentList(sessionMap);
		attachmentList.addAll(resource.getAttachments());
		resourceForm.setResource(resourcePO);
		
		request.setAttribute(AuthoringConstants.LAMS_AUTHORING_SUCCESS_FLAG,Boolean.TRUE);
    	if(mode.isAuthor())
    		return mapping.findForward("author");
    	else
    		return mapping.findForward("monitor");
	}

	/**
	 * Handle upload online instruction files request. 
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 * @throws UploadResourceFileException 
	 */
	public ActionForward uploadOnline(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws UploadResourceFileException {
		return uploadFile(mapping, form, IToolContentHandler.TYPE_ONLINE,request);
	}
	/**
	 * Handle upload offline instruction files request.
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 * @throws UploadResourceFileException 
	 */
	public ActionForward uploadOffline(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws UploadResourceFileException {
		return uploadFile(mapping, form, IToolContentHandler.TYPE_OFFLINE,request);
	}
	/**
	 * Common method to upload online or offline instruction files request.
	 * @param mapping
	 * @param form
	 * @param type
	 * @param request
	 * @return
	 * @throws UploadResourceFileException 
	 */
	private ActionForward uploadFile(ActionMapping mapping, ActionForm form,
			String type,HttpServletRequest request) throws UploadResourceFileException {

		ResourceForm resourceForm = (ResourceForm) form;
		//get back sessionMAP
		SessionMap sessionMap = (SessionMap)request.getSession().getAttribute(resourceForm.getSessionMapID());

		FormFile file;
		if(StringUtils.equals(IToolContentHandler.TYPE_OFFLINE,type))
			file = (FormFile) resourceForm.getOfflineFile();
		else
			file = (FormFile) resourceForm.getOnlineFile();
		
		IResourceService service = getResourceService();
		//upload to repository
		ResourceAttachment  att = service.uploadInstructionFile(file, type);
		//handle session value
		List attachmentList = getAttachmentList(sessionMap);
		List deleteAttachmentList = getDeletedAttachmentList(sessionMap);
		//first check exist attachment and delete old one (if exist) to deletedAttachmentList
		Iterator iter = attachmentList.iterator();
		ResourceAttachment existAtt;
		while(iter.hasNext()){
			existAtt = (ResourceAttachment) iter.next();
			if(StringUtils.equals(existAtt.getFileName(),att.getFileName())){
				//if there is same name attachment, delete old one
				deleteAttachmentList.add(existAtt);
				iter.remove();
				break;
			}
		}
		//add to attachmentList
		attachmentList.add(att);

		return mapping.findForward(ResourceConstants.SUCCESS);

	}
	/**
	 * Delete offline instruction file from current Resource authoring page.
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 */
	public ActionForward deleteOfflineFile(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) {
		return deleteFile(mapping,request, response,form, IToolContentHandler.TYPE_OFFLINE);
	}
	/**
	 * Delete online instruction file from current Resource authoring page.
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 */
	public ActionForward deleteOnlineFile(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) {
		return deleteFile(mapping, request, response,form, IToolContentHandler.TYPE_ONLINE);
	}

	/**
	 * General method to delete file (online or offline)
	 * @param mapping 
	 * @param request
	 * @param response
	 * @param form 
	 * @param type 
	 * @return
	 */
	private ActionForward deleteFile(ActionMapping mapping, HttpServletRequest request, HttpServletResponse response, ActionForm form, String type) {
		Long versionID = new Long(WebUtil.readLongParam(request,ResourceConstants.PARAM_FILE_VERSION_ID));
		Long uuID = new Long(WebUtil.readLongParam(request,ResourceConstants.PARAM_FILE_UUID));
		
		//get back sessionMAP
		String sessionMapID = WebUtil.readStrParam(request, ResourceConstants.ATTR_SESSION_MAP_ID);
		SessionMap sessionMap = (SessionMap)request.getSession().getAttribute(sessionMapID);
		
		//handle session value
		List attachmentList = getAttachmentList(sessionMap);
		List deleteAttachmentList = getDeletedAttachmentList(sessionMap);
		//first check exist attachment and delete old one (if exist) to deletedAttachmentList
		Iterator iter = attachmentList.iterator();
		ResourceAttachment existAtt;
		while(iter.hasNext()){
			existAtt = (ResourceAttachment) iter.next();
			if(existAtt.getFileUuid().equals(uuID) && existAtt.getFileVersionId().equals(versionID)){
				//if there is same name attachment, delete old one
				deleteAttachmentList.add(existAtt);
				iter.remove();
			}
		}

		request.setAttribute(ResourceConstants.ATTR_FILE_TYPE_FLAG, type);
		request.setAttribute(ResourceConstants.ATTR_SESSION_MAP_ID, sessionMapID);
		return mapping.findForward(ResourceConstants.SUCCESS);

	}
	//*************************************************************************************
	// Private method 
	//*************************************************************************************
	/**
	 * Return ResourceService bean.
	 */
	private IResourceService getResourceService() {
	      WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServlet().getServletContext());
	      return (IResourceService) wac.getBean(ResourceConstants.RESOURCE_SERVICE);
	}
	/**
	 * @param request
	 * @return
	 */
	private List getAttachmentList(SessionMap sessionMap) {
		return getListFromSession(sessionMap,ResourceConstants.ATT_ATTACHMENT_LIST);
	}
	/**
	 * @param request
	 * @return
	 */
	private List getDeletedAttachmentList(SessionMap sessionMap) {
		return getListFromSession(sessionMap,ResourceConstants.ATTR_DELETED_ATTACHMENT_LIST);
	}
	/**
	 * List save current resource items.
	 * @param request
	 * @return
	 */
	private SortedSet<ResourceItem> getResourceItemList(SessionMap sessionMap) {
		SortedSet<ResourceItem> list = (SortedSet<ResourceItem>) sessionMap.get(ResourceConstants.ATTR_RESOURCE_ITEM_LIST);
		if(list == null){
			list = new TreeSet<ResourceItem>(new ResourceItemComparator());
			sessionMap.put(ResourceConstants.ATTR_RESOURCE_ITEM_LIST,list);
		}
		return list;
	}	
	/**
	 * List save deleted resource items, which could be persisted or non-persisted items. 
	 * @param request
	 * @return
	 */
	private List getDeletedResourceItemList(SessionMap sessionMap) {
		return getListFromSession(sessionMap,ResourceConstants.ATTR_DELETED_RESOURCE_ITEM_LIST);
	}
	/**
	 * If a resource item has attahment file, and the user edit this item and change the attachment
	 * to new file, then the old file need be deleted when submitting the whole authoring page.
	 * Save the file uuid and version id into ResourceItem object for temporarily use.
	 * @param request
	 * @return
	 */
	private List getDeletedItemAttachmentList(SessionMap sessionMap) {
		return getListFromSession(sessionMap,ResourceConstants.ATTR_DELETED_RESOURCE_ITEM_ATTACHMENT_LIST);
	}


	/**
	 * Get <code>java.util.List</code> from HttpSession by given name.
	 * 
	 * @param request
	 * @param name
	 * @return
	 */
	private List getListFromSession(SessionMap sessionMap,String name) {
		List list = (List) sessionMap.get(name);
		if(list == null){
			list = new ArrayList();
			sessionMap.put(name,list);
		}
		return list;
	}
	
	
	/**
	 * Get resource items instruction from <code>HttpRequest</code>
	 * @param request
	 */
	private List<String> getInstructionsFromRequest(HttpServletRequest request) {
		String list = request.getParameter("instructionList");
		String[] params = list.split("&");
		Map<String,String> paramMap = new HashMap<String,String>();
		String[] pair;
		for (String item: params) {
			pair = item.split("=");
			if(pair == null || pair.length != 2)
				continue;
			try {
				paramMap.put(pair[0],URLDecoder.decode(pair[1],"UTF-8"));
			} catch (UnsupportedEncodingException e) {
				log.error("Error occurs when decode instruction string:" + e.toString());
			}
		}
		
		int count = NumberUtils.stringToInt(paramMap.get(INSTRUCTION_ITEM_COUNT));
		List<String> instructionList = new ArrayList<String>();
		for(int idx=0;idx<count;idx++){
			String item = paramMap.get(INSTRUCTION_ITEM_DESC_PREFIX+idx);
			if(item == null)
				continue;
			instructionList.add(item);
		}
		return instructionList;
	}
	/**
	 * Get back relative <code>ActionForward</code> from request.
	 * @param type
	 * @param mapping
	 * @return
	 */
	private ActionForward findForward(short type, ActionMapping mapping) {
		ActionForward forward;
		switch (type) {
		case ResourceConstants.RESOURCE_TYPE_URL:
			forward = mapping.findForward("url");
			break;
		case ResourceConstants.RESOURCE_TYPE_FILE:
			forward = mapping.findForward("file");
			break;
		case ResourceConstants.RESOURCE_TYPE_WEBSITE:
			forward = mapping.findForward("website");
			break;
		case ResourceConstants.RESOURCE_TYPE_LEARNING_OBJECT:
			forward = mapping.findForward("learningobject");
			break;
		default:
			forward = null;
			break;
		}
		return forward;
	}


	/**
	 * This method will populate resource item information to its form for edit use.
	 * @param itemIdx
	 * @param item
	 * @param form
	 * @param request
	 */
	private void populateItemToForm(int itemIdx, ResourceItem item, ResourceItemForm form, HttpServletRequest request) {
		form.setDescription(item.getDescription());
		form.setTitle(item.getTitle());
		form.setUrl(item.getUrl());
		form.setOpenUrlNewWindow(item.isOpenUrlNewWindow());
		if(itemIdx >=0)
			form.setItemIndex(new Integer(itemIdx).toString());
		
		Set<ResourceItemInstruction> instructionList = item.getItemInstructions();
		List instructions = new ArrayList();
		for(ResourceItemInstruction in : instructionList){
			instructions.add(in.getDescription());
		}
		//FOR requirment from LDEV-754
		//add extra blank line for instructions
//		for(int idx=0;idx<INIT_INSTRUCTION_COUNT;idx++){
//			instructions.add("");
//		}
		if(item.getFileUuid() != null){
			form.setFileUuid(item.getFileUuid());
			form.setFileVersionId(item.getFileVersionId());
			form.setFileName(item.getFileName());
			form.setHasFile(true);
		}else
			form.setHasFile(false);
		
		request.setAttribute(ResourceConstants.ATTR_INSTRUCTION_LIST,instructions);
		
	}
	/**
	 * Extract web from content to resource item.
	 * @param request
	 * @param instructionList
	 * @param itemForm
	 * @throws ResourceApplicationException 
	 */
	private void extractFormToResourceItem(HttpServletRequest request, List<String> instructionList, ResourceItemForm itemForm) 
		throws Exception {
		/* BE CAREFUL: This method will copy nessary info from request form to a old or new ResourceItem instance.
		 * It gets all info EXCEPT ResourceItem.createDate and ResourceItem.createBy, which need be set when persisting 
		 * this resource item.
		 */
		
		SessionMap sessionMap = (SessionMap)request.getSession().getAttribute(itemForm.getSessionMapID());
		//check whether it is "edit(old item)" or "add(new item)"
		SortedSet<ResourceItem> resourceList = getResourceItemList(sessionMap);
		int itemIdx = NumberUtils.stringToInt(itemForm.getItemIndex(),-1);
		ResourceItem item = null;
		
		if(itemIdx == -1){ //add
			item = new ResourceItem();
			item.setCreateDate(new Timestamp(new Date().getTime()));
			resourceList.add(item);
		}else{ //edit
			List<ResourceItem> rList = new ArrayList<ResourceItem>(resourceList);
			item = rList.get(itemIdx);
		}
		short type = itemForm.getItemType();	
		item.setType(itemForm.getItemType());
		/* Set following fields regards to the type:
	    item.setFileUuid();
		item.setFileVersionId();
		item.setFileType();
		item.setFileName();
		
		item.getInitialItem()
		item.setImsSchema()
		item.setOrganizationXml()
		 */
		//if the item is edit (not new add) then the getFile may return null
		//it may throw exception, so put it as first, to avoid other invlidate update: 
		if(itemForm.getFile() != null){
			if(type == ResourceConstants.RESOURCE_TYPE_WEBSITE 
					||type == ResourceConstants.RESOURCE_TYPE_LEARNING_OBJECT
					||type == ResourceConstants.RESOURCE_TYPE_FILE){
				//if it has old file, and upload a new, then save old to deleteList
				ResourceItem delAttItem = new ResourceItem();
				boolean hasOld = false;
				if(item.getFileUuid() != null){
					hasOld = true;
					//be careful, This new ResourceItem object never be save into database
					//just temporarily use for saving fileUuid and versionID use:
					delAttItem.setFileUuid(item.getFileUuid());
					delAttItem.setFileVersionId(item.getFileVersionId());
				}
				IResourceService service = getResourceService();
				try {
					service.uploadResourceItemFile(item, itemForm.getFile());
				} catch (UploadResourceFileException e) {
					//if it is new add , then remove it!
					if(itemIdx == -1){ 
						resourceList.remove(item);
					}
					throw e;
				}
				//put it after "upload" to ensure deleted file added into list only no exception happens during upload 
				if(hasOld){
					List delAtt = getDeletedItemAttachmentList(sessionMap);
					delAtt.add(delAttItem);
				}
			}
		}
		item.setTitle(itemForm.getTitle());
		item.setCreateByAuthor(true);
		item.setHide(false);
		//set instrcutions
		Set instructions = new LinkedHashSet();
		int idx=0;
		for (String ins : instructionList) {
			ResourceItemInstruction rii = new ResourceItemInstruction();
			rii.setDescription(ins);
			rii.setSequenceId(idx++);
			instructions.add(rii);
		}
		item.setItemInstructions(instructions);

		if(type == ResourceConstants.RESOURCE_TYPE_URL){
			item.setUrl(itemForm.getUrl());
			item.setOpenUrlNewWindow(itemForm.isOpenUrlNewWindow());
		}
//		if(type == ResourceConstants.RESOURCE_TYPE_WEBSITE 
//				||itemForm.getItemType() == ResourceConstants.RESOURCE_TYPE_LEARNING_OBJECT){
			item.setDescription(itemForm.getDescription());
//		}
		
	}

	/**
	 * Vaidate resource item regards to their type (url/file/learning object/website zip file)
	 * @param itemForm
	 * @return
	 */
	private ActionErrors validateResourceItem(ResourceItemForm itemForm) {
		ActionErrors errors = new ActionErrors();
		if(StringUtils.isBlank(itemForm.getTitle()))
			errors.add(ActionMessages.GLOBAL_MESSAGE,new ActionMessage(ResourceConstants.ERROR_MSG_TITLE_BLANK));
		
		if(itemForm.getItemType() == ResourceConstants.RESOURCE_TYPE_URL){
			if(StringUtils.isBlank(itemForm.getUrl()))
				errors.add(ActionMessages.GLOBAL_MESSAGE,new ActionMessage(ResourceConstants.ERROR_MSG_URL_BLANK));
			//URL validation: Commom URL validate(1.3.0) work not very well: it can not support http://address:port format!!!
//			UrlValidator validator = new UrlValidator();
//			if(!validator.isValid(itemForm.getUrl()))
//				errors.add(ActionMessages.GLOBAL_MESSAGE,new ActionMessage(ResourceConstants.ERROR_MSG_INVALID_URL));
		}
//		if(itemForm.getItemType() == ResourceConstants.RESOURCE_TYPE_WEBSITE 
//				||itemForm.getItemType() == ResourceConstants.RESOURCE_TYPE_LEARNING_OBJECT){
//			if(StringUtils.isBlank(itemForm.getDescription()))
//				errors.add(ActionMessages.GLOBAL_MESSAGE,new ActionMessage(ResourceConstants.ERROR_MSG_DESC_BLANK));
//		}
		if(itemForm.getItemType() == ResourceConstants.RESOURCE_TYPE_WEBSITE 
				||itemForm.getItemType() == ResourceConstants.RESOURCE_TYPE_LEARNING_OBJECT
				||itemForm.getItemType() == ResourceConstants.RESOURCE_TYPE_FILE){
			//for edit validate: file already exist
			if(!itemForm.isHasFile() &&
				(itemForm.getFile() == null || StringUtils.isEmpty(itemForm.getFile().getFileName())))
				errors.add(ActionMessages.GLOBAL_MESSAGE,new ActionMessage(ResourceConstants.ERROR_MSG_FILE_BLANK));
		}
		return errors;
	}

	/**
	 * Get ToolAccessMode from HttpRequest parameters. Default value is AUTHOR mode.
	 * @param request
	 * @return
	 */
	private ToolAccessMode getAccessMode(HttpServletRequest request) {
		ToolAccessMode mode;
		String modeStr = request.getParameter(AttributeNames.ATTR_MODE);
		if(StringUtils.equalsIgnoreCase(modeStr,ToolAccessMode.TEACHER.toString()))
			mode = ToolAccessMode.TEACHER;
		else
			mode = ToolAccessMode.AUTHOR;
		return mode;
	}
	
	
	private ActionMessages validate(ResourceForm resourceForm, ActionMapping mapping, HttpServletRequest request) {
		ActionMessages errors = new ActionMessages();
		if (StringUtils.isBlank(resourceForm.getResource().getTitle())) {
			ActionMessage error = new ActionMessage("error.resource.item.title.blank");
			errors.add(ActionMessages.GLOBAL_MESSAGE, error);
		}
		//define it later mode(TEACHER) skip below validation.
		String modeStr = request.getParameter(AttributeNames.ATTR_MODE);
		if(StringUtils.equals(modeStr, ToolAccessMode.TEACHER.toString())){
			return errors;
		}

		//Some other validation outside basic Tab.
		
		return errors;
	}


}
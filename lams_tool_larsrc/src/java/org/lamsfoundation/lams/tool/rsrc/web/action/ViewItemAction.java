/****************************************************************
 * Copyright (C) 2005 LAMS Foundation (http://lamsfoundation.org)
 * =============================================================
 * License Information: http://lamsfoundation.org/licensing/lams/2.0/
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2.0 
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 * 
 * http://www.gnu.org/licenses/gpl.txt
 * ****************************************************************
 */

/* $Id$ */
package org.lamsfoundation.lams.tool.rsrc.web.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.lamsfoundation.lams.tool.ToolAccessMode;
import org.lamsfoundation.lams.tool.rsrc.ResourceConstants;
import org.lamsfoundation.lams.tool.rsrc.model.ResourceItem;
import org.lamsfoundation.lams.tool.rsrc.model.ResourceItemInstruction;
import org.lamsfoundation.lams.tool.rsrc.service.IResourceService;
import org.lamsfoundation.lams.tool.rsrc.web.form.InstructionNavForm;
import org.lamsfoundation.lams.usermanagement.dto.UserDTO;
import org.lamsfoundation.lams.web.session.SessionManager;
import org.lamsfoundation.lams.web.util.AttributeNames;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class ViewItemAction extends Action {
	
	private static final Logger log = Logger.getLogger(ViewItemAction.class);
	private static final String DEFUALT_PROTOCOL_REFIX = "http://";
	private static final String ALLOW_PROTOCOL_REFIX = new String("[http://|https://|ftp://|nntp://]");
	
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		
		String param = mapping.getParameter();
	    //-----------------------Display Learning Object function ---------------------------
	  	if (param.equals("reviewItem")) {
       		return reviewItem(mapping, form, request, response);
        }
	  	//for preview top frame html page use:
	  	if (param.equals("nextInstruction")) {
	  		return nextInstruction(mapping, form, request, response);
	  	}

        return mapping.findForward(ResourceConstants.ERROR);
	}

	private ActionForward nextInstruction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
		InstructionNavForm navForm = (InstructionNavForm) form;
		List list = navForm.getAllInstructions();
		if(list != null && navForm.getCurrent() < list.size()){
			//current is start from 1, so, this will return next.
			navForm.setInstruction((ResourceItemInstruction) list.get(navForm.getCurrent()));
			navForm.setCurrent(navForm.getCurrent()+1);
		}
		
		return mapping.findForward(ResourceConstants.SUCCESS);
	}

	private ActionForward reviewItem(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
		ToolAccessMode mode = (ToolAccessMode) request.getSession().getAttribute(AttributeNames.ATTR_MODE);
		ResourceItem item = null;
		if(mode.isAuthor()){
			int itemIdx = NumberUtils.stringToInt(request.getParameter(ResourceConstants.PARAM_ITEM_INDEX),-1);
			//authoring: does not save item yet, so only has ItemList from session and identity by Index
			List<ResourceItem> resourceList = getResourceItemList(request);
			item = resourceList.get(itemIdx);
		}else if(mode.isLearner()){
			Long itemUid = NumberUtils.createLong(request.getParameter(ResourceConstants.PARAM_RESOURCE_ITEM_UID));
			//save itemUid to HttpSession
			request.getSession().setAttribute(ResourceConstants.ATTR_RESOURCE_ITEM_UID,itemUid);
			Long sessionId =  (Long) request.getSession().getAttribute(ResourceConstants.ATTR_TOOL_SESSION_ID);
			//learning, list from database, so get item by Uid
//			get back the resource and item list and display them on page
			IResourceService service = getResourceService();			
			item = service.getResourceItemByUid(itemUid);
			HttpSession ss = SessionManager.getSession();
			//get back login user DTO
			UserDTO user = (UserDTO) ss.getAttribute(AttributeNames.USER);
			service.setItemAccess(itemUid,new Long(user.getUserID().intValue()),sessionId);
		}
		if(item != null){
			Set instructions = item.getItemInstructions();
			InstructionNavForm navForm = (InstructionNavForm) form;
			//For Learner upload item, its instruction will display description/comment fields in ReosourceItem.
			if(!item.isCreateByAuthor()){
				List<ResourceItemInstruction> navItems = new ArrayList<ResourceItemInstruction>(1);
				//create a new instruction and put ResourceItem description into it: just for display use.
				ResourceItemInstruction ins = new ResourceItemInstruction();
				ins.setSequenceId(1);
				ins.setDescription(item.getDescription());
				navItems.add(ins);
				navForm.setAllInstructions(navItems);
				instructions.add(ins);
			}else{
				navForm.setAllInstructions(new ArrayList(instructions));
			}
			navForm.setTitle(item.getTitle());
			navForm.setType(item.getType());
			navForm.setTotal(instructions.size());
			if(instructions.size() > 0){
				navForm.setCurrent(1);
				navForm.setInstruction((ResourceItemInstruction) instructions.iterator().next());
			}else{
				navForm.setCurrent(0);
				navForm.setInstruction(null);
			}
			if(item.getType() == ResourceConstants.RESOURCE_TYPE_LEARNING_OBJECT)
				request.getSession().setAttribute(ResourceConstants.ATT_LEARNING_OBJECT,item);
			//set url to content frame
			request.setAttribute(ResourceConstants.ATTR_RESOURCE_REVIEW_URL,getReviewUrl(item));
			return mapping.findForward(ResourceConstants.SUCCESS);
		}
		
		return mapping.findForward(ResourceConstants.ERROR);
	}
	//*************************************************************************************
	// Private method 
	//*************************************************************************************
	private IResourceService getResourceService() {
	      WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServlet().getServletContext());
	      return (IResourceService) wac.getBean(ResourceConstants.RESOURCE_SERVICE);
	}
	
	private Object getReviewUrl(ResourceItem item) {
		short type = item.getType();
		String url = null;
		switch (type) {
		case ResourceConstants.RESOURCE_TYPE_URL:
			url = protocol(item.getUrl());
			break;
		case ResourceConstants.RESOURCE_TYPE_FILE:
			url = "/download/?uuid="+item.getFileUuid()+"&preferDownload=false";
			break;
		case ResourceConstants.RESOURCE_TYPE_WEBSITE:
			url = "/download/?uuid="+item.getFileUuid()+"&preferDownload=false";
			break;
		case ResourceConstants.RESOURCE_TYPE_LEARNING_OBJECT:
			url = "/pages/learningobj/mainframe.jsp";
			break;
		}
		return url;
	}
	/**
	 * If there is not url prefix, such as http://, https:// or ftp:// etc, this 
	 * method will add default url protocol.
	 * 
	 * @param url
	 * @return
	 */
	private String protocol(String url) {
		if(url == null)
			return "";
		
		if(!url.matches("^" + ALLOW_PROTOCOL_REFIX + ".*"))
			url = DEFUALT_PROTOCOL_REFIX + url;
		
		return url;
	}

	/**
	 * List save current resource items.
	 * @param request
	 * @return
	 */
	private List getResourceItemList(HttpServletRequest request) {
		return getListFromSession(request,ResourceConstants.ATTR_RESOURCE_ITEM_LIST);
	}	
	/**
	 * Get <code>java.util.List</code> from HttpSession by given name.
	 * 
	 * @param request
	 * @param name
	 * @return
	 */
	private List getListFromSession(HttpServletRequest request,String name) {
		List list = (List) request.getSession().getAttribute(name);
		if(list == null){
			list = new ArrayList();
			request.getSession().setAttribute(name,list);
		}
		return list;
	}
	
}

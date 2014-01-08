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

/**
 * @author Ozgur Demirtas
 * 
 * Created on 8/03/2005
 * 
 */

/**
 * Tool path The URL path for the tool should be <lamsroot>/tool/$TOOL_SIG.
 * 
 * McStarterAction loads the default content and initializes the presentation Map
 * Requests can come either from authoring envuironment or from the monitoring environment for Edit Activity screen
 * 
 * Check McUtils.createAuthoringUser again User Management Service is ready
 * 
 * */

/**
 *
 * Tool Content:
 *
 * While tool's manage their own content, the LAMS core and the tools work together to create and use the content. 
 * The tool content id (toolContentID) is the key by which the tool and the LAMS core discuss data - 
 * it is generated by the LAMS core and supplied to the tool whenever content needs to be stored. 
 * The LAMS core will refer to the tool content id whenever the content needs to be used. 
 * Tool content will be covered in more detail in following sections.
 *
 * Each tool will have one piece of content that is the default content. 
 * The tool content id for this content is created as part of the installation process. 
 * Whenever a tool is asked for some tool content that does not exist, it should supply the default tool content. 
 * This will allow the system to render the normal screen, albeit with useless information, rather than crashing. 
 */

/**
 *
 * Authoring URL: 
 *
 * The tool must supply an authoring module, which will be called to create new content or edit existing content. It will be called by an authoring URL using the following format: ?????
 * The initial data displayed on the authoring screen for a new tool content id may be the default tool content.
 *
 * Authoring UI data consists of general Activity data fields and the Tool specific data fields.
 * The authoring interface will have three tabs. The mandatory (and suggested) fields are given. Each tool will have its own fields which it will add on any of the three tabs, as appropriate to the tabs' function.
 *
 * Basic: Displays the basic set of fields that are needed for the tool, and it could be expected that a new LAMS user would use. Mandatory fields: Title, Instructions.
 * Advanced: Displays the extra fields that would be used by experienced LAMS users. Optional fields: Lock On Finish, Make Responses Anonymous
 * Instructions: Displays the "instructions" fields for teachers. Mandatory fields: Online instructions, Offline instructions, Document upload.
 * The "Define Later" and "Run Offline" options are set on the Flash authoring part, and not on the tool's authoring screens.
 *
 * Preview The tool must be able to show the specified content as if it was running in a lesson. It will be the learner url with tool access mode set to ToolAccessMode.AUTHOR.
 * Export The tool must be able to export its tool content for part of the overall learning design export.
 *
 * The format of the serialization for export is XML. Tool will define extra namespace inside the <Content> element to add a new data element (type). Inside the data element, it can further define more structures and types as it seems fit.
 * The data elements must be "version" aware. The data elements must be "type" aware if they are to be shared between Tools.
 * 
 * 
 <!-- ========== Action Mapping Definitions =================================== -->

 <!--Authoring Starter  -->
 <action
 path="/authoringStarter"
 type="org.lamsfoundation.lams.tool.mc.web.McStarterAction"
 name="McAuthoringForm"
 scope="request"
 unknown="false"
 validate="false"
 >

 <forward
 name="load"
 path="/AuthoringMaincontent.jsp"
 redirect="false"
 />

 <forward
 name="loadViewOnly"
 path="/authoring/AuthoringTabsHolder.jsp"
 redirect="false"
 />

 <forward
 name="loadMonitoring"
 path="/monitoring/MonitoringMaincontent.jsp"
 redirect="false"
 />

 <forward
 name="refreshMonitoring"
 path="/monitoring/MonitoringMaincontent.jsp"
 redirect="false"
 />

 </action>  

 * 
 */

/* $$Id$$ */
package org.lamsfoundation.lams.tool.mc.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.Globals;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.lamsfoundation.lams.tool.mc.McAppConstants;
import org.lamsfoundation.lams.tool.mc.McApplicationException;
import org.lamsfoundation.lams.tool.mc.McComparator;
import org.lamsfoundation.lams.tool.mc.McGeneralAuthoringDTO;
import org.lamsfoundation.lams.tool.mc.McUtils;
import org.lamsfoundation.lams.tool.mc.pojos.McContent;
import org.lamsfoundation.lams.tool.mc.pojos.McUploadedFile;
import org.lamsfoundation.lams.tool.mc.service.IMcService;
import org.lamsfoundation.lams.tool.mc.service.McServiceProxy;
import org.lamsfoundation.lams.util.WebUtil;
import org.lamsfoundation.lams.web.util.AttributeNames;
import org.lamsfoundation.lams.web.util.SessionMap;

/**
 * 
 * @author Ozgur Demirtas
 * 
 *         A Map data structure is used to present the UI.
 */
public class McStarterAction extends Action implements McAppConstants {
    static Logger logger = Logger.getLogger(McStarterAction.class.getName());

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
	    HttpServletResponse response) throws IOException, ServletException, McApplicationException {

	McUtils.cleanUpSessionAbsolute(request);
	McAuthoringForm mcAuthoringForm = (McAuthoringForm) form;

	String contentFolderID = WebUtil.readStrParam(request, AttributeNames.PARAM_CONTENT_FOLDER_ID);
	mcAuthoringForm.setContentFolderID(contentFolderID);

	McGeneralAuthoringDTO mcGeneralAuthoringDTO = new McGeneralAuthoringDTO();
	mcGeneralAuthoringDTO.setContentFolderID(contentFolderID);

	Map mapQuestionContent = new TreeMap(new McComparator());

	mcAuthoringForm.resetRadioBoxes();

	IMcService mcService = null;
	if ((getServlet() == null) || (getServlet().getServletContext() == null)) {
	    mcService = mcAuthoringForm.getMcService();
	} else {
	    mcService = McServiceProxy.getMcService(getServlet().getServletContext());
	}

	mcGeneralAuthoringDTO.setCurrentTab("1");

	mcGeneralAuthoringDTO.setMonitoringOriginatedDefineLater(new Boolean(false).toString());
	String servletPath = request.getServletPath();
	String requestedModule = null;
	if (servletPath.indexOf("authoringStarter") > 0) {
	    // request is for authoring module
	    mcGeneralAuthoringDTO.setActiveModule(AUTHORING);
	    mcGeneralAuthoringDTO.setDefineLaterInEditMode(new Boolean(true).toString());
	    mcGeneralAuthoringDTO.setShowAuthoringTabs(new Boolean(true).toString());
	    mcAuthoringForm.setActiveModule(AUTHORING);
	    requestedModule = AUTHORING;
	} else {
	    // request is for define later module either direcly from define later url or monitoring url
	    mcGeneralAuthoringDTO.setActiveModule(DEFINE_LATER);
	    mcGeneralAuthoringDTO.setDefineLaterInEditMode(new Boolean(true).toString());
	    mcGeneralAuthoringDTO.setShowAuthoringTabs(new Boolean(false).toString());
	    mcAuthoringForm.setActiveModule(DEFINE_LATER);
	    requestedModule = DEFINE_LATER;

	    if (servletPath.indexOf("monitoring") > 0) {
		// request is from monitoring url
		mcGeneralAuthoringDTO.setMonitoringOriginatedDefineLater(new Boolean(true).toString());
	    }
	}
	mcGeneralAuthoringDTO.setRequestedModule(requestedModule);

	String sourceMcStarter = (String) request.getAttribute(SOURCE_MC_STARTER);

	boolean validateSignature = readSignature(request, mapping, mcService, mcGeneralAuthoringDTO, mcAuthoringForm);
	if (validateSignature == false) {
	    logger.debug("error during validation");
	}

	/* mark the http session as an authoring activity */
	mcGeneralAuthoringDTO.setTargetMode(TARGET_MODE_AUTHORING);

	/*
	 * find out whether the request is coming from monitoring module for EditActivity tab or from authoring
	 * environment url
	 */
	String strToolContentID = "";
	/* the authoring url must be passed a tool content id */
	strToolContentID = request.getParameter(AttributeNames.PARAM_TOOL_CONTENT_ID);
	mcGeneralAuthoringDTO.setToolContentID(strToolContentID);

	SessionMap sessionMap = new SessionMap();
	List sequentialCheckedCa = new LinkedList();
	sessionMap.put(ATTACHMENT_LIST_KEY, new ArrayList());
	sessionMap.put(DELETED_ATTACHMENT_LIST_KEY, new ArrayList());
	sessionMap.put(ACTIVITY_TITLE_KEY, "");
	sessionMap.put(ACTIVITY_INSTRUCTIONS_KEY, "");
	mcAuthoringForm.setHttpSessionID(sessionMap.getSessionID());
	mcGeneralAuthoringDTO.setHttpSessionID(sessionMap.getSessionID());

	String defaultContentId = null;
	if (strToolContentID == null) {
	    /*
	     * it is possible that the original request for authoring module is coming from monitoring url which keeps
	     * the TOOL_CONTENT_ID in the session
	     */

	    Long toolContentID = (Long) request.getSession().getAttribute(TOOL_CONTENT_ID);
	    if (toolContentID != null) {
		strToolContentID = toolContentID.toString();
		// cached strToolContentID from the session
	    } else {
		// we should IDEALLY not arrive here. The TOOL_CONTENT_ID is NOT available from the url or the session.
		/* use default content instead of giving a warning */
		defaultContentId = mcAuthoringForm.getDefaultContentIdStr();
		strToolContentID = defaultContentId;
	    }
	}

	if ((strToolContentID == null) || (strToolContentID.equals(""))) {
	    McUtils.cleanUpSessionAbsolute(request);
	    // return (mapping.findForward(ERROR_LIST));
	}

	mcAuthoringForm.setToolContentID(strToolContentID);

	/*
	 * find out if the passed tool content id exists in the db present user either a first timer screen with default
	 * content data or fetch the existing content.
	 * 
	 * if the toolcontentid does not exist in the db, create the default Map, there is no need to check if the
	 * content is locked in this case. It is always unlocked since it is the default content.
	 */

	String defaultContentIdStr = null;
	McContent mcContent = null;
	if (!existsContent(new Long(strToolContentID).longValue(), mcService)) {
	    /* fetch default content */
	    defaultContentIdStr = mcAuthoringForm.getDefaultContentIdStr();
	    mcContent = retrieveContent(request, mapping, mcAuthoringForm, mapQuestionContent, new Long(
		    defaultContentIdStr).longValue(), true, mcService, mcGeneralAuthoringDTO, sessionMap);

	} else {
	    /* it is possible that the content is in use by learners. */
	    mcContent = mcService.retrieveMc(new Long(strToolContentID));

	    if (mcService.studentActivityOccurredGlobal(mcContent)) {
		McUtils.cleanUpSessionAbsolute(request);
	    }
	    mcContent = retrieveContent(request, mapping, mcAuthoringForm, mapQuestionContent, new Long(
		    strToolContentID).longValue(), false, mcService, mcGeneralAuthoringDTO, sessionMap);
	}

	if ((mcGeneralAuthoringDTO.getOnlineInstructions() == null)
		|| (mcGeneralAuthoringDTO.getOnlineInstructions().length() == 0)) {
	    mcGeneralAuthoringDTO.setOnlineInstructions(DEFAULT_ONLINE_INST);
	    mcAuthoringForm.setOnlineInstructions(DEFAULT_ONLINE_INST);
	    sessionMap.put(ONLINE_INSTRUCTIONS_KEY, DEFAULT_ONLINE_INST);
	}

	if ((mcGeneralAuthoringDTO.getOfflineInstructions() == null)
		|| (mcGeneralAuthoringDTO.getOfflineInstructions().length() == 0)) {
	    mcGeneralAuthoringDTO.setOfflineInstructions(DEFAULT_OFFLINE_INST);
	    mcAuthoringForm.setOfflineInstructions(DEFAULT_OFFLINE_INST);
	    sessionMap.put(OFFLINE_INSTRUCTIONS_KEY, DEFAULT_OFFLINE_INST);
	}

	String destination = McUtils.getDestination(sourceMcStarter, requestedModule);
	Map mapQuestionContentLocal = mcGeneralAuthoringDTO.getMapQuestionContent();
	sessionMap.put(MAP_QUESTION_CONTENT_KEY, mapQuestionContent);

	AuthoringUtil authoringUtil = new AuthoringUtil();
	List listAddableQuestionContentDTO = authoringUtil.buildDefaultQuestionContent(mcContent, mcService);
	sessionMap.put(NEW_ADDABLE_QUESTION_CONTENT_KEY, listAddableQuestionContentDTO);

	request.getSession().setAttribute(sessionMap.getSessionID(), sessionMap);

	Map marksMap = authoringUtil.buildMarksMap();
	mcGeneralAuthoringDTO.setMarksMap(marksMap);
	mcGeneralAuthoringDTO.setMarkValue("1");

	List listQuestionContentDTOLocal = authoringUtil.buildDefaultQuestionContent(mcContent, mcService);
	Map passMarksMap = authoringUtil.buildDynamicPassMarkMap(listQuestionContentDTOLocal, true);
	mcGeneralAuthoringDTO.setPassMarksMap(passMarksMap);

	String totalMark = AuthoringUtil.getTotalMark(listQuestionContentDTOLocal);
	mcAuthoringForm.setTotalMarks(totalMark);
	mcGeneralAuthoringDTO.setTotalMarks(totalMark);

	String passMark = " ";

	if ((mcContent.getPassMark() != null) && (mcContent.getPassMark().intValue() != 0))
	    passMark = mcContent.getPassMark().toString();

	mcGeneralAuthoringDTO.setPassMarkValue(passMark);

	Map correctMap = authoringUtil.buildCorrectMap();
	mcGeneralAuthoringDTO.setCorrectMap(correctMap);

	request.setAttribute(MC_GENERAL_AUTHORING_DTO, mcGeneralAuthoringDTO);

	return (mapping.findForward(destination));
    }

    /**
     * retrives the existing content information from the db and prepares the data for presentation purposes.
     * 
     * @param request
     * @param mapping
     * @param mcAuthoringForm
     * @param mapQuestionContent
     * @param toolContentID
     * @return ActionForward
     */
    protected McContent retrieveContent(HttpServletRequest request, ActionMapping mapping,
	    McAuthoringForm mcAuthoringForm, Map mapQuestionContent, long toolContentID, boolean isDefaultContent,
	    IMcService mcService, McGeneralAuthoringDTO mcGeneralAuthoringDTO, SessionMap sessionMap) {
	McContent mcContent = mcService.retrieveMc(new Long(toolContentID));

	McUtils.populateAuthoringDTO(request, mcContent, mcGeneralAuthoringDTO);

	mcAuthoringForm.setSln(mcContent.isShowReport() ? "1" : "0");
	mcAuthoringForm.setQuestionsSequenced(mcContent.isQuestionsSequenced() ? "1" : "0");
	mcAuthoringForm.setRandomize(mcContent.isRandomize() ? "1" : "0");
	mcAuthoringForm.setDisplayAnswers(mcContent.isDisplayAnswers() ? "1" : "0");
	mcAuthoringForm.setShowMarks(mcContent.isShowMarks() ? "1" : "0");
	mcAuthoringForm.setUseSelectLeaderToolOuput(mcContent.isUseSelectLeaderToolOuput() ? "1" : "0");
	mcAuthoringForm.setPrefixAnswersWithLetters(mcContent.isPrefixAnswersWithLetters() ? "1" : "0");

	mcAuthoringForm.setRetries(mcContent.isRetries() ? "1" : "0");
	mcAuthoringForm.setReflect(mcContent.isReflect() ? "1" : "0");
	mcAuthoringForm.setReflectionSubject(mcContent.getReflectionSubject());

	mcGeneralAuthoringDTO.setSln(mcContent.isShowReport() ? "1" : "0");
	mcGeneralAuthoringDTO.setQuestionsSequenced(mcContent.isQuestionsSequenced() ? "1" : "0");
	mcGeneralAuthoringDTO.setRandomize(mcContent.isRandomize() ? "1" : "0");
	mcGeneralAuthoringDTO.setDisplayAnswers(mcContent.isDisplayAnswers() ? "1" : "0");
	mcGeneralAuthoringDTO.setRetries(mcContent.isRetries() ? "1" : "0");
	mcGeneralAuthoringDTO.setReflect(mcContent.isReflect() ? "1" : "0");
	mcGeneralAuthoringDTO.setReflectionSubject(mcContent.getReflectionSubject());

	List<McUploadedFile> attachmentList = mcService.retrieveMcUploadedFiles(mcContent);
	mcGeneralAuthoringDTO.setAttachmentList(attachmentList);
	mcGeneralAuthoringDTO.setDeletedAttachmentList(new ArrayList());

	sessionMap.put(ATTACHMENT_LIST_KEY, attachmentList);
	sessionMap.put(DELETED_ATTACHMENT_LIST_KEY, new ArrayList());

	mcGeneralAuthoringDTO.setIsDefineLater(new Boolean(mcContent.isDefineLater()).toString());

	mcGeneralAuthoringDTO.setActivityTitle(mcContent.getTitle());
	mcAuthoringForm.setTitle(mcContent.getTitle());

	mcGeneralAuthoringDTO.setActivityInstructions(mcContent.getInstructions());
	mcAuthoringForm.setInstructions(mcContent.getInstructions());

	sessionMap.put(ACTIVITY_TITLE_KEY, mcGeneralAuthoringDTO.getActivityTitle());
	sessionMap.put(ACTIVITY_INSTRUCTIONS_KEY, mcGeneralAuthoringDTO.getActivityInstructions());

	AuthoringUtil authoringUtil = new AuthoringUtil();
	List listQuestionContentDTO = authoringUtil.buildDefaultQuestionContent(mcContent, mcService);

	request.setAttribute(TOTAL_QUESTION_COUNT, new Integer(listQuestionContentDTO.size()));
	request.setAttribute(LIST_QUESTION_CONTENT_DTO, listQuestionContentDTO);
	sessionMap.put(LIST_QUESTION_CONTENT_DTO_KEY, listQuestionContentDTO);

	if (isDefaultContent) {
	    // overwriting default question
	    mcGeneralAuthoringDTO.setDefaultQuestionContent("Sample Question 1?");

	}

	mcGeneralAuthoringDTO.setMapQuestionContent(mapQuestionContent);
	mcGeneralAuthoringDTO.setOnlineInstructions(mcContent.getOnlineInstructions());
	mcGeneralAuthoringDTO.setOfflineInstructions(mcContent.getOfflineInstructions());

	mcAuthoringForm.setOnlineInstructions(mcContent.getOnlineInstructions());
	mcAuthoringForm.setOfflineInstructions(mcContent.getOfflineInstructions());
	sessionMap.put(ONLINE_INSTRUCTIONS_KEY, mcContent.getOnlineInstructions());
	sessionMap.put(OFFLINE_INSTRUCTIONS_KEY, mcContent.getOfflineInstructions());

	mcAuthoringForm.resetUserAction();
	return mcContent;
    }

    /**
     * each tool has a signature. MC tool's signature is stored in MY_SIGNATURE. The default tool content id and other
     * depending content ids are obtained in this method. if all the default content has been setup properly the method
     * persists DEFAULT_CONTENT_ID in the session.
     * 
     * @param request
     * @param mapping
     * @return ActionForward
     */
    public boolean readSignature(HttpServletRequest request, ActionMapping mapping, IMcService mcService,
	    McGeneralAuthoringDTO mcGeneralAuthoringDTO, McAuthoringForm mcAuthoringForm) {
	/*
	 * retrieve the default content id based on tool signature
	 */
	long defaultContentID = 0;
	try {
	    defaultContentID = mcService.getToolDefaultContentIdBySignature(MY_SIGNATURE);
	    if (defaultContentID == 0) {
		// default content id has not been setup
		return false;
	    }
	} catch (Exception e) {
	    logger.debug("error getting the default content id: " + e.getMessage());
	    persistError(request, "error.defaultContent.notSetup");
	    return false;
	}

	/* retrieve uid of the content based on default content id determined above */
	long contentUID = 0;
	try {
	    McContent mcContent = mcService.retrieveMc(new Long(defaultContentID));
	    if (mcContent == null) {
		logger.debug("Exception occured: No default content");
		persistError(request, "error.defaultContent.notSetup");
		return false;
	    }
	    contentUID = mcContent.getUid().longValue();
	} catch (Exception e) {
	    logger.debug("Exception occured: No default question content");
	    persistError(request, "error.defaultContent.notSetup");
	    return false;
	}

	mcGeneralAuthoringDTO.setDefaultContentIdStr(new Long(defaultContentID).toString());
	mcAuthoringForm.setDefaultContentIdStr(new Long(defaultContentID).toString());

	return true;
    }

    /**
     * @param long toolContentID
     * @return boolean determine whether a specific toolContentID exists in the db
     */
    protected boolean existsContent(long toolContentID, IMcService mcService) {
	McContent mcContent = mcService.retrieveMc(new Long(toolContentID));
	if (mcContent == null)
	    return false;

	return true;
    }

    /**
     * bridges define later url request to authoring functionality
     * 
     * 
     * @param mapping
     * @param form
     * @param request
     * @param response
     * @param mcService
     * @return
     * @throws IOException
     * @throws ServletException
     * @throws McApplicationException
     */
    public ActionForward executeDefineLater(ActionMapping mapping, McAuthoringForm mcAuthoringForm,
	    HttpServletRequest request, HttpServletResponse response, IMcService mcService) throws IOException,
	    ServletException, McApplicationException {
	return execute(mapping, mcAuthoringForm, request, response);
    }

    /**
     * persists error messages to request scope
     * 
     * @param request
     * @param message
     */
    public void persistError(HttpServletRequest request, String message) {
	ActionMessages errors = new ActionMessages();
	errors.add(Globals.ERROR_KEY, new ActionMessage(message));
	saveErrors(request, errors);
    }
}

/***************************************************************************
Copyright (C) 2005 LAMS Foundation (http://lamsfoundation.org)
License Information: http://lamsfoundation.org/licensing/lams/2.0/

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License version 2 as
published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
USA

http://www.gnu.org/licenses/gpl.txt
 * ***********************************************************************/

package org.lamsfoundation.lams.tool.qa.web.controller;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts.Globals;
import org.lamsfoundation.lams.learning.web.bean.ActivityPositionDTO;
import org.lamsfoundation.lams.learning.web.util.LearningWebUtil;
import org.lamsfoundation.lams.notebook.model.NotebookEntry;
import org.lamsfoundation.lams.notebook.service.CoreNotebookConstants;
import org.lamsfoundation.lams.rating.dto.ItemRatingCriteriaDTO;
import org.lamsfoundation.lams.rating.dto.ItemRatingDTO;
import org.lamsfoundation.lams.rating.dto.RatingCommentDTO;
import org.lamsfoundation.lams.rating.model.LearnerItemRatingCriteria;
import org.lamsfoundation.lams.tool.ToolAccessMode;
import org.lamsfoundation.lams.tool.exception.ToolException;
import org.lamsfoundation.lams.tool.qa.QaAppConstants;
import org.lamsfoundation.lams.tool.qa.QaContent;
import org.lamsfoundation.lams.tool.qa.QaQueContent;
import org.lamsfoundation.lams.tool.qa.QaQueUsr;
import org.lamsfoundation.lams.tool.qa.QaSession;
import org.lamsfoundation.lams.tool.qa.QaUsrResp;
import org.lamsfoundation.lams.tool.qa.dto.GeneralLearnerFlowDTO;
import org.lamsfoundation.lams.tool.qa.dto.QaQuestionDTO;
import org.lamsfoundation.lams.tool.qa.service.IQaService;
import org.lamsfoundation.lams.tool.qa.util.LearningUtil;
import org.lamsfoundation.lams.tool.qa.util.QaApplicationException;
import org.lamsfoundation.lams.tool.qa.util.QaComparator;
import org.lamsfoundation.lams.tool.qa.util.QaStringComparator;
import org.lamsfoundation.lams.tool.qa.util.QaUtils;
import org.lamsfoundation.lams.tool.qa.web.form.QaLearningForm;
import org.lamsfoundation.lams.usermanagement.dto.UserDTO;
import org.lamsfoundation.lams.util.DateUtil;
import org.lamsfoundation.lams.util.MessageService;
import org.lamsfoundation.lams.util.ValidationUtil;
import org.lamsfoundation.lams.util.WebUtil;
import org.lamsfoundation.lams.web.session.SessionManager;
import org.lamsfoundation.lams.web.util.AttributeNames;
import org.lamsfoundation.lams.web.util.SessionMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Ozgur Demirtas
 */
@Controller
@RequestMapping("/learning")
public class QaLearningController implements QaAppConstants {
    private static Logger logger = Logger.getLogger(QaLearningController.class.getName());

    @Autowired
    private IQaService qaService;

    @Autowired
    @Qualifier("qaMessageService")
    private MessageService messageService;

    @Autowired
    private WebApplicationContext applicationContext;

    @RequestMapping("/")
    public String unspecified() throws IOException, ServletException, ToolException {
	QaLearningController.logger.warn("dispatching unspecified...");
	return null;
    }

    @RequestMapping("/learning")
    public String execute(@ModelAttribute("learningForm") QaLearningForm learningForm, HttpServletRequest request)
	    throws IOException, ServletException, QaApplicationException {

	QaUtils.cleanUpSessionAbsolute(request);

	/* validate learning mode parameters */
	validateParameters(request, learningForm);
	String mode = learningForm.getMode();
	String toolSessionID = learningForm.getToolSessionID();

	/*
	 * By now, the passed tool session id MUST exist in the db by calling:
	 * public void createToolSession(Long toolSessionId, Long toolContentId) by the core.
	 *
	 * make sure this session exists in tool's session table by now.
	 */
	QaSession qaSession = qaService.getSessionById(new Long(toolSessionID).longValue());
	if (qaSession == null) {
	    QaUtils.cleanUpSessionAbsolute(request);
	    throw new ServletException("No session found");
	}

	QaContent qaContent = qaSession.getQaContent();
	if (qaContent == null) {
	    QaUtils.cleanUpSessionAbsolute(request);
	    throw new ServletException("No QA content found");
	}

	QaQueUsr user = null;
	if ((mode != null) && mode.equals(ToolAccessMode.TEACHER.toString())) {
	    // monitoring mode - user is specified in URL
	    // assessmentUser may be null if the user was force completed.
	    user = getSpecifiedUser(toolSessionID, WebUtil.readIntParam(request, AttributeNames.PARAM_USER_ID, false));
	} else {
	    user = getCurrentUser(toolSessionID);
	}
	Long userId = user.getQueUsrId();
	learningForm.setUserID(user.getQueUsrId().toString());
	request.setAttribute("qaLearningForm", learningForm);
	QaQueUsr groupLeader = null;
	if (qaContent.isUseSelectLeaderToolOuput()) {
	    groupLeader = qaService.checkLeaderSelectToolForSessionLeader(user, new Long(toolSessionID).longValue());

	    // forwards to the leaderSelection page
	    if (groupLeader == null && !mode.equals(ToolAccessMode.TEACHER.toString())) {

		List<QaQueUsr> groupUsers = qaService.getUsersBySessionId(new Long(toolSessionID).longValue());
		request.setAttribute(ATTR_GROUP_USERS, groupUsers);
		request.setAttribute(TOOL_SESSION_ID, toolSessionID);
		request.setAttribute(ATTR_CONTENT, qaContent);

		return "learning/WaitForLeader";
	    }

	    // check if leader has submitted all answers
	    if (groupLeader.isResponseFinalized() && !mode.equals(ToolAccessMode.TEACHER.toString())) {

		// in case user joins the lesson after leader has answers some answers already - we need to make sure
		// he has the same scratches as leader
		qaService.copyAnswersFromLeader(user, groupLeader);

		user.setResponseFinalized(true);
		qaService.updateUser(user);
	    }
	}

	/* holds the question contents for a given tool session and relevant content */
	Map mapQuestionStrings = new TreeMap(new QaComparator());
	Map<Integer, QaQuestionDTO> mapQuestions = new TreeMap<>();

	String httpSessionID = learningForm.getHttpSessionID();
	SessionMap<String, Object> sessionMap = httpSessionID == null ? null
		: (SessionMap<String, Object>) request.getSession().getAttribute(httpSessionID);
	if (sessionMap == null) {
	    sessionMap = new SessionMap<>();
	    Map mapSequentialAnswers = new HashMap();
	    sessionMap.put(MAP_SEQUENTIAL_ANSWERS_KEY, mapSequentialAnswers);
	    request.getSession().setAttribute(sessionMap.getSessionID(), sessionMap);
	    learningForm.setHttpSessionID(sessionMap.getSessionID());

	    sessionMap.put(AttributeNames.ATTR_LEARNER_CONTENT_FOLDER,
		    qaService.getLearnerContentFolder(new Long(toolSessionID), user.getQueUsrId()));
	}
	String sessionMapId = sessionMap.getSessionID();
	sessionMap.put(IS_DISABLED, qaContent.isLockWhenFinished() && user.isLearnerFinished()
		|| (mode != null) && mode.equals(ToolAccessMode.TEACHER.toString()));

	sessionMap.put(ATTR_GROUP_LEADER, groupLeader);
	boolean isUserLeader = qaService.isUserGroupLeader(user, new Long(toolSessionID));
	boolean lockWhenFinished = qaContent.isLockWhenFinished();
	sessionMap.put(ATTR_IS_USER_LEADER, isUserLeader);
	sessionMap.put(AttributeNames.ATTR_MODE, mode);
	sessionMap.put(ATTR_CONTENT, qaContent);
	sessionMap.put(AttributeNames.USER, user);

	GeneralLearnerFlowDTO generalLearnerFlowDTO = LearningUtil.buildGeneralLearnerFlowDTO(qaService, qaContent);
	generalLearnerFlowDTO.setUserUid(user.getQueUsrId().toString());
	generalLearnerFlowDTO.setHttpSessionID(sessionMapId);
	generalLearnerFlowDTO.setToolSessionID(toolSessionID);
	generalLearnerFlowDTO.setToolContentID(qaContent.getQaContentId().toString());
	generalLearnerFlowDTO.setReportTitleLearner(qaContent.getReportTitle());

	generalLearnerFlowDTO.setLockWhenFinished(new Boolean(lockWhenFinished).toString());
	generalLearnerFlowDTO.setNoReeditAllowed(qaContent.isNoReeditAllowed());
	generalLearnerFlowDTO.setReflection(new Boolean(qaContent.isReflect()).toString());
	generalLearnerFlowDTO.setReflectionSubject(qaContent.getReflectionSubject());

	NotebookEntry notebookEntry = qaService.getEntry(new Long(toolSessionID), CoreNotebookConstants.NOTEBOOK_TOOL,
		MY_SIGNATURE, userId.intValue());
	if (notebookEntry != null) {
	    //String notebookEntryPresentable = QaUtils.replaceNewLines(notebookEntry.getEntry());
	    learningForm.setEntryText(notebookEntry.getEntry());
	    generalLearnerFlowDTO.setNotebookEntry(notebookEntry.getEntry());
	}

	/*
	 * Is the tool activity been checked as Define Later in the property inspector?
	 */
	if (qaContent.isDefineLater()) {
	    QaUtils.cleanUpSessionAbsolute(request);
	    return "learning/defineLater";
	}

	ActivityPositionDTO activityPosition = LearningWebUtil.putActivityPositionInRequestByToolSessionId(
		new Long(toolSessionID), request, applicationContext.getServletContext());
	sessionMap.put(AttributeNames.ATTR_ACTIVITY_POSITION, activityPosition);

	/*
	 * fetch question content from content
	 */
	Iterator contentIterator = qaContent.getQaQueContents().iterator();
	while (contentIterator.hasNext()) {
	    QaQueContent qaQuestion = (QaQueContent) contentIterator.next();
	    if (qaQuestion != null) {
		int displayOrder = qaQuestion.getDisplayOrder();

		if (displayOrder != 0) {
		    /*
		     * add the question to the questions Map in the displayOrder
		     */
		    QaQuestionDTO questionDTO = new QaQuestionDTO(qaQuestion);
		    mapQuestions.put(displayOrder, questionDTO);

		    mapQuestionStrings.put(new Integer(displayOrder).toString(), qaQuestion.getQuestion());

		}
	    }
	}
	generalLearnerFlowDTO.setMapQuestions(mapQuestionStrings);
	generalLearnerFlowDTO.setMapQuestionContentLearner(mapQuestions);
	generalLearnerFlowDTO.setTotalQuestionCount(new Integer(mapQuestions.size()));
	learningForm.setTotalQuestionCount(new Integer(mapQuestions.size()).toString());

	String feedBackType = "";
	if (qaContent.isQuestionsSequenced()) {
	    feedBackType = FEEDBACK_TYPE_SEQUENTIAL;
	} else {
	    feedBackType = FEEDBACK_TYPE_COMBINED;
	}
	String userFeedback = feedBackType + generalLearnerFlowDTO.getTotalQuestionCount() + QUESTIONS;
	generalLearnerFlowDTO.setUserFeedback(userFeedback);

	generalLearnerFlowDTO.setRemainingQuestionCount(generalLearnerFlowDTO.getTotalQuestionCount().toString());
	generalLearnerFlowDTO.setInitialScreen(new Boolean(true).toString());

	request.setAttribute(GENERAL_LEARNER_FLOW_DTO, generalLearnerFlowDTO);

	/*
	 * by now, we know that the mode is either teacher or learner
	 * check if the mode is teacher and request is for Learner Progress
	 */
	if (mode.equals("teacher")) {
	    //start generating learner progress report for toolSessionID

	    /*
	     * the report should have the all entries for the users in this tool session,
	     * and display under the "my answers" section the answers for the user id in the url
	     */
//	    Long learnerProgressUserId = WebUtil.readLongParam(request, AttributeNames.PARAM_USER_ID, false);
	    generalLearnerFlowDTO.setRequestLearningReport(new Boolean(true).toString());
	    generalLearnerFlowDTO.setRequestLearningReportProgress(new Boolean(true).toString());
	    generalLearnerFlowDTO.setTeacherViewOnly(new Boolean(true).toString());

	    QaLearningController.refreshSummaryData(request, qaContent, qaSession, qaService, sessionMapId, user,
		    generalLearnerFlowDTO);
	    request.setAttribute(QaAppConstants.GENERAL_LEARNER_FLOW_DTO, generalLearnerFlowDTO);

	    return "learning/LearnerRep";
	}

	//check if there is submission deadline
	Date submissionDeadline = qaContent.getSubmissionDeadline();
	if (submissionDeadline != null) {
	    // store submission deadline to sessionMap
	    sessionMap.put(QaAppConstants.ATTR_SUBMISSION_DEADLINE, submissionDeadline);

	    HttpSession ss = SessionManager.getSession();
	    UserDTO learnerDto = (UserDTO) ss.getAttribute(AttributeNames.USER);
	    TimeZone learnerTimeZone = learnerDto.getTimeZone();
	    Date tzSubmissionDeadline = DateUtil.convertToTimeZoneFromDefault(learnerTimeZone, submissionDeadline);
	    Date currentLearnerDate = DateUtil.convertToTimeZoneFromDefault(learnerTimeZone, new Date());

	    // calculate whether submission deadline has passed, and if so forward to "submissionDeadline"
	    if (currentLearnerDate.after(tzSubmissionDeadline)) {

		//if ShowOtherAnswersAfterDeadline is enabled - show others answers
		if (qaContent.isShowOtherAnswersAfterDeadline()) {
		    generalLearnerFlowDTO.setLockWhenFinished(Boolean.TRUE.toString());
		    generalLearnerFlowDTO.setNoReeditAllowed(true);
		    //only for ActionForward refreshAllResults(..) method
		    sessionMap.put("noRefresh", true);
		    /*
		     * the report should have all the users' entries OR the report should have only the current
		     * session's entries
		     */
		    generalLearnerFlowDTO.setRequestLearningReport(new Boolean(true).toString());

		    QaLearningController.refreshSummaryData(request, qaContent, qaSession, qaService, sessionMapId,
			    user, generalLearnerFlowDTO);

		    if (user.isLearnerFinished()) {
			generalLearnerFlowDTO.setRequestLearningReportViewOnly(new Boolean(true).toString());
			return "learning/RevisitedLearnerRep";
		    } else {
			generalLearnerFlowDTO.setRequestLearningReportViewOnly(new Boolean(false).toString());
			return "learning/LearnerRep";
		    }

		    // show submissionDeadline page otherwise
		} else {
		    return "learning/submissionDeadline";
		}
	    }
	}

	/*
	 * Verify that userId does not already exist in the db.
	 * If it does exist and the passed tool session id exists in the db, that means the user already responded to
	 * the content and
	 * his answers must be displayed read-only
	 *
	 * if the user's tool session id AND user id exists in the tool tables go to learner's report.
	 */
	/*
	 * if the 'All Responses' has been clicked no more user entry is accepted, and isResponseFinalized() returns
	 * true
	 */
	Long currentToolSessionID = new Long(learningForm.getToolSessionID());

	//if Response is Finalized
	if (user.isResponseFinalized()) {
	    QaSession checkSession = user.getQaSession();

	    if (checkSession != null) {
		Long checkQaSessionId = checkSession.getQaSessionId();

		if (checkQaSessionId.toString().equals(currentToolSessionID.toString())) {

		    // the learner is in the same session and has already responsed to this content

		    generalLearnerFlowDTO.setLockWhenFinished(new Boolean(qaContent.isLockWhenFinished()).toString());
		    generalLearnerFlowDTO.setNoReeditAllowed(qaContent.isNoReeditAllowed());
		    /*
		     * the report should have all the users' entries OR the report should have only the current
		     * session's entries
		     */
		    generalLearnerFlowDTO.setRequestLearningReport(new Boolean(true).toString());

		    QaLearningController.refreshSummaryData(request, qaContent, qaSession, qaService, sessionMapId,
			    user, generalLearnerFlowDTO);

		    if (user.isLearnerFinished()) {
			generalLearnerFlowDTO.setRequestLearningReportViewOnly(new Boolean(true).toString());
			return "learning/RevisitedLearnerRep";
		    } else {
			generalLearnerFlowDTO.setRequestLearningReportViewOnly(new Boolean(false).toString());
			return "learning/LearnerRep";
		    }
		}
	    }
	}

	//**---- showing AnswersContent.jsp ----**
	LearningUtil.populateAnswers(sessionMap, qaContent, user, mapQuestions, generalLearnerFlowDTO, qaService);

	return "learning/AnswersContent";
    }

    /**
     * validates the learning mode parameters
     *
     * @param request
     * @param mapping
     * @return ActionForward
     */
    protected void validateParameters(HttpServletRequest request,
	    @ModelAttribute("qaLearningForm") QaLearningForm qaLearningForm) {
	/*
	 * process incoming tool session id and later derive toolContentId from it.
	 */
	String strToolSessionId = request.getParameter(AttributeNames.PARAM_TOOL_SESSION_ID);
	long toolSessionId = 0;
	if ((strToolSessionId == null) || (strToolSessionId.length() == 0)) {
	    MultiValueMap<String, String> errorMap = new LinkedMultiValueMap<>();
	    errorMap.add(Globals.ERROR_KEY, messageService.getMessage("error.toolSessionId.required"));
	    logger.error("error.toolSessionId.required");
	    request.setAttribute("errorMap", errorMap);
	    return;
	} else {
	    try {
		toolSessionId = new Long(strToolSessionId).longValue();
		qaLearningForm.setToolSessionID(new Long(toolSessionId).toString());
	    } catch (NumberFormatException e) {
		logger.error("add error.sessionId.numberFormatException to ActionMessages.");
		return;
	    }
	}

	/* mode can be learner, teacher or author */
	String mode = request.getParameter(MODE);
	if ((mode == null) || (mode.length() == 0)) {
	    logger.error("Mode is empty");
	    return;
	}
	if ((!mode.equals("learner")) && (!mode.equals("teacher")) && (!mode.equals("author"))) {
	    logger.error("Wrong mode");
	    return;
	}
	qaLearningForm.setMode(mode);
    }

    private QaQueUsr getSpecifiedUser(String toolSessionId, Integer userId) {
	QaQueUsr qaUser = qaService.getUserByIdAndSession(userId.longValue(), new Long(toolSessionId));
	if (qaUser == null) {
	    qaUser = qaService.createUser(new Long(toolSessionId), userId);
	}
	return qaUser;
    }

    /**
     * submits users responses
     *
     * @param mapping
     * @param form
     * @param request
     * @param response
     * @return
     * @throws IOException
     * @throws ServletException
     */
    @RequestMapping("/submitAnswersContent")
    public String submitAnswersContent(@ModelAttribute("qaLearningForm") QaLearningForm qaLearningForm,
	    HttpServletRequest request) throws IOException, ServletException {

	LearningUtil.saveFormRequestData(request, qaLearningForm);
	String toolSessionID = request.getParameter(AttributeNames.PARAM_TOOL_SESSION_ID);

	QaSession qaSession = qaService.getSessionById(new Long(toolSessionID).longValue());
	QaContent qaContent = qaSession.getQaContent();

	QaQueUsr qaQueUsr = getCurrentUser(toolSessionID);
	//prohibit users from submitting answers after response is finalized but Resubmit button is not pressed (e.g. using 2 browsers)
	if (qaQueUsr.isResponseFinalized()) {
	    String redirectURL = "redirect:/learning/learning.do";
	    redirectURL = WebUtil.appendParameterToURL(redirectURL, AttributeNames.PARAM_TOOL_SESSION_ID,
		    toolSessionID.toString());
	    redirectURL = WebUtil.appendParameterToURL(redirectURL, QaAppConstants.MODE, "learner");
	    return redirectURL;
	}

	GeneralLearnerFlowDTO generalLearnerFlowDTO = LearningUtil.buildGeneralLearnerFlowDTO(qaService, qaContent);

	String totalQuestionCount = generalLearnerFlowDTO.getTotalQuestionCount().toString();
	int intTotalQuestionCount = new Integer(totalQuestionCount).intValue();

	String questionListingMode = generalLearnerFlowDTO.getQuestionListingMode();

	Map<String, String> mapAnswers = new TreeMap<String, String>(new QaComparator());
	Map<String, String> mapAnswersPresentable = new TreeMap<String, String>(new QaComparator());

	String forwardName = QaAppConstants.INDIVIDUAL_LEARNER_RESULTS;

	String httpSessionID = qaLearningForm.getHttpSessionID();
	SessionMap<String, Object> sessionMap = (SessionMap<String, Object>) request.getSession()
		.getAttribute(httpSessionID);

	MultiValueMap<String, String> errorMap = new LinkedMultiValueMap<>();
	/* if the listing mode is QUESTION_LISTING_MODE_COMBINED populate the answers here */
	if (questionListingMode.equalsIgnoreCase(QaAppConstants.QUESTION_LISTING_MODE_COMBINED)) {

	    for (int questionIndex = QaAppConstants.INITIAL_QUESTION_COUNT
		    .intValue(); questionIndex <= intTotalQuestionCount; questionIndex++) {
		// TestHarness can not send "answerX" fields, so stick to the original, unfiltered field
		boolean isTestHarness = Boolean.valueOf(request.getParameter("testHarness"));
		String answerParamName = "answer" + questionIndex + (isTestHarness ? "__textarea" : "");
		String answer = request.getParameter(answerParamName);

		Integer questionIndexInteger = new Integer(questionIndex);
		mapAnswers.put(questionIndexInteger.toString(), answer);
		mapAnswersPresentable.put(questionIndexInteger.toString(), answer);

		//validate
		errorMap = validateQuestionAnswer(answer, questionIndexInteger, generalLearnerFlowDTO);

		// store
		if (errorMap.isEmpty()) {
		    qaService.updateResponseWithNewAnswer(answer, toolSessionID, new Long(questionIndex), false);
		}
	    }

	} else {
	    Object[] results = storeSequentialAnswer(qaLearningForm, request, generalLearnerFlowDTO, true);
	    mapAnswers = (Map<String, String>) results[0];
	    errorMap = (MultiValueMap<String, String>) results[1];

	    mapAnswersPresentable = (Map) sessionMap.get(QaAppConstants.MAP_ALL_RESULTS_KEY);
	    mapAnswersPresentable = QaLearningController.removeNewLinesMap(mapAnswersPresentable);
	}

	//finalize response so user won't need to edit his answers again, if coming back to the activity after leaving activity at this point
	if (errorMap.isEmpty()) {
	    qaQueUsr.setResponseFinalized(true);
	    qaService.updateUser(qaQueUsr);

	    //in case of errors - prompt learner to enter answers again
	} else {
	    request.setAttribute("errorMap", errorMap);
	    forwardName = QaAppConstants.LOAD_LEARNER;
	}

	generalLearnerFlowDTO.setMapAnswers(mapAnswers);
	generalLearnerFlowDTO.setMapAnswersPresentable(mapAnswersPresentable);

	/* mapAnswers will be used in the viewAllAnswers screen */
	if (sessionMap == null) {
	    sessionMap = new SessionMap<>();
	}

	sessionMap.put(QaAppConstants.MAP_ALL_RESULTS_KEY, mapAnswers);
	request.getSession().setAttribute(sessionMap.getSessionID(), sessionMap);
	qaLearningForm.setHttpSessionID(sessionMap.getSessionID());
	qaLearningForm.resetAll();
	generalLearnerFlowDTO.setHttpSessionID(sessionMap.getSessionID());

	boolean lockWhenFinished = qaContent.isLockWhenFinished();
	generalLearnerFlowDTO.setLockWhenFinished(new Boolean(lockWhenFinished).toString());
	generalLearnerFlowDTO.setNoReeditAllowed(qaContent.isNoReeditAllowed());
	generalLearnerFlowDTO.setReflection(new Boolean(qaContent.isReflect()).toString());

	request.setAttribute(QaAppConstants.GENERAL_LEARNER_FLOW_DTO, generalLearnerFlowDTO);

	// notify teachers on response submit
	if (errorMap.isEmpty() && qaContent.isNotifyTeachersOnResponseSubmit()) {
	    qaService.notifyTeachersOnResponseSubmit(new Long(toolSessionID));
	}
	request.setAttribute("learningForm", qaLearningForm);
	return "learning/AnswersContent";
    }

    @RequestMapping("/checkLeaderProgress")
    public String checkLeaderProgress(HttpServletRequest request, HttpServletResponse response) throws IOException {

	Long toolSessionId = WebUtil.readLongParam(request, AttributeNames.PARAM_TOOL_SESSION_ID);

	QaSession session = qaService.getSessionById(toolSessionId);
	QaQueUsr leader = session.getGroupLeader();

	boolean isLeaderResponseFinalized = leader.isResponseFinalized();

	ObjectNode ObjectNode = JsonNodeFactory.instance.objectNode();
	ObjectNode.put("isLeaderResponseFinalized", isLeaderResponseFinalized);
	response.setContentType("application/x-json;charset=utf-8");
	response.getWriter().print(ObjectNode);
	return null;
    }

    /**
     * auto saves responses
     *
     * @param mapping
     * @param form
     * @param request
     * @param response
     * @return
     * @throws IOException
     * @throws ServletException
     */
    @RequestMapping("/autoSaveAnswers")
    public String autoSaveAnswers(@ModelAttribute("qaLearningForm") QaLearningForm qaLearningForm,
	    HttpServletRequest request) throws IOException, ServletException {
	String toolSessionID = request.getParameter(AttributeNames.PARAM_TOOL_SESSION_ID);

	QaQueUsr qaQueUsr = getCurrentUser(toolSessionID);
	//prohibit users from autosaving answers after response is finalized but Resubmit button is not pressed (e.g. using 2 browsers)
	if (qaQueUsr.isResponseFinalized()) {
	    return null;
	}

	LearningUtil.saveFormRequestData(request, qaLearningForm);
	QaSession qaSession = qaService.getSessionById(new Long(toolSessionID).longValue());
	QaContent qaContent = qaSession.getQaContent();
	int intTotalQuestionCount = qaContent.getQaQueContents().size();

	if (!qaContent.isQuestionsSequenced()) {

	    for (int questionIndex = QaAppConstants.INITIAL_QUESTION_COUNT
		    .intValue(); questionIndex <= intTotalQuestionCount; questionIndex++) {
		String newAnswer = request.getParameter("answer" + questionIndex);
		qaService.updateResponseWithNewAnswer(newAnswer, toolSessionID, new Long(questionIndex), true);
	    }

	} else {
	    String currentQuestionIndex = qaLearningForm.getCurrentQuestionIndex();
	    String newAnswer = qaLearningForm.getAnswer();
	    QaQueContent currentQuestion = qaService.getQuestionByContentAndDisplayOrder(new Long(currentQuestionIndex),
		    qaContent.getUid());

	    boolean isRequiredQuestionMissed = currentQuestion.isRequired() && isEmpty(newAnswer);
	    if (!isRequiredQuestionMissed) {
		qaService.updateResponseWithNewAnswer(newAnswer, toolSessionID, new Long(currentQuestionIndex), true);
	    }
	}
	return null;
    }

    /**
     * enables retaking the activity
     *
     * @param mapping
     * @param form
     * @param request
     * @param response
     * @return
     * @throws IOException
     * @throws ServletException
     */
    @RequestMapping("/redoQuestions")
    public String redoQuestions(@ModelAttribute("qaLearningForm") QaLearningForm qaLearningForm,
	    HttpServletRequest request) throws IOException, ServletException {

	LearningUtil.saveFormRequestData(request, qaLearningForm);

	String toolSessionID = request.getParameter(AttributeNames.PARAM_TOOL_SESSION_ID);
	QaSession qaSession = qaService.getSessionById(new Long(toolSessionID).longValue());
	QaContent qaContent = qaSession.getQaContent();

	GeneralLearnerFlowDTO generalLearnerFlowDTO = LearningUtil.buildGeneralLearnerFlowDTO(qaService, qaContent);

	qaLearningForm.setCurrentQuestionIndex(new Integer(1).toString());

	String httpSessionID = qaLearningForm.getHttpSessionID();

	SessionMap<String, Object> sessionMap = (SessionMap<String, Object>) request.getSession()
		.getAttribute(httpSessionID);
	request.getSession().setAttribute(sessionMap.getSessionID(), sessionMap);
	qaLearningForm.setHttpSessionID(sessionMap.getSessionID());
	generalLearnerFlowDTO.setHttpSessionID(sessionMap.getSessionID());
	generalLearnerFlowDTO.setToolContentID(qaContent.getQaContentId().toString());

	// create mapQuestions
	Map<Integer, QaQuestionDTO> mapQuestions = generalLearnerFlowDTO.getMapQuestionContentLearner();
	generalLearnerFlowDTO.setMapQuestions(mapQuestions);
	generalLearnerFlowDTO.setTotalQuestionCount(new Integer(mapQuestions.size()));
	generalLearnerFlowDTO.setRemainingQuestionCount(new Integer(mapQuestions.size()).toString());
	qaLearningForm.setTotalQuestionCount(new Integer(mapQuestions.size()).toString());

	//in order to track whether redo button is pressed store this info
	QaQueUsr qaQueUsr = getCurrentUser(toolSessionID);
	qaQueUsr.setResponseFinalized(false);
	qaService.updateUser(qaQueUsr);

	// populate answers
	LearningUtil.populateAnswers(sessionMap, qaContent, qaQueUsr, mapQuestions, generalLearnerFlowDTO, qaService);

	request.setAttribute(QaAppConstants.GENERAL_LEARNER_FLOW_DTO, generalLearnerFlowDTO);
	qaLearningForm.resetAll();
	return "learning/AnswersContent";
    }

    /**
     * Stores all results and moves onto the next step. If view other users answers = true, then goes to the view all
     * answers screen, otherwise goes straight to the reflection screen (if any).
     *
     * @return Learner Report for a session
     * @throws IOException
     * @throws ServletException
     */
    @RequestMapping("/storeAllResults")
    public String storeAllResults(@ModelAttribute("qaLearningForm") QaLearningForm qaLearningForm,
	    HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

	LearningUtil.saveFormRequestData(request, qaLearningForm);

	String toolSessionID = request.getParameter(AttributeNames.PARAM_TOOL_SESSION_ID);
	String userID = request.getParameter("userID");
	QaQueUsr user = qaService.getUserByIdAndSession(new Long(userID), new Long(toolSessionID));
	QaSession qaSession = qaService.getSessionById(new Long(toolSessionID).longValue());
	QaContent qaContent = qaSession.getQaContent();

	// LearningUtil.storeResponses(mapAnswers, qaService, toolContentID, new Long(toolSessionID));

	qaLearningForm.resetUserActions();
	qaLearningForm.setSubmitAnswersContent(null);

	if (qaContent.isShowOtherAnswers()) {
	    GeneralLearnerFlowDTO generalLearnerFlowDTO = LearningUtil.buildGeneralLearnerFlowDTO(qaService, qaContent);
	    String httpSessionID = qaLearningForm.getHttpSessionID();
	    generalLearnerFlowDTO.setHttpSessionID(httpSessionID);

	    /** Set up the data for the view all answers screen */
	    QaLearningController.refreshSummaryData(request, qaContent, qaSession, qaService, httpSessionID, user,
		    generalLearnerFlowDTO);

	    generalLearnerFlowDTO.setRequestLearningReport(new Boolean(true).toString());
	    generalLearnerFlowDTO.setRequestLearningReportProgress(new Boolean(false).toString());

	    generalLearnerFlowDTO.setReflection(new Boolean(qaContent.isReflect()).toString());

	    qaLearningForm.resetAll();

	    boolean lockWhenFinished = qaContent.isLockWhenFinished();
	    generalLearnerFlowDTO.setLockWhenFinished(new Boolean(lockWhenFinished).toString());
	    generalLearnerFlowDTO.setNoReeditAllowed(qaContent.isNoReeditAllowed());

	    boolean useSelectLeaderToolOuput = qaContent.isUseSelectLeaderToolOuput();
	    generalLearnerFlowDTO.setUseSelectLeaderToolOuput(new Boolean(useSelectLeaderToolOuput).toString());

	    boolean allowRichEditor = qaContent.isAllowRichEditor();
	    generalLearnerFlowDTO.setAllowRichEditor(new Boolean(allowRichEditor).toString());
	    generalLearnerFlowDTO.setUserUid(user.getQueUsrId().toString());

	    boolean usernameVisible = qaContent.isUsernameVisible();
	    generalLearnerFlowDTO.setUserNameVisible(new Boolean(usernameVisible).toString());

	    NotebookEntry notebookEntry = qaService.getEntry(new Long(toolSessionID),
		    CoreNotebookConstants.NOTEBOOK_TOOL, QaAppConstants.MY_SIGNATURE, new Integer(userID));

	    if (notebookEntry != null) {
		// String notebookEntryPresentable=QaUtils.replaceNewLines(notebookEntry.getEntry());
		String notebookEntryPresentable = notebookEntry.getEntry();
		qaLearningForm.setEntryText(notebookEntryPresentable);
	    }

	    request.setAttribute(QaAppConstants.GENERAL_LEARNER_FLOW_DTO, generalLearnerFlowDTO);
	    return "learning/LearnerRep";

	} else if (qaContent.isReflect()) {
	    return forwardtoReflection(request, qaContent, toolSessionID, userID, qaLearningForm);

	} else {
	    return endLearning(qaLearningForm, request, response);
	}
    }

    /**
     * @param qaLearningForm
     * @param request
     * @return
     * @throws IOException
     * @throws ServletException
     */
    @RequestMapping("/refreshAllResults")
    public String refreshAllResults(@ModelAttribute("qaLearningForm") QaLearningForm qaLearningForm,
	    HttpServletRequest request) throws IOException, ServletException {

	LearningUtil.saveFormRequestData(request, qaLearningForm);

	String toolSessionID = request.getParameter(AttributeNames.PARAM_TOOL_SESSION_ID);
	qaLearningForm.setToolSessionID(toolSessionID);

	String userID = request.getParameter("userID");
	QaQueUsr user = qaService.getUserByIdAndSession(new Long(userID), new Long(toolSessionID));

	QaSession qaSession = qaService.getSessionById(new Long(toolSessionID).longValue());

	QaContent qaContent = qaSession.getQaContent();

	GeneralLearnerFlowDTO generalLearnerFlowDTO = LearningUtil.buildGeneralLearnerFlowDTO(qaService, qaContent);

	String httpSessionID = qaLearningForm.getHttpSessionID();
	qaLearningForm.setHttpSessionID(httpSessionID);
	generalLearnerFlowDTO.setHttpSessionID(httpSessionID);
	SessionMap<String, Object> sessionMap = (SessionMap<String, Object>) request.getSession()
		.getAttribute(httpSessionID);

	/* recreate the users and responses */
	qaLearningForm.resetUserActions();
	qaLearningForm.setSubmitAnswersContent(null);

	QaLearningController.refreshSummaryData(request, qaContent, qaSession, qaService, httpSessionID, user,
		generalLearnerFlowDTO);

	generalLearnerFlowDTO.setRequestLearningReport(new Boolean(true).toString());
	generalLearnerFlowDTO.setRequestLearningReportProgress(new Boolean(false).toString());

	generalLearnerFlowDTO.setReflection(new Boolean(qaContent.isReflect()).toString());
	// generalLearnerFlowDTO.setNotebookEntriesVisible(new Boolean(false).toString());

	qaLearningForm.resetAll();

	boolean lockWhenFinished;
	boolean noReeditAllowed;
	if (sessionMap.get("noRefresh") != null && (boolean) sessionMap.get("noRefresh")) {
	    lockWhenFinished = true;
	    noReeditAllowed = true;
	} else {
	    lockWhenFinished = qaContent.isLockWhenFinished();
	    noReeditAllowed = qaContent.isNoReeditAllowed();
	}
	generalLearnerFlowDTO.setLockWhenFinished(new Boolean(lockWhenFinished).toString());
	generalLearnerFlowDTO.setNoReeditAllowed(noReeditAllowed);

	boolean allowRichEditor = qaContent.isAllowRichEditor();
	generalLearnerFlowDTO.setAllowRichEditor(new Boolean(allowRichEditor).toString());

	boolean useSelectLeaderToolOuput = qaContent.isUseSelectLeaderToolOuput();
	generalLearnerFlowDTO.setUseSelectLeaderToolOuput(new Boolean(useSelectLeaderToolOuput).toString());

	QaQueUsr qaQueUsr = getCurrentUser(toolSessionID);
	generalLearnerFlowDTO.setUserUid(qaQueUsr.getQueUsrId().toString());

	boolean usernameVisible = qaContent.isUsernameVisible();
	generalLearnerFlowDTO.setUserNameVisible(new Boolean(usernameVisible).toString());

	request.setAttribute(QaAppConstants.GENERAL_LEARNER_FLOW_DTO, generalLearnerFlowDTO);

	return "learning/LearnerRep";
    }

    /**
     * moves to the next question and modifies the map ActionForward
     *
     * @param qaLearningForm
     * @param request
     * @return
     * @throws IOException
     * @throws ServletException
     * @throws ToolException
     */
    @RequestMapping("/getNextQuestion")
    public String getNextQuestion(@ModelAttribute("qaLearningForm") QaLearningForm qaLearningForm,
	    HttpServletRequest request) throws IOException, ServletException, ToolException {

	LearningUtil.saveFormRequestData(request, qaLearningForm);

	String toolSessionID = request.getParameter(AttributeNames.PARAM_TOOL_SESSION_ID);
	qaLearningForm.setToolSessionID(toolSessionID);
	String httpSessionID = qaLearningForm.getHttpSessionID();
	qaLearningForm.setHttpSessionID(httpSessionID);

	QaSession qaSession = qaService.getSessionById(new Long(toolSessionID).longValue());
	QaContent qaContent = qaSession.getQaContent();

	QaQueUsr qaQueUsr = getCurrentUser(toolSessionID);
	//prohibit users from submitting answers after response is finalized but Resubmit button is not pressed (e.g. using 2 browsers)
	if (qaQueUsr.isResponseFinalized()) {
	    String redirectURL = "redirect:learning/LearnerRep.jsp";
	    redirectURL = WebUtil.appendParameterToURL(redirectURL, AttributeNames.PARAM_TOOL_SESSION_ID,
		    toolSessionID.toString());
	    redirectURL = WebUtil.appendParameterToURL(redirectURL, QaAppConstants.MODE, "learner");
	    return redirectURL;
	}

	GeneralLearnerFlowDTO generalLearnerFlowDTO = LearningUtil.buildGeneralLearnerFlowDTO(qaService, qaContent);

	storeSequentialAnswer(qaLearningForm, request, generalLearnerFlowDTO, true);

	qaLearningForm.resetAll();
	request.setAttribute("learningForm",qaLearningForm);
	return "learning/AnswersContent";
    }

    /**
     * Get the answer from the form and copy into DTO. Set up the next question. If the current question is required and
     * the answer is blank, then just persist the error and don't change questions.
     *
     * @param form
     * @param request
     * @param generalLearnerFlowDTO
     * @param getNextQuestion
     * @return
     */
    private Object[] storeSequentialAnswer(QaLearningForm qaLearningForm,
	    HttpServletRequest request, GeneralLearnerFlowDTO generalLearnerFlowDTO, boolean getNextQuestion) {
	String httpSessionID = qaLearningForm.getHttpSessionID();
	SessionMap<String, Object> sessionMap = (SessionMap<String, Object>) request.getSession()
		.getAttribute(httpSessionID);

	String currentQuestionIndex = qaLearningForm.getCurrentQuestionIndex();

	Map<String, String> mapAnswers = (Map<String, String>) sessionMap.get(QaAppConstants.MAP_ALL_RESULTS_KEY);
	if (mapAnswers == null) {
	    mapAnswers = new TreeMap<String, String>(new QaComparator());
	}

	String newAnswer = qaLearningForm.getAnswer();
	Map<String, String> mapSequentialAnswers = (Map<String, String>) sessionMap
		.get(QaAppConstants.MAP_SEQUENTIAL_ANSWERS_KEY);
	if (mapSequentialAnswers.size() >= new Integer(currentQuestionIndex).intValue()) {
	    mapSequentialAnswers.remove(new Long(currentQuestionIndex).toString());
	}
	mapSequentialAnswers.put(new Long(currentQuestionIndex).toString(), newAnswer);
	mapAnswers.put(currentQuestionIndex, newAnswer);

	int nextQuestionOffset = getNextQuestion ? 1 : -1;

	// validation only if trying to go to the next question
	MultiValueMap<String, String> errorMap = new LinkedMultiValueMap<>();
	if (getNextQuestion) {
	    errorMap = validateQuestionAnswer(newAnswer, new Integer(currentQuestionIndex), generalLearnerFlowDTO);
	}

	// store
	if (errorMap.isEmpty()) {
	    qaService.updateResponseWithNewAnswer(newAnswer, qaLearningForm.getToolSessionID(),
		    new Long(currentQuestionIndex), false);
	} else {
	    request.setAttribute("errorMap", errorMap);
	    nextQuestionOffset = 0;
	}

	int intCurrentQuestionIndex = new Integer(currentQuestionIndex).intValue() + nextQuestionOffset;
	String currentAnswer = "";
	if (mapAnswers.size() >= intCurrentQuestionIndex) {
	    currentAnswer = mapAnswers.get(new Long(intCurrentQuestionIndex).toString());
	}
	generalLearnerFlowDTO.setCurrentAnswer(currentAnswer);

	// currentQuestionIndex will be:
	generalLearnerFlowDTO.setCurrentQuestionIndex(new Integer(intCurrentQuestionIndex));

	String totalQuestionCount = qaLearningForm.getTotalQuestionCount();

	int remainingQuestionCount = new Long(totalQuestionCount).intValue()
		- new Integer(currentQuestionIndex).intValue() + 1;
	String userFeedback = "";
	if (remainingQuestionCount != 0) {
	    userFeedback = "Remaining question count: " + remainingQuestionCount;
	} else {
	    userFeedback = "End of the questions.";
	}
	generalLearnerFlowDTO.setUserFeedback(userFeedback);
	generalLearnerFlowDTO.setRemainingQuestionCount("" + remainingQuestionCount);

	qaLearningForm.resetUserActions(); /* resets all except submitAnswersContent */

	sessionMap.put(QaAppConstants.MAP_ALL_RESULTS_KEY, mapAnswers);
	sessionMap.put(QaAppConstants.MAP_SEQUENTIAL_ANSWERS_KEY, mapSequentialAnswers);
	request.getSession().setAttribute(sessionMap.getSessionID(), sessionMap);
	qaLearningForm.setHttpSessionID(sessionMap.getSessionID());
	generalLearnerFlowDTO.setHttpSessionID(sessionMap.getSessionID());

	request.setAttribute(QaAppConstants.GENERAL_LEARNER_FLOW_DTO, generalLearnerFlowDTO);

	return new Object[] { mapSequentialAnswers, errorMap };
    }

    private MultiValueMap<String, String> validateQuestionAnswer(String newAnswer, Integer questionIndex,
	    GeneralLearnerFlowDTO generalLearnerFlowDTO) {
	MultiValueMap<String, String> errorMap = new LinkedMultiValueMap<>();

	Map<Integer, QaQuestionDTO> questionMap = generalLearnerFlowDTO.getMapQuestionContentLearner();
	QaQuestionDTO dto = questionMap.get(questionIndex);

	// if so, check if the answer is blank and generate an error if it is blank.
	boolean isRequiredQuestionMissed = dto.isRequired() && isEmpty(newAnswer);
	if (isRequiredQuestionMissed) {
	    errorMap.add(Globals.ERROR_KEY,
		    messageService.getMessage("error.required", new Object[] { questionIndex }));
	}

	boolean isMinWordsLimitReached = ValidationUtil.isMinWordsLimitReached(newAnswer, dto.getMinWordsLimit(),
		Boolean.parseBoolean(generalLearnerFlowDTO.getAllowRichEditor()));
	if (!isMinWordsLimitReached) {
	    errorMap.add(Globals.ERROR_KEY, messageService.getMessage("label.minimum.number.words",
		    ": " + new Object[] { dto.getMinWordsLimit() }));
	}

	return errorMap;
    }

    /**
     * Is this string empty? Need to strip out all HTML tags first otherwise an empty DIV might look like a valid answer
     * Smileys and math functions only put in an img tag so explicitly look for that.
     */
    private boolean isEmpty(String answer) {
	if ((answer != null) && ((answer.indexOf("<img") > -1) || (answer.indexOf("<IMG") > -1))) {
	    return false;
	} else {
	    return StringUtils.isBlank(WebUtil.removeHTMLtags(answer));
	}
    }

    /**
     * moves to the previous question and modifies the map ActionForward
     *
     * @param qaLearningForm
     * @param request
     * @return
     * @throws IOException
     * @throws ServletException
     * @throws ToolException
     */
    @RequestMapping("/getPreviousQuestion")
    public String getPreviousQuestion(@ModelAttribute("qaLearningForm") QaLearningForm qaLearningForm,
	    HttpServletRequest request) throws IOException, ServletException, ToolException {

	LearningUtil.saveFormRequestData(request, qaLearningForm);

	String httpSessionID = qaLearningForm.getHttpSessionID();
	qaLearningForm.setHttpSessionID(httpSessionID);
	String toolSessionID = request.getParameter(AttributeNames.PARAM_TOOL_SESSION_ID);
	qaLearningForm.setToolSessionID(toolSessionID);
	QaSession qaSession = qaService.getSessionById(new Long(toolSessionID).longValue());
	QaContent qaContent = qaSession.getQaContent();

	QaQueUsr qaQueUsr = getCurrentUser(toolSessionID);
	//prohibit users from submitting answers after response is finalized but Resubmit button is not pressed (e.g. using 2 browsers)
	if (qaQueUsr.isResponseFinalized()) {
	    String redirectURL = "redirect:learning/learningIndex.jsp";
	    redirectURL = WebUtil.appendParameterToURL(redirectURL, AttributeNames.PARAM_TOOL_SESSION_ID,
		    toolSessionID.toString());
	    redirectURL = WebUtil.appendParameterToURL(redirectURL, QaAppConstants.MODE, "learner");
	    return redirectURL;
	}

	GeneralLearnerFlowDTO generalLearnerFlowDTO = LearningUtil.buildGeneralLearnerFlowDTO(qaService, qaContent);

	storeSequentialAnswer(qaLearningForm, request, generalLearnerFlowDTO, false);

	qaLearningForm.resetAll();
	return "learning/AnswersContent";
    }

    /**
     * finishes the user's tool activity
     *
     * @param request
     * @param qaService
     * @param response
     * @throws IOException
     * @throws ToolException
     */
    @RequestMapping("/endLearning")
    public String endLearning(@ModelAttribute("qaLearningForm") QaLearningForm qaLearningForm,
	    HttpServletRequest request, HttpServletResponse response)
	    throws IOException, ServletException, ToolException {

	LearningUtil.saveFormRequestData(request, qaLearningForm);

	String toolSessionID = request.getParameter(AttributeNames.PARAM_TOOL_SESSION_ID);
	qaLearningForm.setToolSessionID(toolSessionID);

	String userID = request.getParameter("userID");
	qaLearningForm.setUserID(userID);

	QaSession qaSession = qaService.getSessionById(new Long(toolSessionID).longValue());

	QaQueUsr qaQueUsr = qaService.getUserByIdAndSession(new Long(userID), qaSession.getQaSessionId());
	qaQueUsr.setLearnerFinished(true);
	qaService.updateUser(qaQueUsr);

	/*
	 * The learner is done with the tool session. The tool needs to clean-up.
	 */
	HttpSession ss = SessionManager.getSession();
	/* get back login user DTO */
	UserDTO user = (UserDTO) ss.getAttribute(AttributeNames.USER);

	qaSession.setSession_end_date(new Date(System.currentTimeMillis()));
	qaSession.setSession_status(QaAppConstants.COMPLETED);
	qaService.updateSession(qaSession);

	String httpSessionID = qaLearningForm.getHttpSessionID();
	// request.getSession().removeAttribute(httpSessionID);
	qaLearningForm.setHttpSessionID(httpSessionID);

	qaLearningForm.resetAll();

	String nextActivityUrl = qaService.leaveToolSession(new Long(toolSessionID),
		new Long(user.getUserID().longValue()));
	response.sendRedirect(nextActivityUrl);

	return null;
    }

    /**
     *
     * @param reflectionForm
     * @param request
     * @param response
     * @return
     * @throws IOException
     * @throws ServletException
     * @throws ToolException
     */
    @RequestMapping("/submitReflection")
    public String submitReflection(QaLearningForm reflectionForm, HttpServletRequest request,
	    HttpServletResponse response) throws IOException, ServletException, ToolException {

	LearningUtil.saveFormRequestData(request, reflectionForm);

	String httpSessionID = reflectionForm.getHttpSessionID();

	reflectionForm.setHttpSessionID(httpSessionID);

	String toolSessionIDString = request.getParameter(AttributeNames.PARAM_TOOL_SESSION_ID);
	reflectionForm.setToolSessionID(toolSessionIDString);
	Long toolSessionID = new Long(toolSessionIDString);

	String userIDString = request.getParameter("userID");
	reflectionForm.setUserID(userIDString);
	Integer userID = new Integer(userIDString);

	String reflectionEntry = request.getParameter(QaAppConstants.ENTRY_TEXT);

	// check for existing notebook entry
	NotebookEntry entry = qaService.getEntry(toolSessionID, CoreNotebookConstants.NOTEBOOK_TOOL, MY_SIGNATURE,
		userID);

	if (entry == null) {
	    // create new entry
	    qaService.createNotebookEntry(toolSessionID, CoreNotebookConstants.NOTEBOOK_TOOL,
		    QaAppConstants.MY_SIGNATURE, userID, reflectionEntry);

	} else {
	    // update existing entry
	    entry.setEntry(reflectionEntry);
	    entry.setLastModified(new Date());
	    qaService.updateEntry(entry);
	}

	reflectionForm.resetUserActions(); /* resets all except submitAnswersContent */
	return endLearning(reflectionForm, request, response);
    }

    /**
     *
     * @param reflectionForm
     * @param request
     * @return
     * @throws IOException
     * @throws ServletException
     * @throws ToolException
     */
    @RequestMapping("/forwardtoReflection")
    public String forwardtoReflection(QaLearningForm reflectionForm, HttpServletRequest request)
	    throws IOException, ServletException, ToolException {

	String httpSessionID = reflectionForm.getHttpSessionID();

	reflectionForm.setHttpSessionID(httpSessionID);

	String toolSessionID = request.getParameter(AttributeNames.PARAM_TOOL_SESSION_ID);

	QaSession qaSession = qaService.getSessionById(new Long(toolSessionID).longValue());

	QaContent qaContent = qaSession.getQaContent();

	String userID = request.getParameter("userID");
	reflectionForm.setUserID(userID);

	return forwardtoReflection(request, qaContent, toolSessionID, userID, reflectionForm);
    }

    @RequestMapping("/forwardtoReflection")
    private String forwardtoReflection(HttpServletRequest request, QaContent qaContent, String toolSessionID,
	    String userID, QaLearningForm reflectionForm) {

	GeneralLearnerFlowDTO generalLearnerFlowDTO = new GeneralLearnerFlowDTO();
	generalLearnerFlowDTO.setActivityTitle(qaContent.getTitle());
	String reflectionSubject = qaContent.getReflectionSubject();
	// reflectionSubject = QaUtils.replaceNewLines(reflectionSubject);
	generalLearnerFlowDTO.setReflectionSubject(reflectionSubject);

	// attempt getting notebookEntry
	NotebookEntry notebookEntry = qaService.getEntry(new Long(toolSessionID), CoreNotebookConstants.NOTEBOOK_TOOL,
		QaAppConstants.MY_SIGNATURE, new Integer(userID));

	if (notebookEntry != null) {
	    // String notebookEntryPresentable=QaUtils.replaceNewLines(notebookEntry.getEntry());
	    String notebookEntryPresentable = notebookEntry.getEntry();
	    generalLearnerFlowDTO.setNotebookEntry(notebookEntryPresentable);
	    reflectionForm.setEntryText(notebookEntryPresentable);
	}

	request.setAttribute(QaAppConstants.GENERAL_LEARNER_FLOW_DTO, generalLearnerFlowDTO);
	reflectionForm.resetUserActions(); /* resets all except submitAnswersContent */

	reflectionForm.resetAll();
	return "learning/Notebook";
    }

    /**
     * populates data for summary screen, view all results screen.
     *
     * User id is needed if isUserNamesVisible is false && learnerRequest is true, as it is required to work out if the
     * data being analysed is the current user.
     */
    public static void refreshSummaryData(HttpServletRequest request, QaContent qaContent, QaSession qaSession,
	    IQaService qaService, String httpSessionID, QaQueUsr user, GeneralLearnerFlowDTO generalLearnerFlowDTO) {

	SessionMap<String, Object> sessionMap = (SessionMap<String, Object>) request.getSession()
		.getAttribute(httpSessionID);
	Long userId = user.getQueUsrId();
	Set<QaQueContent> questions = qaContent.getQaQueContents();
	generalLearnerFlowDTO.setQuestions(questions);
	generalLearnerFlowDTO.setUserNameVisible(new Boolean(qaContent.isUsernameVisible()).toString());

	// potentially empty list if the user starts the lesson after the time restriction has expired.
	List<QaUsrResp> userResponses = qaService.getResponsesByUserUid(user.getUid());

	//handle rating criterias
	int commentsMinWordsLimit = 0;
	boolean isCommentsEnabled = false;
	int countRatedQuestions = 0;
	if (qaContent.isAllowRateAnswers()) {

	    if (userResponses.isEmpty()) {
		Set<LearnerItemRatingCriteria> criterias = qaContent.getRatingCriterias();
		for (LearnerItemRatingCriteria criteria : criterias) {
		    if (criteria.isCommentRating()) {
			isCommentsEnabled = true;
			break;
		    }
		}

	    } else {
		// create itemIds list
		List<Long> itemIds = new LinkedList<>();
		for (QaUsrResp responseIter : userResponses) {
		    itemIds.add(responseIter.getResponseId());
		}

		List<ItemRatingDTO> itemRatingDtos = qaService.getRatingCriteriaDtos(qaContent.getQaContentId(),
			qaSession.getQaSessionId(), itemIds, true, userId);
		sessionMap.put(AttributeNames.ATTR_ITEM_RATING_DTOS, itemRatingDtos);

		if (itemRatingDtos.size() > 0) {
		    commentsMinWordsLimit = itemRatingDtos.get(0).getCommentsMinWordsLimit();
		    isCommentsEnabled = itemRatingDtos.get(0).isCommentsEnabled();
		}

		//map itemRatingDto to corresponding response
		for (QaUsrResp response : userResponses) {

		    //find corresponding itemRatingDto
		    ItemRatingDTO itemRatingDto = null;
		    for (ItemRatingDTO itemRatingDtoIter : itemRatingDtos) {
			if (itemRatingDtoIter.getItemId().equals(response.getResponseId())) {
			    itemRatingDto = itemRatingDtoIter;
			    break;
			}
		    }

		    response.setItemRatingDto(itemRatingDto);
		}

		// store how many items are rated
		countRatedQuestions = qaService.getCountItemsRatedByUser(qaContent.getQaContentId(), userId.intValue());
	    }
	}

	request.setAttribute(TOOL_SESSION_ID, qaSession.getQaSessionId());

	sessionMap.put("commentsMinWordsLimit", commentsMinWordsLimit);
	sessionMap.put("isCommentsEnabled", isCommentsEnabled);
	sessionMap.put(AttributeNames.ATTR_COUNT_RATED_ITEMS, countRatedQuestions);

	generalLearnerFlowDTO.setUserResponses(userResponses);
	generalLearnerFlowDTO.setRequestLearningReportProgress(new Boolean(true).toString());
    }

    /**
     * Refreshes user list.
     */
    @RequestMapping("/getResponses")
    public String getResponses(HttpServletRequest request, HttpServletResponse res)
	    throws IOException, ServletException {

	// teacher timezone
	HttpSession ss = SessionManager.getSession();
	UserDTO userDto = (UserDTO) ss.getAttribute(AttributeNames.USER);
	TimeZone userTimeZone = userDto.getTimeZone();

	boolean isAllowRateAnswers = WebUtil.readBooleanParam(request, "isAllowRateAnswers");
	boolean isAllowRichEditor = WebUtil.readBooleanParam(request, "isAllowRichEditor");
	boolean isOnlyLeadersIncluded = WebUtil.readBooleanParam(request, "isOnlyLeadersIncluded", false);
	Long qaContentId = WebUtil.readLongParam(request, "qaContentId");

	Long questionUid = WebUtil.readLongParam(request, "questionUid");
	Long qaSessionId = WebUtil.readLongParam(request, "qaSessionId");

	//in case of monitoring we show all results. in case of learning - don't show results from the current user
	boolean isMonitoring = WebUtil.readBooleanParam(request, "isMonitoring", false);
	Long userId = isMonitoring ? -1 : WebUtil.readLongParam(request, "userId");

	//paging parameters of tablesorter
	int size = WebUtil.readIntParam(request, "size");
	int page = WebUtil.readIntParam(request, "page");
	Integer sortByCol1 = WebUtil.readIntParam(request, "column[0]", true);
	Integer sortByCol2 = WebUtil.readIntParam(request, "column[1]", true);
	String searchString = request.getParameter("fcol[0]");

	int sorting = QaAppConstants.SORT_BY_NO;
	if (sortByCol1 != null) {
	    if (isMonitoring) {
		sorting = sortByCol1.equals(0) ? QaAppConstants.SORT_BY_USERNAME_ASC
			: QaAppConstants.SORT_BY_USERNAME_DESC;
	    } else {
		sorting = sortByCol1.equals(0) ? QaAppConstants.SORT_BY_ANSWER_ASC : QaAppConstants.SORT_BY_ANSWER_DESC;
	    }

	} else if (sortByCol2 != null) {
	    sorting = sortByCol2.equals(0) ? QaAppConstants.SORT_BY_RATING_ASC : QaAppConstants.SORT_BY_RATING_DESC;

	} else if (!isMonitoring) {
	    // Is it learner and comment only? If so sort by number of comments.
	    QaSession qaSession = qaService.getSessionById(qaSessionId);
	    Set<LearnerItemRatingCriteria> criterias = qaSession.getQaContent().getRatingCriterias();
	    boolean hasComment = false;
	    boolean hasRating = false;
	    for (LearnerItemRatingCriteria criteria : criterias) {
		if (criteria.isCommentRating()) {
		    hasComment = true;
		} else {
		    hasRating = true;
		}
	    }
	    if (hasComment && !hasRating) {
		sorting = QaAppConstants.SORT_BY_COMMENT_COUNT;
	    }
	}

	List<QaUsrResp> responses = qaService.getResponsesForTablesorter(qaContentId, qaSessionId, questionUid, userId,
		isOnlyLeadersIncluded, page, size, sorting, searchString);

	ObjectNode responcedata = JsonNodeFactory.instance.objectNode();
	ArrayNode rows = JsonNodeFactory.instance.arrayNode();

	responcedata.put("total_rows", qaService.getCountResponsesBySessionAndQuestion(qaSessionId, questionUid, userId,
		isOnlyLeadersIncluded, searchString));

	// handle rating criterias - even though we may have searched on ratings earlier we can't use the average ratings
	// calculated as they may have been averages over more than one criteria.
	List<ItemRatingDTO> itemRatingDtos = null;
	if (isAllowRateAnswers && !responses.isEmpty()) {
	    //create itemIds list
	    List<Long> itemIds = new LinkedList<>();
	    for (QaUsrResp response : responses) {
		itemIds.add(response.getResponseId());
	    }

	    //all comments required only for monitoring
	    boolean isCommentsByOtherUsersRequired = isMonitoring;
	    itemRatingDtos = qaService.getRatingCriteriaDtos(qaContentId, qaSessionId, itemIds,
		    isCommentsByOtherUsersRequired, userId);

	    // store how many items are rated
	    int countRatedQuestions = qaService.getCountItemsRatedByUser(qaContentId, userId.intValue());
	    responcedata.put(AttributeNames.ATTR_COUNT_RATED_ITEMS, countRatedQuestions);
	}

	for (QaUsrResp response : responses) {
	    QaQueUsr user = response.getQaQueUser();

	    /*
	     * LDEV-3891: This code has been commented out, as the escapeCsv puts double quotes in the string, which
	     * goes through to the
	     * client and wrecks img src entries. It appears the browser cannot process the string with all the double
	     * quotes.
	     * This is the second time it is being fixed - the escapeCsv was removed in LDEV-3448 and then added back in
	     * when Peer Review was added (LDEV-3480). If escapeCsv needs to be added in again, make sure it does not
	     * break
	     * learner added images being seen in monitoring.
	     * //remove leading and trailing quotes
	     * String answer = StringEscapeUtils.escapeCsv(response.getAnswer());
	     * if (isAllowRichEditor && answer.startsWith("\"") && answer.length() >= 3) {
	     * answer = answer.substring(1, answer.length() - 1);
	     * }
	     */

	    ObjectNode responseRow = JsonNodeFactory.instance.objectNode();
	    responseRow.put("responseUid", response.getResponseId().toString());
	    responseRow.put("answer", response.getAnswer());
	    responseRow.put("userName", StringEscapeUtils.escapeCsv(user.getFullname()));
	    responseRow.put("visible", new Boolean(response.isVisible()).toString());
	    responseRow.put("userID", user.getQueUsrId());
	    responseRow.put("portraitId", response.getPortraitId());

	    // format attemptTime - got straight from server time to other timezones in formatter
	    // as trying to convert dates runs into tz issues - any Date object created is still
	    // in the server time zone.
	    Date attemptTime = response.getAttemptTime();
	    responseRow.put("attemptTime", DateUtil.convertToStringForJSON(attemptTime, request.getLocale()));
	    responseRow.put("timeAgo", DateUtil.convertToStringForTimeagoJSON(attemptTime));

	    if (isAllowRateAnswers) {

		//find corresponding itemRatingDto
		ItemRatingDTO itemRatingDto = null;
		for (ItemRatingDTO itemRatingDtoIter : itemRatingDtos) {
		    if (response.getResponseId().equals(itemRatingDtoIter.getItemId())) {
			itemRatingDto = itemRatingDtoIter;
			break;
		    }
		}

		boolean isItemAuthoredByUser = response.getQaQueUser().getQueUsrId().equals(userId);
		responseRow.put("isItemAuthoredByUser", isItemAuthoredByUser);

		ArrayNode criteriasRows = JsonNodeFactory.instance.arrayNode();
		for (ItemRatingCriteriaDTO criteriaDto : itemRatingDto.getCriteriaDtos()) {
		    ObjectNode criteriasRow = JsonNodeFactory.instance.objectNode();
		    criteriasRow.put("ratingCriteriaId", criteriaDto.getRatingCriteria().getRatingCriteriaId());
		    criteriasRow.put("title", criteriaDto.getRatingCriteria().getTitle());
		    criteriasRow.put("averageRating", criteriaDto.getAverageRating());
		    criteriasRow.put("numberOfVotes", criteriaDto.getNumberOfVotes());
		    criteriasRow.put("userRating", criteriaDto.getUserRating());

		    criteriasRows.add(criteriasRow);
		}
		responseRow.set("criteriaDtos", criteriasRows);

		//handle comments
		responseRow.put("commentsCriteriaId", itemRatingDto.getCommentsCriteriaId());
		String commentPostedByUser = itemRatingDto.getCommentPostedByUser() == null ? ""
			: itemRatingDto.getCommentPostedByUser().getComment();
		responseRow.put("commentPostedByUser", commentPostedByUser);
		if (itemRatingDto.getCommentDtos() != null) {

		    ArrayNode comments = JsonNodeFactory.instance.arrayNode();
		    for (RatingCommentDTO commentDto : itemRatingDto.getCommentDtos()) {
			ObjectNode comment = JsonNodeFactory.instance.objectNode();
			comment.put("comment", StringEscapeUtils.escapeCsv(commentDto.getComment()));

			if (isMonitoring) {
			    // format attemptTime
			    Date postedDate = commentDto.getPostedDate();
			    postedDate = DateUtil.convertToTimeZoneFromDefault(userTimeZone, postedDate);
			    comment.put("postedDate", DateUtil.convertToStringForJSON(postedDate, request.getLocale()));

			    comment.put("userFullName", StringEscapeUtils.escapeCsv(commentDto.getUserFullName()));
			}

			comments.add(comment);
		    }
		    responseRow.set("comments", comments);
		}
	    }

	    rows.add(responseRow);
	}
	responcedata.set("rows", rows);

	res.setContentType("application/json;charset=utf-8");
	res.getWriter().print(new String(responcedata.toString()));
	return null;
    }

    private static Map removeNewLinesMap(Map map) {
	Map newMap = new TreeMap(new QaStringComparator());

	Iterator itMap = map.entrySet().iterator();
	while (itMap.hasNext()) {
	    Map.Entry pairs = (Map.Entry) itMap.next();
	    String newText = "";
	    if (pairs.getValue().toString() != null) {
		newText = pairs.getValue().toString().replaceAll("\n", "<br>");
	    }
	    newMap.put(pairs.getKey(), newText);
	}
	return newMap;
    }

    private QaQueUsr getCurrentUser(String toolSessionId) {
	// get back login user DTO
	HttpSession ss = SessionManager.getSession();
	UserDTO toolUser = (UserDTO) ss.getAttribute(AttributeNames.USER);
	Integer userId = toolUser.getUserID();

	QaQueUsr qaUser = qaService.getUserByIdAndSession(userId.longValue(), new Long(toolSessionId));
	if (qaUser == null) {
	    qaUser = qaService.createUser(new Long(toolSessionId), userId);
	}

	return qaUser;
    }

}

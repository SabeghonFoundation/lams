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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 * USA
 *
 * http://www.gnu.org/licenses/gpl.txt
 * ****************************************************************
 */ 
 
/* $Id$ */ 
package org.lamsfoundation.lams.webservice.xml; 

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.lamsfoundation.lams.integration.ExtServerOrgMap;
import org.lamsfoundation.lams.integration.security.AuthenticationException;
import org.lamsfoundation.lams.integration.security.Authenticator;
import org.lamsfoundation.lams.integration.service.IntegrationService;
import org.lamsfoundation.lams.learningdesign.LearningDesign;
import org.lamsfoundation.lams.lesson.Lesson;
import org.lamsfoundation.lams.lesson.service.ILessonService;
import org.lamsfoundation.lams.monitoring.service.IMonitoringService;
import org.lamsfoundation.lams.usermanagement.Organisation;
import org.lamsfoundation.lams.usermanagement.Role;
import org.lamsfoundation.lams.usermanagement.User;
import org.lamsfoundation.lams.usermanagement.UserOrganisation;
import org.lamsfoundation.lams.usermanagement.dto.UserDTO;
import org.lamsfoundation.lams.usermanagement.service.IUserManagementService;
import org.lamsfoundation.lams.util.CentralConstants;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
 
/**
 * @author jliew
 *
 * @web:servlet name="LessonJoinServlet"
 * @web:servlet-mapping url-pattern="/services/xml/LessonJoin/*"
 */
public class LessonJoinServlet extends HttpServlet {
	
	private static Logger log = Logger.getLogger(LessonJoinServlet.class);
	private static ILessonService lessonService = null;
	private static IUserManagementService userService = null;
	private static IMonitoringService monitoringService = null;
	private static IntegrationService integrationService = null;
	
	public void init() throws ServletException {
		lessonService = (ILessonService) WebApplicationContextUtils
			.getRequiredWebApplicationContext(getServletContext()).getBean(
				"lessonService");
		
		userService = (IUserManagementService) WebApplicationContextUtils
			.getRequiredWebApplicationContext(getServletContext()).getBean(
				"userManagementService");
		
		monitoringService = (IMonitoringService) WebApplicationContextUtils
			.getRequiredWebApplicationContext(getServletContext()).getBean(
				"monitoringService");
		
		integrationService = (IntegrationService) WebApplicationContextUtils
			.getRequiredWebApplicationContext(getServletContext()).getBean(
				"integrationService");
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) 
		throws ServletException, IOException {
		
		PrintWriter out = response.getWriter();
		response.setContentType("text/xml");
		response.setCharacterEncoding("UTF8");
		
		String serverId = request.getParameter(CentralConstants.PARAM_SERVER_ID);
		String datetime = request.getParameter(CentralConstants.PARAM_DATE_TIME);
		String hashValue = request.getParameter(CentralConstants.PARAM_HASH_VALUE);
		String username = request.getParameter(CentralConstants.PARAM_USERNAME);
		String orgId = request.getParameter(CentralConstants.PARAM_COURSE_ID);
		String ldId = request.getParameter(CentralConstants.PARAM_LEARNING_DESIGN_ID);
		String learnerSize = request.getParameter(CentralConstants.PARAM_LEARNER_SIZE);
		
		DocumentBuilderFactory factory = null;
		DocumentBuilder builder = null;
		Document document = null;
		Element element = null;
		
		try {
			// Create xml document
			factory = DocumentBuilderFactory.newInstance();
			builder = factory.newDocumentBuilder();
			document = builder.newDocument();
			element = document.createElement("status");
			
			// validation
			if (StringUtils.isBlank(username)) {
				log.debug("Missing parameter: " + CentralConstants.PARAM_USERNAME);
				element.setAttribute(CentralConstants.ATTR_RESULT_CODE, "0");
				element.setAttribute(CentralConstants.ATTR_RESULT_TEXT, "Missing parameter: " + CentralConstants.PARAM_USERNAME);
			} else if (StringUtils.isBlank(orgId)) {
				log.debug("Missing parameter: " + CentralConstants.PARAM_COURSE_ID);
				element.setAttribute(CentralConstants.ATTR_RESULT_CODE, "0");
				element.setAttribute(CentralConstants.ATTR_RESULT_TEXT, "Missing parameter: " + CentralConstants.PARAM_COURSE_ID);
			} else if (StringUtils.isBlank(ldId)) {
				log.debug("Missing parameter: " + CentralConstants.PARAM_LEARNING_DESIGN_ID);
				element.setAttribute(CentralConstants.ATTR_RESULT_CODE, "0");
				element.setAttribute(CentralConstants.ATTR_RESULT_TEXT, "Missing parameter: " + CentralConstants.PARAM_LEARNING_DESIGN_ID);
			} else if (StringUtils.isBlank(learnerSize)) {
				log.debug("Missing parameter: " + CentralConstants.PARAM_LEARNER_SIZE);
				element.setAttribute(CentralConstants.ATTR_RESULT_CODE, "0");
				element.setAttribute(CentralConstants.ATTR_RESULT_TEXT, "Missing parameter: " + CentralConstants.PARAM_LEARNER_SIZE);
			}
			
			// authenticate integrated server
			ExtServerOrgMap serverMap = integrationService.getExtServerOrgMap(serverId);
			Authenticator.authenticate(serverMap, datetime, hashValue);
			
			User user = userService.getUserByLogin(username);
			if (user != null) {
				// add user to organisation if not already a member
				addUserToOrg(user, Integer.valueOf(orgId));
				
				// get list of lessons based on ldId
				List lessons = lessonService.getLessonsByOriginalLearningDesign(Long.valueOf(ldId), Integer.valueOf(orgId));
				if (lessons != null && lessons.size() > 0) {
					
					// add to first lesson where lesson size < learnerSize
					if (addUserToLesson(lessons, Integer.valueOf(learnerSize).intValue(), Integer.valueOf(orgId), user)) {
						// success result code
						element.setAttribute(CentralConstants.ATTR_RESULT_CODE, "1");
					}
					
					// check if need to create new lesson
					String resultCode = element.getAttribute(CentralConstants.ATTR_RESULT_CODE);
					if (StringUtils.isBlank(resultCode)) {
						if (createNewLesson(lessons, Long.valueOf(ldId).longValue(), Integer.valueOf(orgId), user)) {
							// success result code
							element.setAttribute(CentralConstants.ATTR_RESULT_CODE, "1");
						}
					}
				}
			} else {
				log.debug("Couldn't find user with username '" + username + "'.");
				element.setAttribute(CentralConstants.ATTR_RESULT_CODE, "0");
				element.setAttribute(CentralConstants.ATTR_RESULT_TEXT, "no username");
			}
			
			// result code shouldn't be blank at this point, but just in case
			String resultCode = element.getAttribute(CentralConstants.ATTR_RESULT_CODE);
			if (StringUtils.isBlank(resultCode)) element.setAttribute(CentralConstants.ATTR_RESULT_CODE, "0");
			
		} catch (ParserConfigurationException e) {
			log.error(e, e);
		} catch (AuthenticationException e) {
			log.debug(e);
			element.setAttribute(CentralConstants.ATTR_RESULT_CODE, "0");
			element.setAttribute(CentralConstants.ATTR_RESULT_TEXT, "server authentication error");
		} finally {
			if (document != null) {
				document.appendChild(element);

				// write out the xml document.
				try {
					DOMSource domSource = new DOMSource(document);
					StringWriter writer = new StringWriter();
					StreamResult result = new StreamResult(writer);
					TransformerFactory tf = TransformerFactory.newInstance();
					Transformer transformer = tf.newTransformer();
					transformer.transform(domSource, result);
			
					out.write(writer.toString());
				} catch (TransformerConfigurationException e) {
					log.error(e, e);
				} catch (TransformerException e) {
					log.error(e, e);
				}
			}
		}
			
	}
	
	private void addUserToOrg(User user, Integer orgId) {
		UserOrganisation uo = userService.getUserOrganisation(user.getUserId(), orgId);
		if (uo == null) {	
			ArrayList<String> rolesList = new ArrayList<String>();
			rolesList.add(Role.ROLE_LEARNER.toString());
			userService.setRolesForUserOrganisation(user, orgId, rolesList);
		}
	}

	private Boolean addUserToLesson(List lessons, int learnerSize, Integer orgId, User user) {
		for (int i=0; i<lessons.size(); i++) {
			Lesson l = (Lesson)lessons.get(i);
			int size = l.getLessonClass().getLearnersGroup().getUsers().size();
			if (size < learnerSize) {
				// verify lesson's org is the same as the course the user has signed up for
				Organisation org = l.getOrganisation();
				if (!org.getOrganisationId().equals(orgId)) continue;
											
				// add user to lesson (method is idempotent)
				l.getLessonClass().addLearner(user);
				
				if (log.isDebugEnabled()) {
					log.debug("Added " + user.getLogin() + " to lesson (id: " + l.getLessonId() 
							+ ", name: '" + l.getLessonName() + "')");
				}
				return true;
			}
		}
		return false;
	}
	
	private Boolean createNewLesson(List lessons, long ldId, Integer orgId, User user) {
		// create new lesson
		String lessonName;
		String lessonDescription;
		Boolean learnerExportAvailable;
		Integer ownerUserId;
		Organisation org = (Organisation)userService.findById(Organisation.class, orgId);
		String learnerGroupName;
		ArrayList<User> learnerList = new ArrayList<User>();
		learnerList.add(user);
		String staffGroupName;
		List<User> staffList;
	
		if (lessons.size() > 0) {
			// use existing lesson's settings
			Lesson l = (Lesson)lessons.get(lessons.size()-1);
			lessonName = incrementLessonName(l.getLessonName());
			lessonDescription = l.getLessonDescription();
			learnerExportAvailable = l.getLearnerExportAvailable();
			ownerUserId = l.getUser().getUserId();
			learnerGroupName = l.getLessonClass().getLearnersGroup().getGroupName();
			staffGroupName = l.getLessonClass().getStaffGroup().getGroupName();
			staffList = getListFromSet(l.getLessonClass().getStaffGroup().getUsers());
		} else {
			// create new lesson settings
			LearningDesign ld = (LearningDesign)userService.findById(LearningDesign.class, ldId);
			lessonName = ld.getTitle();
			lessonDescription = ld.getDescription();
			learnerExportAvailable = true;
			ownerUserId = ld.getUser().getUserId();
			learnerGroupName = org.getName() + " Learners";
			staffGroupName = org.getName() + " Staff";
			Vector userDTOs = userService.getUsersFromOrganisationByRole(orgId, Role.MONITOR, false);
			staffList = getUsersFromDTOs(userDTOs);
		}
		Lesson lesson = monitoringService.initializeLesson(lessonName, lessonDescription, learnerExportAvailable, 
				ldId, orgId, ownerUserId, null);
		monitoringService.createLessonClassForLesson(lesson.getLessonId().longValue(), org, 
				learnerGroupName, learnerList, staffGroupName, staffList, ownerUserId);
		monitoringService.startLesson(lesson.getLessonId().longValue(), ownerUserId);
		if (log.isDebugEnabled()) {
			log.debug("Started new lesson (id: " + lesson.getLessonId() 
					+ ", name: '" + lesson.getLessonName() + "') with '" + user.getLogin() + "' as learner.");
		}
		return true;
	}
	
	private String incrementLessonName(String name) {
		if (!StringUtils.isBlank(name)) {
			int i = name.lastIndexOf(' ');
			if (i >= 0) {
				String counterStr = name.substring(i).trim();
				try {
					int counterInt = Integer.parseInt(counterStr);
					counterInt++;
					return (name.substring(0, i) + " " + counterInt);
				} catch (NumberFormatException e) {
					return name + " 2";
				}
			} else {
				return name + " 2";
			}
		}
		return name;
	}
	
	private List<User> getListFromSet(Set users) {
		ArrayList<User> list = new ArrayList<User>();
		Iterator i = users.iterator();
		while (i.hasNext()) {
			User user = (User)i.next();
			list.add(user);
		}
		return list;
	}
	
	private List<User> getUsersFromDTOs(Vector userDTOs) {
		ArrayList<User> list = new ArrayList<User>();
		for (Object o : userDTOs) {
			UserDTO dto = (UserDTO)o;
			User user = (User)userService.findById(User.class, dto.getUserID());
			list.add(user);
		}
		return list;
	}

}
 
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

package org.lamsfoundation.lams.web.outcome;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.actions.DispatchAction;
import org.lamsfoundation.lams.outcome.Outcome;
import org.lamsfoundation.lams.outcome.OutcomeScale;
import org.lamsfoundation.lams.outcome.OutcomeScaleItem;
import org.lamsfoundation.lams.outcome.service.IOutcomeService;
import org.lamsfoundation.lams.security.ISecurityService;
import org.lamsfoundation.lams.usermanagement.Organisation;
import org.lamsfoundation.lams.usermanagement.Role;
import org.lamsfoundation.lams.usermanagement.User;
import org.lamsfoundation.lams.usermanagement.dto.UserDTO;
import org.lamsfoundation.lams.usermanagement.service.IUserManagementService;
import org.lamsfoundation.lams.util.WebUtil;
import org.lamsfoundation.lams.web.session.SessionManager;
import org.lamsfoundation.lams.web.util.AttributeNames;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class OutcomeAction extends DispatchAction {

    private static Logger log = Logger.getLogger(OutcomeAction.class);

    private static IUserManagementService userManagementService;
    private static ISecurityService securityService;
    private static IOutcomeService outcomeService;

    public ActionForward outcomeManage(ActionMapping mapping, ActionForm form, HttpServletRequest request,
	    HttpServletResponse response) throws Exception {
	Integer userId = getUserDTO().getUserID();
	Integer organisationId = WebUtil.readIntParam(request, AttributeNames.PARAM_ORGANISATION_ID, true);

	if (organisationId == null) {
	    // check if user is allowed to view and edit global outcomes
	    if (!getSecurityService().isSysadmin(userId, "manage global outcomes", false)) {
		response.sendError(HttpServletResponse.SC_FORBIDDEN, "User is not a sysadmin");
		return null;
	    }
	} else {
	    // check if user is allowed to view and edit course outcomes
	    if (!getSecurityService().hasOrgRole(organisationId, userId, new String[] { Role.AUTHOR },
		    "manage course outcomes", false)) {
		response.sendError(HttpServletResponse.SC_FORBIDDEN, "User is not an author in the organisation");
		return null;
	    }
	}
	List<Outcome> outcomes = getOutcomeService().getOutcomesForManagement(organisationId);
	request.setAttribute("outcomes", outcomes);

	request.setAttribute("canManageGlobal", getUserManagementService().isUserSysAdmin());
	return mapping.findForward("outcomeManage");
    }

    public ActionForward outcomeEdit(ActionMapping mapping, ActionForm form, HttpServletRequest request,
	    HttpServletResponse response) throws Exception {
	Integer userId = getUserDTO().getUserID();
	Long outcomeId = WebUtil.readLongParam(request, "outcomeId", true);
	Outcome outcome = null;
	Integer organisationId = null;
	if (outcomeId == null) {
	    organisationId = WebUtil.readIntParam(request, AttributeNames.PARAM_ORGANISATION_ID, true);
	} else {
	    outcome = (Outcome) getUserManagementService().findById(Outcome.class, outcomeId);
	    if (outcome.getOrganisation() != null) {
		// get organisation ID from the outcome - the safest way
		organisationId = outcome.getOrganisation().getOrganisationId();
	    }
	}

	if (organisationId != null && !getSecurityService().hasOrgRole(organisationId, userId,
		new String[] { Role.AUTHOR }, "add/edit course outcome", false)) {
	    response.sendError(HttpServletResponse.SC_FORBIDDEN, "User is not an author in the organisation");
	    return null;
	}

	OutcomeForm outcomeForm = (OutcomeForm) form;
	outcomeForm.setOrganisationId(organisationId);
	outcomeForm.setContentFolderId(getOutcomeService().getContentFolderId(organisationId));
	if (outcome != null) {
	    outcomeForm.setOutcomeId(outcome.getOutcomeId());
	    outcomeForm.setName(outcome.getName());
	    outcomeForm.setCode(outcome.getCode());
	    outcomeForm.setDescription(outcome.getDescription());
	    outcomeForm.setScaleId(outcome.getScale().getScaleId());
	}

	List<OutcomeScale> scales = getOutcomeService().getScalesForManagement(organisationId);
	request.setAttribute("scales", scales);

	request.setAttribute("canManageGlobal", getUserManagementService().isUserSysAdmin());
	return mapping.findForward("outcomeEdit");
    }

    @SuppressWarnings("unchecked")
    public ActionForward outcomeSave(ActionMapping mapping, ActionForm form, HttpServletRequest request,
	    HttpServletResponse response) throws Exception {
	OutcomeForm outcomeForm = (OutcomeForm) form;
	Integer userId = getUserDTO().getUserID();
	Long outcomeId = outcomeForm.getOutcomeId();
	Outcome outcome = null;
	Integer organisationId = null;
	if (outcomeId == null) {
	    organisationId = outcomeForm.getOrganisationId();
	} else {
	    outcome = (Outcome) getUserManagementService().findById(Outcome.class, outcomeId);
	    if (outcome.getOrganisation() != null) {
		// get organisation ID from the outcome - the safest way
		organisationId = outcome.getOrganisation().getOrganisationId();
	    }
	}

	if (organisationId == null) {
	    if (!getSecurityService().isSysadmin(userId, "persist global outcome", false)) {
		response.sendError(HttpServletResponse.SC_FORBIDDEN, "User is not a sysadmin");
		return null;
	    }
	} else {
	    if (!getSecurityService().hasOrgRole(organisationId, userId, new String[] { Role.AUTHOR },
		    "persist course outcome", false)) {
		response.sendError(HttpServletResponse.SC_FORBIDDEN, "User is not an author in the organisation");
		return null;
	    }
	}

	ActionErrors errors = validateOutcomeForm(outcomeForm);
	if (!errors.isEmpty()) {
	    this.addErrors(request, errors);
	} else {
	    try {
		Organisation organisation = (Organisation) (organisationId == null ? null
			: getUserManagementService().findById(Organisation.class, organisationId));
		if (outcome == null) {
		    outcome = new Outcome();
		    outcome.setOrganisation(organisation);
		    User user = (User) getUserManagementService().findById(User.class, userId);
		    outcome.setCreateBy(user);
		    outcome.setCreateDateTime(new Date());
		}

		outcome.setName(outcomeForm.getName());
		outcome.setCode(outcomeForm.getCode());
		outcome.setDescription(outcomeForm.getDescription());
		outcome.setContentFolderId(outcomeForm.getContentFolderId());
		if (outcomeForm.getScaleId() != null) {
		    OutcomeScale scale = (OutcomeScale) getUserManagementService().findById(OutcomeScale.class,
			    outcomeForm.getScaleId());
		    outcome.setScale(scale);
		}
		getUserManagementService().save(outcome);
		request.setAttribute("saved", true);
	    } catch (Exception e) {
		log.error("Exception while saving an outcome", e);
		errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("outcome.manage.add.error"));
		this.addErrors(request, errors);
	    }
	}

	List<OutcomeScale> scales = getUserManagementService().findAll(OutcomeScale.class);
	request.setAttribute("scales", scales);
	return mapping.findForward("outcomeEdit");
    }

    public ActionForward outcomeRemove(ActionMapping mapping, ActionForm form, HttpServletRequest request,
	    HttpServletResponse response) throws Exception {
	Long outcomeId = WebUtil.readLongParam(request, "outcomeId", false);
	Outcome outcome = (Outcome) getUserManagementService().findById(Outcome.class, outcomeId);
	if (outcome == null) {
	    throw new IllegalArgumentException("Can not find an outcome with ID " + outcomeId);
	}
	Integer organisationId = outcome.getOrganisation() == null ? null
		: outcome.getOrganisation().getOrganisationId();
	Integer userId = getUserDTO().getUserID();

	if (organisationId == null) {
	    if (!getSecurityService().isSysadmin(userId, "remove global outcome", false)) {
		response.sendError(HttpServletResponse.SC_FORBIDDEN, "User is not a sysadmin");
		return null;
	    }
	} else {
	    if (!getSecurityService().hasOrgRole(organisationId, userId, new String[] { Role.AUTHOR },
		    "remove course outcome", false)) {
		response.sendError(HttpServletResponse.SC_FORBIDDEN, "User is not an author in the organisation");
		return null;
	    }
	}
	getUserManagementService().delete(outcome);
	return outcomeManage(mapping, form, request, response);
    }

    public ActionForward scaleManage(ActionMapping mapping, ActionForm form, HttpServletRequest request,
	    HttpServletResponse response) throws Exception {
	Integer userId = getUserDTO().getUserID();
	Integer organisationId = WebUtil.readIntParam(request, AttributeNames.PARAM_ORGANISATION_ID, true);

	if (organisationId == null) {
	    // check if user is allowed to view and edit global outcomes
	    if (!getSecurityService().isSysadmin(userId, "manage global scales", false)) {
		response.sendError(HttpServletResponse.SC_FORBIDDEN, "User is not a sysadmin");
		return null;
	    }
	} else {
	    // check if user is allowed to view and edit course outcomes
	    if (!getSecurityService().hasOrgRole(organisationId, userId, new String[] { Role.AUTHOR },
		    "manage course scales", false)) {
		response.sendError(HttpServletResponse.SC_FORBIDDEN, "User is not an author in the organisation");
		return null;
	    }
	}
	List<OutcomeScale> scales = getOutcomeService().getScalesForManagement(organisationId);
	request.setAttribute("scales", scales);

	request.setAttribute("canManageGlobal", getUserManagementService().isUserSysAdmin());
	return mapping.findForward("scaleManage");
    }

    public ActionForward scaleRemove(ActionMapping mapping, ActionForm form, HttpServletRequest request,
	    HttpServletResponse response) throws Exception {
	Long scaleId = WebUtil.readLongParam(request, "scaleId", false);
	OutcomeScale scale = (OutcomeScale) getUserManagementService().findById(OutcomeScale.class, scaleId);
	if (scale == null) {
	    throw new IllegalArgumentException("Can not find an outcome scale with ID " + scaleId);
	}
	Integer organisationId = scale.getOrganisation() == null ? null : scale.getOrganisation().getOrganisationId();
	Integer userId = getUserDTO().getUserID();

	if (organisationId == null) {
	    if (!getSecurityService().isSysadmin(userId, "remove global scale", false)) {
		response.sendError(HttpServletResponse.SC_FORBIDDEN, "User is not a sysadmin");
		return null;
	    }
	} else {
	    if (!getSecurityService().hasOrgRole(organisationId, userId, new String[] { Role.AUTHOR },
		    "remove course scale", false)) {
		response.sendError(HttpServletResponse.SC_FORBIDDEN, "User is not an author in the organisation");
		return null;
	    }
	}
	getUserManagementService().delete(scale);
	return scaleManage(mapping, form, request, response);
    }

    public ActionForward scaleEdit(ActionMapping mapping, ActionForm form, HttpServletRequest request,
	    HttpServletResponse response) throws Exception {
	Integer userId = getUserDTO().getUserID();
	Long scaleId = WebUtil.readLongParam(request, "scaleId", true);
	OutcomeScale scale = null;
	Integer organisationId = null;
	if (scaleId == null) {
	    organisationId = WebUtil.readIntParam(request, AttributeNames.PARAM_ORGANISATION_ID, true);
	} else {
	    scale = (OutcomeScale) getUserManagementService().findById(OutcomeScale.class, scaleId);
	    if (scale.getOrganisation() != null) {
		// get organisation ID from the outcome - the safest way
		organisationId = scale.getOrganisation().getOrganisationId();
	    }
	}

	if (organisationId != null && !getSecurityService().hasOrgRole(organisationId, userId,
		new String[] { Role.AUTHOR }, "add/edit course outcome", false)) {
	    response.sendError(HttpServletResponse.SC_FORBIDDEN, "User is not an author in the organisation");
	    return null;
	}

	OutcomeScaleForm scaleForm = (OutcomeScaleForm) form;
	scaleForm.setOrganisationId(organisationId);
	scaleForm.setContentFolderId(getOutcomeService().getContentFolderId(organisationId));
	if (scale != null) {
	    scaleForm.setScaleId(scale.getScaleId());
	    scaleForm.setName(scale.getName());
	    scaleForm.setCode(scale.getCode());
	    scaleForm.setDescription(scale.getDescription());
	    scaleForm.setItems(scale.getItemString());
	}

	request.setAttribute("canManageGlobal", getUserManagementService().isUserSysAdmin());
	return mapping.findForward("scaleEdit");
    }

    public ActionForward scaleSave(ActionMapping mapping, ActionForm form, HttpServletRequest request,
	    HttpServletResponse response) throws Exception {
	OutcomeScaleForm scaleForm = (OutcomeScaleForm) form;
	Integer userId = getUserDTO().getUserID();
	Long scaleId = scaleForm.getScaleId();
	OutcomeScale scale = null;
	Integer organisationId = null;
	if (scaleId == null) {
	    organisationId = scaleForm.getOrganisationId();
	} else {
	    scale = (OutcomeScale) getUserManagementService().findById(OutcomeScale.class, scaleId);
	    if (scale.getOrganisation() != null) {
		// get organisation ID from the outcome - the safest way
		organisationId = scale.getOrganisation().getOrganisationId();
	    }
	}

	if (organisationId == null) {
	    if (!getSecurityService().isSysadmin(userId, "persist global scale", false)) {
		response.sendError(HttpServletResponse.SC_FORBIDDEN, "User is not a sysadmin");
		return null;
	    }
	} else {
	    if (!getSecurityService().hasOrgRole(organisationId, userId, new String[] { Role.AUTHOR },
		    "persist course scale", false)) {
		response.sendError(HttpServletResponse.SC_FORBIDDEN, "User is not an author in the organisation");
		return null;
	    }
	}

	ActionErrors errors = validateScaleForm(scaleForm);
	List<String> items = OutcomeScale.parseItems(scaleForm.getItems());
	if (items == null) {
	    errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("scale.manage.add.value.error.blank"));
	}
	if (errors.isEmpty()) {
	    try {
		Organisation organisation = (Organisation) (organisationId == null ? null
			: getUserManagementService().findById(Organisation.class, organisationId));
		if (scale == null) {
		    scale = new OutcomeScale();
		    scale.setOrganisation(organisation);
		    User user = (User) getUserManagementService().findById(User.class, userId);
		    scale.setCreateBy(user);
		    scale.setCreateDateTime(new Date());
		}

		scale.setName(scaleForm.getName());
		scale.setCode(scaleForm.getCode());
		scale.setDescription(scaleForm.getDescription());
		scale.setContentFolderId(scaleForm.getContentFolderId());
		getUserManagementService().save(scale);

		// find existing scales and add new ones
		Set<OutcomeScaleItem> newItems = new LinkedHashSet<>();
		int value = 0;
		for (String itemString : items) {
		    itemString = itemString.trim();
		    if (StringUtils.isBlank(itemString)) {
			errors.add(ActionMessages.GLOBAL_MESSAGE,
				new ActionMessage("scale.manage.add.value.error.blank"));
			break;
		    }
		    OutcomeScaleItem item = null;
		    for (OutcomeScaleItem exisitngItem : scale.getItems()) {
			if (itemString.equals(exisitngItem.getName())) {
			    item = exisitngItem;
			    break;
			}
		    }
		    if (item == null) {
			item = new OutcomeScaleItem();
			item.setScale(scale);
			item.setName(itemString);
		    }
		    item.setValue(value++);
		    newItems.add(item);
		}
		if (errors.isEmpty()) {
		    scale.getItems().clear();
		    scale.getItems().addAll(newItems);
		    getUserManagementService().save(scale);

		    request.setAttribute("saved", true);
		}
	    } catch (Exception e) {
		log.error("Exception while saving an outcome", e);
		errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("scale.manage.add.error"));
		this.addErrors(request, errors);
	    }
	}
	if (!errors.isEmpty()) {
	    this.addErrors(request, errors);
	}

	return mapping.findForward("scaleEdit");
    }

    private UserDTO getUserDTO() {
	HttpSession ss = SessionManager.getSession();
	return (UserDTO) ss.getAttribute(AttributeNames.USER);
    }

    private ActionErrors validateOutcomeForm(OutcomeForm outcomeForm) {
	ActionErrors errors = new ActionErrors();
	if (StringUtils.isBlank(outcomeForm.getName())) {
	    errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("outcome.manage.add.error.name.blank"));
	}
	if (StringUtils.isBlank(outcomeForm.getCode())) {
	    errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("outcome.manage.add.error.code.blank"));
	}
	if (outcomeForm.getScaleId() == null || outcomeForm.getScaleId() == 0) {
	    errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("outcome.manage.add.error.scale.choose"));
	}
	return errors;
    }

    private ActionErrors validateScaleForm(OutcomeScaleForm scaleForm) {
	ActionErrors errors = new ActionErrors();
	if (StringUtils.isBlank(scaleForm.getName())) {
	    errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("outcome.manage.add.error.name.blank"));
	}
	if (StringUtils.isBlank(scaleForm.getCode())) {
	    errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("outcome.manage.add.error.code.blank"));
	}
	return errors;
    }

    private IUserManagementService getUserManagementService() {
	if (OutcomeAction.userManagementService == null) {
	    WebApplicationContext ctx = WebApplicationContextUtils
		    .getRequiredWebApplicationContext(getServlet().getServletContext());
	    OutcomeAction.userManagementService = (IUserManagementService) ctx.getBean("userManagementService");
	}
	return OutcomeAction.userManagementService;
    }

    private ISecurityService getSecurityService() {
	if (OutcomeAction.securityService == null) {
	    WebApplicationContext ctx = WebApplicationContextUtils
		    .getRequiredWebApplicationContext(getServlet().getServletContext());
	    OutcomeAction.securityService = (ISecurityService) ctx.getBean("securityService");
	}
	return OutcomeAction.securityService;
    }

    private IOutcomeService getOutcomeService() {
	if (OutcomeAction.outcomeService == null) {
	    WebApplicationContext ctx = WebApplicationContextUtils
		    .getRequiredWebApplicationContext(getServlet().getServletContext());
	    OutcomeAction.outcomeService = (IOutcomeService) ctx.getBean("outcomeService");
	}
	return OutcomeAction.outcomeService;
    }
}
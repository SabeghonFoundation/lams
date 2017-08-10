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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 * USA
 *
 * http://www.gnu.org/licenses/gpl.txt
 * ****************************************************************
 */

package org.lamsfoundation.lams.web;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;
import org.lamsfoundation.lams.events.Event;
import org.lamsfoundation.lams.events.IEventNotificationService;
import org.lamsfoundation.lams.events.Subscription;
import org.lamsfoundation.lams.usermanagement.dto.UserDTO;
import org.lamsfoundation.lams.util.WebUtil;
import org.lamsfoundation.lams.web.session.SessionManager;
import org.lamsfoundation.lams.web.util.AttributeNames;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Processes notification sent from Tools to users.
 *
 *
 */
public class NotificationAction extends DispatchAction {

    private static IEventNotificationService eventNotificationService;

    public ActionForward getNotificationSubscriptions(ActionMapping mapping, ActionForm form, HttpServletRequest req,
	    HttpServletResponse res) throws IOException {
	Integer limit = WebUtil.readIntParam(req, "limit", true);
	Integer offset = WebUtil.readIntParam(req, "offset", true);
	List<Subscription> subscriptions = getEventNotificationService().getNotificationSubscriptions(null,
		getUser().getUserID(), false, limit, offset);
	ArrayNode responseJSON = JsonNodeFactory.instance.arrayNode();
	for (Subscription subscription : subscriptions) {
	    Event event = subscription.getEvent();
	    ObjectNode subscriptionJSON = JsonNodeFactory.instance.objectNode();
	    subscriptionJSON.put("subscriptionUid", subscription.getUid());
	    subscriptionJSON.put("message", event.getMessage());
	    subscriptionJSON.put("pending", subscription.getLastOperationMessage() == null);
	    responseJSON.add(subscriptionJSON);
	}

	if (responseJSON.size() > 0) {
	    res.setContentType("application/json;charset=UTF-8");
	    res.getWriter().print(responseJSON.toString());
	}
	return null;
    }

    public ActionForward markNotificationAsRead(ActionMapping mapping, ActionForm form, HttpServletRequest req,
	    HttpServletResponse res) {
	long subscriptionUid = WebUtil.readLongParam(req, "subscriptionUid");
	// trigger means send, i.e. mark as "seen"
	getEventNotificationService().triggerForSingleUser(subscriptionUid, null, null);
	return null;
    }

    public ActionForward getPendingNotificationCount(ActionMapping mapping, ActionForm form, HttpServletRequest req,
	    HttpServletResponse res) throws IOException {
	long count = getEventNotificationService().getNotificationPendingCount(null, getUser().getUserID());
	res.setContentType("text/plain;charset=UTF-8");
	res.getWriter().print(count);
	return null;
    }

    private IEventNotificationService getEventNotificationService() {
	if (NotificationAction.eventNotificationService == null) {
	    WebApplicationContext ctx = WebApplicationContextUtils
		    .getRequiredWebApplicationContext(getServlet().getServletContext());
	    NotificationAction.eventNotificationService = (IEventNotificationService) ctx
		    .getBean("eventNotificationService");
	}
	return NotificationAction.eventNotificationService;
    }

    private UserDTO getUser() {
	HttpSession ss = SessionManager.getSession();
	return (UserDTO) ss.getAttribute(AttributeNames.USER);
    }
}
<% 
/****************************************************************
 * Copyright (C) 2005 LAMS Foundation (http://lamsfoundation.org)
 * =============================================================
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
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
 
 /**
  * Tab.tag
  *	Author: Mitchell Seaton
  *	Description: Creates a tab element.
  * Wiki: 
  */
 
 %>
<%@ tag body-content="empty" %>
<%@ attribute name="id" required="true" rtexprvalue="true" %>
<%@ attribute name="value" required="false" rtexprvalue="true" %>
<%@ attribute name="key" required="false" rtexprvalue="true" %>
<%@ taglib uri="tags-core" prefix="c" %>
<%@ taglib uri="tags-bean" prefix="bean" %>
<%@ taglib uri="tags-lams" prefix="lams" %>
<c:set var="lams"><lams:LAMSURL/></c:set>
<c:set var="methodCall" value="selectTab"/>
<c:set var="title" value="${value}"/>
<c:if test="${dControl}">
	<c:set var="methodCall" value="doSelectTab"/>
</c:if>
<c:if test="${key != null && value == null}">
	<c:set var="title"><bean:message name="key" scope="page"/></c:set>
</c:if>

<td>
	<table border="0" cellspacing="0" cellpadding="0" width="120" summary="This table is being used for layout purposes only">
	  <tr>
		<td width="8"><a href="#" onClick="${methodCall}(${id});return false;" ><img src="${lams}images/aqua_tab_left.gif" name="tableft_${id}" width="8" height="22" border="0" id="tableft_${id}"/></a></td>
		<td class="tab tabcentre" id="tab${id}"  onClick="${methodCall}(${id});return false;"  nowrap="nowrap"><a href="#" onClick="${methodCall}(${id});return false;" id="${id}" >${title}</a></td>
		<td width="8"><a href="#" onClick="${methodCall}(${id});return false;" ><img src="${lams}images/aqua_tab_right.gif" name="tabright_${id}" width="8" height="22" border="0" id="tabright_${id}"/></a></td></tr>
	</table>
</td>
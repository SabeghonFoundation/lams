<%@ include file="/common/taglibs.jsp"%>
<div id="optionArea">
	<input type="hidden" name="optionCount" id="optionCount" value="${fn:length(optionList)}">
	<input type="hidden" name="questionType" id="questionType" value="${questionType}" />
	
	<table class="alternative-color" cellspacing="0">
		<c:forEach var="option" items="${optionList}" varStatus="status">
			<tr id="optionItem${status.index}">
				<td width="3px" style="vertical-align:middle;">
					${status.index+1}
				</td>
				<td style="padding-left:10px;">
					<c:choose>
						<c:when test="${(questionType == 1) || (questionType == 3)}">
							<%@ include file="option.jsp"%>
						</c:when>
						<c:when test="${questionType == 2}">
							<%@ include file="matchingpair.jsp"%>
						</c:when>
						<c:when test="${questionType == 4}">
							<%@ include file="numerical.jsp"%>
						</c:when>						
					</c:choose>	
				</td>
					
				<td width="15px" style="padding-left:0px; vertical-align:middle; text-align: center;">
					<c:if test="${not status.first}">
						<img src="<html:rewrite page='/includes/images/uparrow.gif'/>"
							border="0" title="<fmt:message key="label.authoring.basic.up"/>"
							onclick="upOption(${status.index})">
						<c:if test="${status.last}">
							<img
								src="<html:rewrite page='/includes/images/downarrow_disabled.gif'/>"
								border="0" title="<fmt:message key="label.authoring.basic.down"/>">
						</c:if>
					</c:if>
	
					<c:if test="${not status.last}">
						<c:if test="${status.first}">
							<img
								src="<html:rewrite page='/includes/images/uparrow_disabled.gif'/>"
								border="0" title="<fmt:message key="label.authoring.basic.up"/>">
						</c:if>
	
						<img src="<html:rewrite page='/includes/images/downarrow.gif'/>"
							border="0" title="<fmt:message key="label.authoring.basic.down"/>"
							onclick="downOption(${status.index})">
					</c:if>
				</td>
	                
				<td width="20px" style="padding-left:0px; vertical-align:middle;">
					<img src="<html:rewrite page='/includes/images/cross.gif'/>"
						title="<fmt:message key="label.authoring.basic.delete" />"
						onclick="removeOption(${status.index})" />
				</td>
			</tr>
		</c:forEach>
	</table>
</div>
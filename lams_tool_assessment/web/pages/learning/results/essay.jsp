<%@ include file="/common/taglibs.jsp"%>

<div class="question-type">
	<fmt:message key="label.learning.short.answer.answer" />
</div>

<div class="table-responsive">
	<table class="table table-hover table-condensed">
		<tr>
			<td>
				${question.answer}
			</td>
		</tr>
	</table>
</div>

<c:if test="${assessment.allowDiscloseAnswers && fn:length(sessions) > 1}">
	<table class="table table-responsive table-striped table-bordered table-hover table-condensed">
		<tr role="row">
			<td colspan="2" class="text-center"><b><fmt:message key="label.learning.summary.other.team.answers"/></b></td>
		</tr>
		<c:forEach var="session" items="${sessions}" varStatus="status">
			<%-- Default answer value, when answers are not disclosed yet --%>
			<c:set var="answer"><fmt:message key="label.not.yet.disclosed"/></c:set>
			
			<c:if test="${question.groupsAnswersDisclosed}">
				<%-- Get the needed piece of information from a complicated questionSummaries structure --%>
				<c:set var="questionSummary" value="${questionSummaries[question.uid]}" />
				<c:set var="sessionResults" 
					value="${questionSummary.questionResultsPerSession[status.index]}" />
				<c:set var="sessionResults" value="${sessionResults[fn:length(sessionResults)-1]}" />
				<c:set var="answer" value="${sessionResults.answer}" />
				<c:set var="itemRatingDto" value="${itemRatingDtos[sessionResults.uid]}" />
				<c:set var="canRate" value="${toolSessionID != session.sessionId and (!isLeadershipEnabled or isUserLeader)}" />
				<c:set var="showRating" 
					value="${canRate or (not empty itemRatingDto.commentDtos and (toolSessionID != session.sessionId or questionSummary.showOwnGroupRating))}" />
			</c:if>
			
			<%-- Show answers for all other teams, and just rating if someone has already commented on this team's answer --%>
			<c:if test="${toolSessionID != session.sessionId or showRating}">
				<tr role="row" ${toolSessionID == session.sessionId ? 'class="bg-success"' : ''}>
					<td class="text-center" style="width: 33%" ${showRating ? 'rowspan="2"' : ''}>
						<lams:Portrait userId="${session.groupLeader.userId}"/>&nbsp;
						<c:choose>
							<c:when test="${toolSessionID == session.sessionId}">
								<b><fmt:message key="label.your.team"/></b>
							</c:when>
							<c:otherwise>
								<%-- Sessions are named after groups --%>
								<c:out value="${session.sessionName}" escapeXml="true"/> 
							</c:otherwise>
						</c:choose>
					</td>
					
					<%-- Do not show your own answer --%> 
					<c:if test="${toolSessionID != session.sessionId}">
							<td>
								<c:out value="${answer}" escapeXml="false" /> 
							</td>
						</tr>
					</c:if>
					
					<c:if test="${showRating}">
						<tr>
							<td>
								<%-- Do not allow voting for own answer, and for non-leaders if leader is enabled --%>
								<lams:Rating itemRatingDto="${itemRatingDto}"
											 isItemAuthoredByUser="${not canRate}"
											 showAllComments="true"
											 refreshOnComment="true" />
							</td>
						</tr>
					</c:if>
				
			</c:if>
		</c:forEach>
	</table>
</c:if>
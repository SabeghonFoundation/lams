<%@ include file="/common/taglibs.jsp"%>
<!DOCTYPE html>


<lams:html>
<lams:head>
	<title><fmt:message key="label.learning.title" /></title>
	<%@ include file="/common/header.jsp"%>

	<script type="text/javascript">
		function disableFinishButton() {
			document.getElementById("finishButton").disabled = true;
		}
	</script>
</lams:head>
<body class="stripes">

	<c:set var="sessionMapID" value="${param.sessionMapID}" />
	<c:set var="sessionMap" value="${sessionScope[sessionMapID]}" />

	<form:form action="/learning/submitReflection" modelAttribute="reflectionForm" method="post" onsubmit="disableFinishButton();" id="messageForm">
		<form:hidden property="userID" />
		<form:hidden property="sessionMapID" />

		<lams:Page type="learner" title="${sessionMap.title}">

			<%@ include file="/common/messages.jsp"%>

			<p>
				<lams:out value="${sessionMap.reflectInstructions}" escapeHtml="true" />
			</p>

			<form:input cols="60" id="notebookEntry" path="entryText" cssClass="form-control" rows="5" />

			<div class="voffset10">
				<button type="submit" class="btn btn-primary pull-right na" id="finishButton">
					<span class="nextActivity"> <c:choose>
							<c:when test="${sessionMap.activityPosition.last}">
								<fmt:message key="label.submit" />
							</c:when>
							<c:otherwise>
								<fmt:message key="label.finished" />
							</c:otherwise>
						</c:choose>
					</span>
				</button>
			</div>

			<div id="footer"></div>
			<!--closes footer-->
		</lams:Page>
			
	</form:form>

</body>
</lams:html>

<%@ include file="/common/taglibs.jsp"%>

<c:set var="sessionMapID" value="${param.sessionMapID}" />
<c:set var="sessionMap" value="${sessionScope[sessionMapID]}" />
<c:set var="kaltura" value="${sessionMap.kaltura}" />

<script type="text/javascript">
	function disableFinishButton() {
		document.getElementById("finishButton").disabled = true;
	}
	function submitForm(methodName){
		var f = document.getElementById('messageForm');
		f.submit();
	}
</script>

<div data-role="header" data-theme="b" data-nobackbtn="true">
	<h1>
		${kaltura.title}
	</h1>
</div>
	
<div data-role="content">
	<html:form action="/learning/submitReflection" method="post" onsubmit="disableFinishButton();" styleId="messageForm">
		<html:hidden property="userID" />
		<html:hidden property="sessionMapID" />
	
	
		<%@ include file="/common/messages.jsp"%>
	
		<p>
			<lams:out value="${kaltura.reflectInstructions}" />
		</p>
	
		<html:textarea cols="60" rows="8" property="entryText" styleClass="text-area" />
	
		<div class="space-bottom-top align-right">
			<html:link href="#nogo" styleClass="button" styleId="finishButton" onclick="submitForm('finish')">
				<span class="nextActivity"><fmt:message key="button.finish" /></span>
			</html:link>
		</div>
	</html:form>
</div>
		
<div data-role="footer" data-theme="b">
	<h2>&nbsp;</h2>
</div>

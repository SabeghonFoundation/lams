<!DOCTYPE html>
<html>
<%@ page language="java" isErrorPage="true" pageEncoding="UTF-8" contentType="text/html;charset=utf-8"%>
<%@ taglib uri="tags-lams" prefix="lams"%>
<%@ taglib uri="tags-core" prefix="c"%>
<%@ taglib uri="tags-fmt" prefix="fmt"%>
<c:set var="lams">
	<lams:LAMSURL />
</c:set>

<%-- Catch JSP Servlet Exception --%>
<%
	if (exception != null) {
%>
<c:set var="errorMessage">
	<%=exception.getMessage()%>
</c:set>
<c:set var="errorName">
	<%=exception.getClass().getName()%>
</c:set>
<%
	java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
		java.io.PrintStream os = new java.io.PrintStream(bos);
		exception.printStackTrace(os);
		String stack = new String(bos.toByteArray());
%>
<c:set var="errorStack">
	<%=stack%>
</c:set>
<%
	} else if ((Exception) request
			.getAttribute("javax.servlet.error.exception") != null) {
%>

<c:set var="errorMessage">
	<%=((Exception) request
							.getAttribute("javax.servlet.error.exception"))
							.getMessage()%>
</c:set>
<c:set var="errorName">
	<%=((Exception) request
							.getAttribute("javax.servlet.error.exception"))
							.getMessage().getClass().getName()%>
</c:set>
<%
	java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
		java.io.PrintStream os = new java.io.PrintStream(bos);
		((Exception) request
				.getAttribute("javax.servlet.error.exception"))
				.printStackTrace(os);
		String stack = new String(bos.toByteArray());
%>
<c:set var="errorStack">
	<%=stack%>
</c:set>
<%
	}
%>
<body class="stripes">

	<lams:Page type="learner" title="">
		<form action="${lams}errorpages/error.jsp" method="post" id="errorForm">
			<input type="hidden" name="errorName" value="<c:out value='${errorName}' />" /> <input type="hidden"
				name="errorMessage" value="<c:out value='${errorMessage}' />" /> <input type="hidden" name="errorStack"
				value="<c:out value='${errorStack}' />" />
		</form>

		<script type="text/javascript">
			if (window.top != null)
				document.getElementById("errorForm").target = "_parent";
			document.getElementById("errorForm").submit();
		</script>
	</lams:Page>
</body>
</html>

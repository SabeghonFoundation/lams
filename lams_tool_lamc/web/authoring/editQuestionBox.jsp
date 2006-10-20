<%--
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
  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA

  http://www.gnu.org/licenses/gpl.txt
--%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN"
        "http://www.w3.org/TR/html4/strict.dtd">

<%@ include file="/common/taglibs.jsp"%>
<html>
	<head>
		<%@ include file="/common/header.jsp"%>

		<script language="JavaScript" type="text/JavaScript">
		
			function showMessage(url) {
				var area=document.getElementById("caArea");
				if(area != null){
					area.style.width="670px";
					area.style.height="100%";
					area.src=url;
					area.style.display="block";
				}
			}
			function hideMessage(){
				var area=document.getElementById("caArea");
				if(area != null){
					area.style.width="0px";
					area.style.height="0px";
					area.style.display="none";
				}
			}

			function submitMethod() {
				document.McAuthoringForm.submit();
			}
			
			function submitMethod(actionMethod) {
				document.McAuthoringForm.dispatch.value=actionMethod; 
				document.McAuthoringForm.submit();
			}
				
			function submitModifyAuthoringCandidate(questionIndexValue, candidateIndexValue, actionMethod) 
			{
				document.McAuthoringForm.candidateIndex.value=candidateIndexValue; 
				document.McAuthoringForm.questionIndex.value=questionIndexValue; 
				submitMethod(actionMethod);
			}

			function removeCandidate(questionIndexValue, candidateIndexValue) 
			{
				document.McAuthoringForm.candidateIndex.value=candidateIndexValue; 
				document.McAuthoringForm.questionIndex.value=questionIndexValue; 
				submitMethod("removeCandidate");
			}
		
		</script>
	</head>

	<body>

		<html:form action="/authoring?validate=false"
			styleId="newQuestionForm" enctype="multipart/form-data" method="POST">

			<html:hidden property="dispatch" value="saveSingleQuestion" />
			<html:hidden property="toolContentID" />
			<html:hidden property="currentTab" styleId="currentTab" />
			<html:hidden property="activeModule" />
			<html:hidden property="httpSessionID" />
			<html:hidden property="defaultContentIdStr" />
			<html:hidden property="defineLaterInEditMode" />
			<html:hidden property="contentFolderID" />
			<html:hidden property="editableQuestionIndex" />
			<html:hidden property="editQuestionBoxRequest" value="true" />
			<html:hidden property="totalMarks" />

			<div class="field-name space-top">
				<fmt:message key="label.edit.question"></fmt:message>
			</div>

			<lams:FCKEditor id="newQuestion"
				value="${mcGeneralAuthoringDTO.editableQuestionText}"
				contentFolderID="${mcGeneralAuthoringDTO.contentFolderID}"></lams:FCKEditor>

			<%@ include file="/authoring/candidateAnswersList.jsp"%>

			<div class="space-bottom small-space-top">
				<div class="right-buttons">
					<html:submit onclick="javascript:submitMethod('newCandidateBox');"
						styleClass="button">
						<fmt:message key="label.add.candidates" />
					</html:submit>
				</div>

				<fmt:message key="label.questions.worth"></fmt:message>
				<select name="mark">
					<c:forEach var="markEntry"
						items="${mcGeneralAuthoringDTO.marksMap}">
						<c:set var="SELECTED_MARK" scope="request" value="" />
						<c:if test="${markEntry.value == mcGeneralAuthoringDTO.markValue}">
							<c:set var="SELECTED_MARK" scope="request" value="SELECTED" />
						</c:if>

						<option value="<c:out value="${markEntry.value}"/>"
							<c:out value="${SELECTED_MARK}"/>>
							<c:out value="${markEntry.value}" />
						</option>
					</c:forEach>
				</select>
				<fmt:message key="label.marks"></fmt:message>
			</div>

			<div class="field-name">
				<fmt:message key="label.feedback"></fmt:message>
			</div>
			<html:textarea property="feedback" rows="3" cols="70"></html:textarea>

			<div class="space-bottom-top">
				<a href="#" onclick="getElementById('newQuestionForm').submit();"
					class="button-add-item"> <fmt:message key="label.save.question" />
				</a>

				<a href="#" onclick="javascript:window.parent.hideMessage()"
					class="button space-left"> <fmt:message key="label.cancel" />
				</a>
			</div>

		</html:form>
	</body>
</html>

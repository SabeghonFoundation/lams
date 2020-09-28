<%@ include file="/common/taglibs.jsp"%>
<c:set var="sessionMap" value="${sessionScope[sessionMapID]}" />
<%@ page import="org.lamsfoundation.lams.qb.service.IQbService" %>

<script type="text/javascript" src="<lams:LAMSURL/>includes/javascript/Sortable.js"></script>
<script>
	// Inform author whether the QB question was modified
	var qbQuestionModified = ${empty qbQuestionModified ? 0 : qbQuestionModified},
		qbMessage = null;
	switch (qbQuestionModified) {
		case <%= IQbService.QUESTION_MODIFIED_UPDATE %>: 
			qbMessage = '<fmt:message key="message.qb.modified.update" />';
			break;
		case <%= IQbService.QUESTION_MODIFIED_VERSION_BUMP %>: 
			qbMessage = '<fmt:message key="message.qb.modified.version" />';
			break;
		case <%= IQbService.QUESTION_MODIFIED_ID_BUMP %>: 
			qbMessage = '<fmt:message key="message.qb.modified.new" />';
			break;
	}
	if (qbMessage) {
		alert(qbMessage);
	}

	$(document).ready(function(){
	    //init questions sorting
	    <c:if test="${not empty sessionMap.questionReferences}">
	    new Sortable($('#referencesTable tbody')[0], {
		    animation: 150,
		    direction: 'vertical',
			store: {
				set: function (sortable) {
					//update all sequenceIds
					for (var i = 0; i < sortable.el.rows.length; i++) {
					 	var tr = sortable.el.rows[i];
					 	var input = $("input[name^=sequenceId]", tr);
					 	input.val(i);
					 	var displayOrder = $(".reference-display-order", tr);
					 	displayOrder.text(i + 1 + ")");
					}

					//prepare SequenceIds parameter
					var serializedSequenceIds = "";
					$("[name^=sequenceId]").each(function() {
						serializedSequenceIds += "&" + this.name + "="  + this.value;
					});

					$.ajax({ 
					    url: '<c:url value="/authoring/cacheReferencesOrder.do"/>',
						type: 'POST',
						data: {
							sessionMapID: "${sessionMapID}",
							sequenceIds: serializedSequenceIds
						}
					});

					//update names
					$("[name^=sequenceId]").each(function() {
						var newSequenceId = this.value;
						//update name of the hidden input
						this.name = "sequenceId" + newSequenceId;
					});
				}
			}
		});
		</c:if>
	});
</script>

<table class="table table-sm" id="referencesTable">
	<thead>
		<tr>
			<th>
				#
			</th>
			<th>
				<fmt:message key="label.authoring.basic.list.header.question" />
			</th>
			<th colspan="4">
				<fmt:message key="label.authoring.basic.list.header.mark" />
			</th>
		</tr>
	</thead>	
	<tbody>
		<c:forEach var="questionReference" items="${sessionMap.questionReferences}" varStatus="status">
			<c:set var="question" value="${questionReference.question}" />
			<tr>
				<td class="reference-display-order align-middle">
					${status.count})
				</td>
				<td class="align-middle">
					<input type="hidden" name="sequenceId${questionReference.sequenceId}" value="${status.index}" class="reference-sequence-id">
				
					<c:choose>
						<c:when test="${questionReference.randomQuestion}">
							<fmt:message key="label.authoring.basic.type.random.question" />
						</c:when>
						<c:otherwise>
							<c:out value="${question.qbQuestion.name}" escapeXml="true"/>
						</c:otherwise>
					</c:choose>
					
					<c:if test="${!questionReference.randomQuestion}">
				        <span class='float-right btn btn-sm btn-info mx-2'>
				       		v.&nbsp;${question.qbQuestion.version}
				        </span>
			        </c:if>
			        
			       	<span class='float-right btn btn-sm btn-info'>
						<c:choose>
							<c:when test="${questionReference.randomQuestion}">
								<fmt:message key="label.authoring.basic.type.random.question" />
							</c:when>
							<c:when test="${question.type == 1}">
								<fmt:message key="label.authoring.basic.type.multiple.choice" />
							</c:when>
							<c:when test="${question.type == 2}">
								<fmt:message key="label.authoring.basic.type.matching.pairs" />
							</c:when>
							<c:when test="${question.type == 3}">
								<fmt:message key="label.authoring.basic.type.short.answer" />
							</c:when>
							<c:when test="${question.type == 4}">
								<fmt:message key="label.authoring.basic.type.numerical" />
							</c:when>
							<c:when test="${question.type == 5}">
								<fmt:message key="label.authoring.basic.type.true.false" />
							</c:when>
							<c:when test="${question.type == 6}">
								<fmt:message key="label.authoring.basic.type.essay" />
							</c:when>
							<c:when test="${question.type == 7}">
								<fmt:message key="label.authoring.basic.type.ordering" />
							</c:when>
							<c:when test="${question.type == 8}">
								<fmt:message key="label.authoring.basic.type.mark.hedging" />
							</c:when>
						</c:choose>
	       			</span>
				</td>
				
				<td class="align-middle" style="width: 70px; padding-right: 10px;">
					<input name="maxMark" value="${questionReference.maxMark}" class="form-control form-control-sm max-mark-input">
				</td>
				
				<td class="align-middle" style="width: 30px">
					<i class="fa fa-xs fa-asterisk ${question.answerRequired ? 'text-danger' : ''}" 
								role="button"
								title="<fmt:message key="label.answer.required"/>" 
								alt="<fmt:message key="label.answer.required"/>"
								onClick="javascript:toggleQuestionRequired(this)"></i>
				</td>
				
				<td class="align-middle" style="width: 30px">
					<a class="thickbox edit-reference-link" onclick="javascript:editReference(this);" style="color: black;"> 
						<i class="fa fa-pencil"	role="button" title="<fmt:message key="label.authoring.basic.edit" />"></i>
					</a>
				</td>

				<td class="align-middle" style="width: 30px">
					<i class="fa fa-times delete-reference-link" role="button" title="<fmt:message key="label.authoring.basic.delete" />"></i>
				</td>
			</tr>
		</c:forEach>
	</tbody>
</table>
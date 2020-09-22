<%@ include file="/common/taglibs.jsp"%>
<c:set var="method">
	<c:choose>
		<c:when test="${isIraAssessment}">
			iraAssessmentStudentChoices
		</c:when>
		<c:otherwise>
			aesStudentChoices
		</c:otherwise>
	</c:choose>
</c:set>
<% pageContext.setAttribute("newLineChar", "\r\n"); %>

<script>
	$(document).ready(function(){
		// change attempted and all learners numbers
		$('.nav-tabs').bind('click', function (event) {
			
			var link = $(event.target);
			var attemptedLearnersNumber = link.data("attempted-learners-number");
			$("#attempted-learners-number").html(attemptedLearnersNumber);

			var toolContentId = link.data("tool-content-id");
			$("#selected-content-id").val(toolContentId);
		});

		//insert total learners number taken from the parent tblmonitor.jsp
		$("#total-learners-number").html(TOTAL_LESSON_LEARNERS_NUMBER);

		// go through each AE and set up disclose buttons there
		$('.assessmentPane').each(function(){
			var assessmentPane = $(this),
				// are any correct/groups buttons clickable?
				discloseAllCorrectEnabled = false,
				discloseAllGroupsEnabled = false; 
			
			// disclose correct/group answers on click
			$('.disclose-button-group .btn', assessmentPane).not('[disabled]').each(function(){
				let button = $(this),
					isCorrectButton = button.hasClass('disclose-correct-button');
				if (isCorrectButton) {
					discloseAllCorrectEnabled = true;
				} else {
					discloseAllGroupsEnabled = true;
				}
				
				
				
				button.click(function(event) {	
					if (!confirm(isCorrectButton ? "<fmt:message key='message.disclose.correct.answers' />"
												: "<fmt:message key='message.disclose.groups.answers' />")) {
						return;
					}
					
					discloseAnswers(button);
				});
			});

			// if disclose all correct/groups answers buttons are clickable, add a click handler
			// and disable if not
			var allCorrectButton = $('.disclose-all-correct-button', assessmentPane);
			if (discloseAllCorrectEnabled) {
				allCorrectButton.click(function(){
					if (!confirm("<fmt:message key='message.disclose.all.correct.answers' />")) {
						return;
					}
					$('.disclose-correct-button', assessmentPane).not('[disabled]').each(function() {
						discloseAnswers($(this));
					});
					disabledAndCheckButton(allCorrectButton);
				});
			} else {
				disabledAndCheckButton(allCorrectButton);
			}

			var allGroupsButton = $('.disclose-all-groups-button', assessmentPane);
			if (discloseAllGroupsEnabled) {
				allGroupsButton.click(function(){
					if (!confirm("<fmt:message key='message.disclose.all.groups.answers' />")) {
						return;
					}
					$('.disclose-groups-button', assessmentPane).not('[disabled]').each(function() {
						discloseAnswers($(this));
					});
					disabledAndCheckButton(allGroupsButton);
				});
			} else {
				disabledAndCheckButton(allGroupsButton);
			}
		});
	});
	
	function discloseAnswers(button) {
		let isCorrectButton = button.hasClass('disclose-correct-button');
		
		$.ajax({
			'url'  : '<lams:WebAppURL />monitoring/' 
					  + (isCorrectButton ? 'discloseCorrectAnswers' : 'discloseGroupsAnswers')
					  + '.do',
			'type': 'POST',
			'data' : {
				'questionUid'   : button.closest('.disclose-button-group').attr('questionUid'),
				'toolContentID' : $('#selected-content-id').val(),
				'<csrf:tokenname/>' : '<csrf:tokenvalue/>'
			}
		}).done(function(){
			// disable the button after click
			disabledAndCheckButton(button);
		});
	}

	function disabledAndCheckButton(button){
		button.attr('disabled', true).html('<i class="fa fa-check text-success">&nbsp;</i>' + button.text());
	}
</script>

<!-- Header -->
<div class="row no-gutter">
	<div class="col-xs-12 col-md-12 col-lg-8">
		<h3>
			<c:choose>
				<c:when test="${isIraAssessment}">
					<fmt:message key="label.ira.questions.marks"/>
				</c:when>
				<c:otherwise>
					<fmt:message key="label.ae.questions.marks"/>
				</c:otherwise>
			</c:choose>
		</h3>
	</div>
</div>
<!-- End header -->    

<!-- Notifications -->  
<div class="row no-gutter">
	<div class="col-xs-6 col-md-4 col-lg-4 ">
		<div class="panel panel-default">
			<div class="panel-heading">
				<h4 class="panel-title">
					<i class="fa fa-users" style="color:gray" ></i> 
					<fmt:message key="label.attendance"/>: <span id="attempted-learners-number">${assessmentDtos[0].attemptedLearnersNumber}</span>/<span id="total-learners-number"></span> 
				</h4> 
			</div>
		</div>
	</div>

	<c:if test="${method eq 'aesStudentChoices' or iraAttemptedByAnyLearners}">
		<div class="col-xs-6 col-md-4 col-md-offset-4 col-lg-4 col-lg-offset-2">
			<a href="#nogo" type="button" class="btn btn-sm btn-default buttons_column"
					onclick="javascript:loadTab('${method}', document.getElementById('selected-content-id').value); return false;">
				<i class="fa fa-file"></i>
				<fmt:message key="label.show.students.choices"/>
			</a>
		</div>       
	</c:if>                           
</div>

<div class="row no-gutter">
	<input type="hidden" value="${assessmentDtos[0].assessment.contentId}" id="selected-content-id">
	
	<c:if test="${!isIraAssessment}">
		<ul class="nav nav-tabs">
			<c:forEach var="assessmentDto" items="${assessmentDtos}" varStatus="i">
				<li <c:if test="${(i.index == 0 && toolContentID == null) || (toolContentID != null && assessmentDto.assessment.contentId == toolContentID)}">class="active"</c:if>>
					<a data-toggle="tab" href="#assessment-${assessmentDto.assessment.uid}" 
							data-attempted-learners-number="${assessmentDto.attemptedLearnersNumber}"
							data-tool-content-id="${assessmentDto.assessment.contentId}">
						<c:out value="${assessmentDto.activityTitle}" escapeXml="false"/>
					</a>
				</li>
			</c:forEach>
		</ul>
	</c:if>
		
</div>
<br>
<!-- End notifications -->

<!-- Tables -->
<div class="tab-content">
	<c:forEach var="assessmentDto" items="${assessmentDtos}" varStatus="k">
		<div id="assessment-${assessmentDto.assessment.uid}" class="assessmentPane tab-pane 
			<c:if test="${(k.index == 0 && toolContentID == null) || (toolContentID != null && assessmentDto.assessment.contentId == toolContentID)}">active</c:if>">
			
			<c:if test="${assessmentDto.assessment.allowDiscloseAnswers}">
				<%-- Release correct/groups answers for all questions in this assessment --%>
				<div class="row no-gutter">
					<div class="col-xs-12 col-md-12 col-lg-12">
						<div class="btn-group-sm pull-right disclose-all-button-group">
							<div class="btn btn-default disclose-all-correct-button">
								<fmt:message key="label.disclose.all.correct.answers"/>
							</div>
							<div class="btn btn-default disclose-all-groups-button">
								<fmt:message key="label.disclose.all.groups.answers"/>
							</div>
						</div>
					</div>
				</div>
			</c:if>
			
			<c:forEach var="question" items="${assessmentDto.questions}" varStatus="i">
				<c:set var="qbQuestion" value="${question.qbQuestion}"/>
				<div class="row no-gutter">
				<div class="col-xs-12 col-md-12 col-lg-12">
				<div class="panel panel-default">
					<div class="panel-heading">
						<div class="panel-title">
							<c:if test="${assessmentDto.assessment.numbered}">
								<span class="float-left space-right">Q${i.index+1})</span>
							</c:if> 
							
							<c:out value="${qbQuestion.name}" escapeXml="false"/>
							
							<c:if test="${assessmentDto.assessment.allowDiscloseAnswers}">
								<div class="btn-group-xs pull-right disclose-button-group" questionUid="${question.uid}">
									<%-- Allow disclosing correct answers only for multiple choice questions --%>
									<c:if test="${qbQuestion.type == 1}">
										<div class="btn btn-default disclose-correct-button"
											<c:if test="${question.correctAnswersDisclosed}">
												disabled="disabled"><i class="fa fa-check text-success">&nbsp;</i
											</c:if>
											>
											<fmt:message key="label.disclose.correct.answers"/>
										</div>
									</c:if>
									<div class="btn btn-default disclose-groups-button"
										<c:if test="${question.groupsAnswersDisclosed}">
											disabled="disabled"><i class="fa fa-check text-success">&nbsp;</i
										</c:if>
										>
										<fmt:message key="label.disclose.groups.answers"/>
									</div>
								</div>
							</c:if>
						</div>
					</div>
					
					<div class="panel-body">
						<div class="table-responsive">	
							<c:out value="${qbQuestion.description}" escapeXml="false"/>
							
							<table class="table table-striped">
								<tbody>
									
									<c:choose>
										<c:when test="${qbQuestion.type == 1 || qbQuestion.type == 3}">
											<c:forEach var="qbOption" items="${qbQuestion.qbOptions}" varStatus="j">
												<tr>
													<td width="5px">
														${ALPHABET[j.index]}.
													</td>									
													<td>
														<c:choose>
															<c:when test="${qbQuestion.type == 1}">
																<c:out value="${qbOption.name}" escapeXml="false"/>
															</c:when>
															<c:otherwise>
																${fn:replace(qbOption.name, newLineChar, ', ')}
															</c:otherwise>
														</c:choose>
													</td>
												</tr>					
											</c:forEach>						
										</c:when>
										
										
										<c:when test="${qbQuestion.type == 2}">
											<tr style="background: none;">
												<td style="border-top: none;">
												</td>
																
												<td style="border-top: none; font-weight: bold;">
													Possible answers
												</td>
											</tr>
										
											<c:forEach var="qbOption" items="${qbQuestion.qbOptions}" varStatus="j">
												<tr>
													<td>
														<span class="pull-left">${ALPHABET[j.index]}.</span>
																
														<c:out value="${qbOption.matchingPair}" escapeXml="false"/>
													</td>
																
													<td>
														<c:out value="${qbOption.name}" escapeXml="false"/>
													</td>
												</tr>
											</c:forEach>
										</c:when>
										
										<c:when test="${(qbQuestion.type == 4) || (qbQuestion.type == 6)}">
										</c:when>
															
										<c:when test="${qbQuestion.type == 5}">
											<tr>
												<td width="5px">
													a.
												</td>
												<td>
													<fmt:message key="label.learning.true.false.true"/>
												</td>
											</tr>
											<tr>
												<td width="5px">
													b.
												</td>								
												<td>
													<fmt:message key="label.learning.true.false.false"/>
												</td>
											</tr>
										</c:when>
															
										<c:when test="${(qbQuestion.type == 7) || (qbQuestion.type == 8)}">
											<c:forEach var="qbOption" items="${qbQuestion.qbOptions}" varStatus="j">
															
												<tr>
													<td width="5px">
														${ALPHABET[j.index]}.
													</td>
													<td>
														<c:out value="${qbOption.name}" escapeXml="false"/>
													</td>
												</tr>
												
											</c:forEach>
										</c:when>						
										
									</c:choose>
								</tbody>
							</table>
						</div>
					</div>
				</div>  
				</div>
				</div>
			</c:forEach>
		</div>
	</c:forEach>
</div>
<!-- End tables -->

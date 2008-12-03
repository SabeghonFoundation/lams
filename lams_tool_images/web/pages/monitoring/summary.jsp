<%@ include file="/common/taglibs.jsp"%>
<c:set var="sessionMap" value="${sessionScope[sessionMapID]}"/>
<c:set var="summaryList" value="${sessionMap.summaryList}"/>

<script type="text/javascript" src="<lams:LAMSURL/>/includes/javascript/monitorToolSummaryAdvanced.js" ></script>
<script type="text/javascript">

	function resizeIframe() {
		if (document.getElementById('TB_iframeContent') != null) {
		    var height = top.window.innerHeight;
		    if ( height == undefined || height == 0 ) {
		    	// IE doesn't use window.innerHeight.
		    	height = document.documentElement.clientHeight;
		    	// alert("using clientHeight");
		    }
			// alert("doc height "+height);
		    height -= document.getElementById('TB_iframeContent').offsetTop + 100;
		    document.getElementById('TB_iframeContent').style.height = height +"px";
	
			TB_HEIGHT = height + 28;
			tb_position();
		}
	};
	window.onresize = resizeIframe;
</script>

<h1>
	<img src="<lams:LAMSURL/>/images/tree_closed.gif" id="treeIcon" onclick="javascript:toggleAdvancedOptionsVisibility(document.getElementById('advancedDiv'), document.getElementById('treeIcon'), '<lams:LAMSURL/>');" />

	<a href="javascript:toggleAdvancedOptionsVisibility(document.getElementById('advancedDiv'), document.getElementById('treeIcon'),'<lams:LAMSURL/>');" >
		<fmt:message key="monitor.summary.th.advancedSettings" />
	</a>
</h1>
<br />

<div class="monitoring-advanced" id="advancedDiv" style="display:none">
	<table class="alternative-color">
		<tr>
			<td>
				<fmt:message key="label.authoring.advance.lock.on.finished" />
			</td>
			
			<td>
				<c:choose>
					<c:when test="${sessionMap.imageGallery.lockWhenFinished == true}">
						<fmt:message key="label.on" />
					</c:when>
					<c:otherwise>
						<fmt:message key="label.off" />
					</c:otherwise>
				</c:choose>	
			</td>
		</tr>
		
		<tr>
			<td>
				<fmt:message key="label.authoring.advance.allow.learner.share.images" />
			</td>
			
			<td>
				<c:choose>
					<c:when test="${sessionMap.imageGallery.allowShareImages == true}">
						<fmt:message key="label.on" />
					</c:when>
					<c:otherwise>
						<fmt:message key="label.off" />
					</c:otherwise>
				</c:choose>	
			</td>
		</tr>
		
		<tr>
			<td>
				<fmt:message key="label.authoring.advance.allow.learner.comment.images" />
			</td>
			
			<td>
				<c:choose>
					<c:when test="${sessionMap.imageGallery.allowCommentImages == true}">
						<fmt:message key="label.on" />
					</c:when>
					<c:otherwise>
						<fmt:message key="label.off" />
					</c:otherwise>
				</c:choose>	
			</td>
		</tr>
		
		<tr>
			<td>
				<fmt:message key="label.authoring.advance.allow.learner.vote" />
			</td>
			
			<td>
				<c:choose>
					<c:when test="${sessionMap.imageGallery.allowVote == true}">
						<fmt:message key="label.on" />
					</c:when>
					<c:otherwise>
						<fmt:message key="label.off" />
					</c:otherwise>
				</c:choose>	
			</td>
		</tr>
		
		<tr>
			<td>
				<fmt:message key="label.authoring.advance.allow.learner.rank" />
			</td>
			
			<td>
				<c:choose>
					<c:when test="${sessionMap.imageGallery.allowRank == true}">
						<fmt:message key="label.on" />
					</c:when>
					<c:otherwise>
						<fmt:message key="label.off" />
					</c:otherwise>
				</c:choose>	
			</td>
		</tr>
		
		<tr>
			<td>
				<fmt:message key="monitor.summary.td.addNotebook" />
			</td>
			
			<td>
				<c:choose>
					<c:when test="${sessionMap.imageGallery.reflectOnActivity == true}">
						<fmt:message key="label.on" />
					</c:when>
					<c:otherwise>
						<fmt:message key="label.off" />
					</c:otherwise>
				</c:choose>	
			</td>
		</tr>
		
		<c:choose>
			<c:when test="${sessionMap.imageGallery.reflectOnActivity == true}">
				<tr>
					<td>
						<fmt:message key="monitor.summary.td.notebookInstructions" />
					</td>
					<td>
						${sessionMap.imageGallery.reflectInstructions}	
					</td>
				</tr>
			</c:when>
		</c:choose>
	</table>
</div>

<c:if test="${empty summaryList}">
	<div align="center">
		<b> <fmt:message key="message.monitoring.summary.no.session" /> </b>
	</div>
</c:if>

<c:forEach var="group" items="${summaryList}">

	<h1>
		<fmt:message key="monitoring.label.group" /> ${group[0].sessionName}	
	</h1>
	<h2 style="color:black; margin-left: 20px;">
		<fmt:message key="label.monitoring.summary.overall.summary" />	
	</h2>

	<table cellpadding="0" class="alternative-color">
		<tr>
			<th width="4%" align="center">
				<!--thumbnail-->
			</th>
			<th>
				<fmt:message key="monitoring.label.title" />
			</th>
			<th width="75px" >
				<!--hide/show-->
			</th>
		</tr>
		<%-- End group title display --%>	
	
		<c:forEach var="summary" items="${group}">
			<c:set var="sessionId" value="${summary.sessionId}" />		
			<c:set var="image" value="${summary.item}" />
			<c:if test="${summary.itemUid == -1}">
				<tr>
					<td colspan="5">
						<div class="align-left">
							<b> <fmt:message key="message.monitoring.summary.no.resource.for.group" /> </b>
						</div>
					</td>
				</tr>
			</c:if>
			<c:if test="${summary.itemUid != -1}">
				<tr>

					<td align="center">
						<c:set var="thumbnailPath">
						   	<html:rewrite page='/download/?uuid='/>${image.thumbnailFileUuid}&preferDownload=false
						</c:set>
						<c:set var="url" >
							<c:url value='/monitoring/imageSummary.do'/>?sessionMapID=${sessionMapID}&imageUid=${image.uid}&resizeIframe=true&TB_iframe=true&height=640&width=740
						</c:set>				
						<a href="${url}" class="thickbox" title="<fmt:message key='label.monitoring.imagesummary.image.summary' />" style="border-style: none;"> 
							<img src="${thumbnailPath}" alt="${image.title}" style="border-style: none;"/>
						</a>
					</td>
					
					<td style="vertical-align:middle;">
						<c:set var="title">
							${image.title}
							<c:if test="${!summary.itemCreateByAuthor}">
								[ <fmt:message key="label.monitoring.by"/> ${summary.username}]
							</c:if>						
						</c:set>
						<a href="${url}" class="thickbox">
							${title}
						</a>
					</td>
					
					<td style="vertical-align:middle; padding-left: 0px; text-align: center;">
						<c:choose>
							<c:when test="${summary.itemHide}">
								<a href="<c:url value='/monitoring/showitem.do'/>?sessionMapID=${sessionMapID}&imageUid=${summary.itemUid}" class="button"> <fmt:message key="monitoring.label.show" /> </a>
							</c:when>
							<c:otherwise>
								<a href="<c:url value='/monitoring/hideitem.do'/>?sessionMapID=${sessionMapID}&imageUid=${summary.itemUid}" class="button"> <fmt:message key="monitoring.label.hide" /> </a>
							</c:otherwise>
						</c:choose>
					</td>
				</tr>
			</c:if>
		</c:forEach>
	</table>			

	<%-- Reflection list  --%>
	<c:if test="${sessionMap.imageGallery.reflectOnActivity && not (empty sessionId)}">	
	
		<h2 style="color:black; margin-left: 20px; " >
			<fmt:message key="label.monitoring.summary.title.reflection"/>
		</h2>
	
		<table cellpadding="0" class="alternative-color">			
		
			<tr>
				<th>
					<fmt:message key="monitoring.user.fullname"/>
				</th>
				<th>
					<fmt:message key="monitoring.label.user.loginname"/>
				</th>
				<th>
					<fmt:message key="monitoring.user.reflection"/>
				</th>
			</tr>		
		
			<c:set var="userList" value="${sessionMap.reflectList[sessionId]}"/>
			<c:forEach var="user" items="${userList}" varStatus="refStatus">
				<tr>
					<td>
						${user.fullName}
					</td>
					<td>
						${user.loginName}
					</td>
					<td>
						<c:set var="viewReflection">
							<c:url value="/monitoring/viewReflection.do?toolSessionID=${sessionId}&userUid=${user.userUid}"/>
						</c:set>
						<html:link href="javascript:launchPopup('${viewReflection}')">
							<fmt:message key="label.view" />
						</html:link>
					</td>
				</tr>
			</c:forEach>
		</table>
	</c:if>

</c:forEach>

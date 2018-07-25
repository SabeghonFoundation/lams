<%@ include file="/common/taglibs.jsp"%>

<link rel="stylesheet" href="${lams}includes/css/lytebox.css"  type="text/css">

<script type="text/javascript"> 
	var pathToImageFolder = "${lams}includes/images/"; 
</script>
<script type="text/javascript" src="<lams:WebAppURL />includes/javascript/lytebox.js'/>" ></script>
<script lang="javascript">

	function hideMessage(){
		$("#new-image-input-area").hide();
		$("#saveCancelButtons").show();
	}
	
	function editItem(idx,sessionMapID){
		var url = "<c:url value="/authoring/editImage.do?imageIndex="/>" + idx +"&sessionMapID="+sessionMapID;;
		showMessage(url);
	}
	//The panel of imageGallery list panel
	var imageGalleryListTargetDiv = "imageGalleryListArea";
	function deleteItem(idx, sessionMapID) {

		var	deletionConfirmed = confirm("<fmt:message key="warning.msg.authoring.do.you.want.to.delete"></fmt:message>");
		
		if (deletionConfirmed) {
			deleteItemLoading();
			$("#" + imageGalleryListTargetDiv).load(
				"<c:url value="/authoring/removeImage.do"/>",
				{
					imageIndex: idx,
					reqID: (new Date()).getTime(), 
					sessionMapID: sessionMapID
				},
				function() {
					deleteItemComplete();
				}
			);
		}
	}
	
	function deleteItemLoading(){
		showBusy(imageGalleryListTargetDiv);
	}
	function deleteItemComplete(){
		hideBusy(imageGalleryListTargetDiv);
		initLytebox();
	}
	
	function upImage(idx, sessionMapID){
		deleteItemLoading();
		$("#" + imageGalleryListTargetDiv).load(
			"<c:url value="/authoring/upImage.do"/>",
			{
				imageIndex: idx,
				reqID: (new Date()).getTime(), 
				sessionMapID: sessionMapID
			},
			function() {
				deleteItemComplete();
			}
		);
	}
	function downImage(idx, sessionMapID){
		deleteItemLoading();
		$("#" + imageGalleryListTargetDiv).load(
			"<c:url value="/authoring/downImage.do"/>",
			{
				imageIndex: idx,
				reqID: (new Date()).getTime(), 
				sessionMapID: sessionMapID
			},
			function() {
				deleteItemComplete();
			}
		);
	}
</script>

<!-- Basic Tab Content -->
<div class="form-group">
    <label for="imageGallery.title">
    	<fmt:message key="label.authoring.basic.title"/>
    </label>
    <input type="text" path="imageGallery.title" class="form-control"/>
</div>

<div class="form-group">
    <label for="imageGallery.instructions">
    	<fmt:message key="label.authoring.basic.instruction"/>
    </label>
	<lams:CKEditor id="imageGallery.instructions" value="${imageGalleryForm.imageGallery.instructions}"
			contentFolderID="${imageGalleryForm.contentFolderID}">
	</lams:CKEditor>
</div>

<div id="imageGalleryListArea">
	<c:set var="sessionMapID" value="${imageGalleryForm.sessionMapID}" />
	<%@ include file="/pages/authoring/parts/itemlist.jsp"%>
</div>

<c:url var="showMessageURL" value='/authoring/newImageInit.do'>
	<c:param name="sessionMapID" value="${imageGalleryForm.sessionMapID}" />
</c:url>
<a href="#nogo" class="btn btn-default btn-sm" onclick="javascript:showMessage('${showMessageURL}')">
	<i class="fa fa-plus"></i>&nbsp;<fmt:message key="label.authoring.basic.add.image" />
</a>

<div id="new-image-input-area" class="voffset10"></div>

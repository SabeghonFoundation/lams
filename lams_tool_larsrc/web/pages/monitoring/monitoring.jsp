<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN"
        "http://www.w3.org/TR/html4/strict.dtd">

<%@ include file="/common/taglibs.jsp"%>

<html>
	<head>
		 <%@ include file="/common/header.jsp" %>
	 <script>
	    
	    	var imgRoot="${lams}images/";
		    var themeName="aqua";
	        
	        function init(){
	        
	            initTabSize(4);
	            
                selectTab(1); //select the default tab;
	        }     
	        
	        function doSelectTab(tabId) {
		    	// end optional tab controller stuff
		    	selectTab(tabId);
	        } 
	        
		    function viewItem(itemUid){
				var myUrl = "<c:url value="/reviewItem.do"/>?mode=teacher&itemUid=" + itemUid;
				launchPopup(myUrl,"MonitoringReview");
			}
	    </script>		 
	</head>
	<body onLoad="init()">
	<div id="page">
	<div id="header">
		<lams:Tabs>
			<lams:Tab id="1" key="monitoring.tab.summary" />
			<lams:Tab id="2" key="monitoring.tab.instructions" />
			<lams:Tab id="3" key="monitoring.tab.edit.activity" />			
			<lams:Tab id="4" key="monitoring.tab.statistics" />
		</lams:Tabs>
	</div>
	<div id="content">

		<div class="tabbody">
			<lams:TabBody id="1" titleKey="monitoring.tab.summary" page="summary.jsp" />
			<lams:TabBody id="2" titleKey="monitoring.tab.instructions" page="instructions.jsp"/>
			<lams:TabBody id="3" titleKey="monitoring.tab.edit.activity" page="editactivity.jsp" />			
			<lams:TabBody id="4" titleKey="monitoring.tab.statistics" page="statistic.jsp" />
		</div>
		</div>
		<div id="footer"></div>
		
	</div>
	</body>
</html>

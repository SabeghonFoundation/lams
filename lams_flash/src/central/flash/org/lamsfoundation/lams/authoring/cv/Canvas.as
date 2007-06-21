﻿/***************************************************************************
 * Copyright (C) 2005 LAMS Foundation (http://lamsfoundation.org)
 * =============================================================
 * License Information: http://lamsfoundation.org/licensing/lams/2.0/
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2.0 
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 * USA
 * 
 * http://www.gnu.org/licenses/gpl.txt
 * ************************************************************************
 */

import org.lamsfoundation.lams.authoring.cv.*
import org.lamsfoundation.lams.authoring.br.*
import org.lamsfoundation.lams.authoring.tk.*
import org.lamsfoundation.lams.common.util.*
import org.lamsfoundation.lams.common.comms.*
import org.lamsfoundation.lams.authoring.*
import org.lamsfoundation.lams.common.ui.*
import org.lamsfoundation.lams.common.dict.*
import org.lamsfoundation.lams.common.style.*
import org.lamsfoundation.lams.common.ws.Workspace
import org.lamsfoundation.lams.common.ApplicationParent
import org.lamsfoundation.lams.common.* 
import mx.managers.*
import mx.utils.*
import mx.transitions.Tween;
import mx.transitions.easing.*;

/**
 * The canvas is the main screen area of the LAMS application where activies are added and sequenced
 * Note - This holds the DesignDataModel _ddm 
 * @version 1.0
 * @since   
 */
class org.lamsfoundation.lams.authoring.cv.Canvas {
	
	//Model
	private var canvasModel:CanvasModel;

	//Views
	private var canvasView:CanvasView;
	private var canvasBranchView:CanvasBranchView;
	private var _canvasView_mc:MovieClip;
	private var _canvasBranchView_mc:MovieClip;
	
	// CookieMonster (SharedObjects)
    private var _cm:CookieMonster;
    private var _comms:Communication;
	
	private var app:Application;
	private var _ddm:DesignDataModel;
	private var _dictionary:Dictionary;
	private var _config:Config;
	private var doc;
	private var _newToolContentID:Number;
	private var _newChildToolContentID:Number;
	private var _undoStack:Array;	
	private var _redoStack:Array;
	private var toolActWidth:Number = 123;
	private var toolActHeight:Number = 50;
	private var complexActWidth:Number = 143;
	private var _isBusy:Boolean;
	private static var AUTOSAVE_CONFIG:String = "autosave";
	private static var AUTOSAVE_TAG:String = "cv.ddm.autosave.user.";
    private var _bin:MovieClip;	//bin
	
	private var _target_mc:MovieClip;
	
    //Defined so compiler can 'see' events added at runtime by EventDispatcher
    private var dispatchEvent:Function;     
    public var addEventListener:Function;
    public var removeEventListener:Function;

	/**
	* Canvas Constructor
	*
	* @param   target_mc	Target clip for attaching view
	*/
	public function Canvas (target_mc:MovieClip,depth:Number,x:Number,y:Number,w:Number,h:Number){
        mx.events.EventDispatcher.initialize(this);
        
		_target_mc = target_mc;
		
		//Design Data Model.
		_ddm = new DesignDataModel();
		
		//Create the model.
		//pass in a ref to this container
		canvasModel = new CanvasModel(this);
		
		_dictionary = Dictionary.getInstance();
		
		//Create the view
		_canvasView_mc = _target_mc.createChildAtDepth("canvasView",DepthManager.kTop);		

        //Cast toolkit view clip as ToolkitView and initialise passing in model
		canvasView = CanvasView(_canvasView_mc);
		canvasView.init(canvasModel,undefined,x,y,w,h);
        
        
        //Get reference to application and design data model
		app = Application.getInstance();
		
		
        //Get a ref to the cookie monster 
        _cm = CookieMonster.getInstance();
        _comms = ApplicationParent.getInstance().getComms();
		
		_undoStack = new Array();
		_redoStack = new Array();
		_isBusy = false;
		//some initialisation:


		//Add listener to view so that we know when it's loaded
        canvasView.addEventListener('load',Proxy.create(this,viewLoaded));
        
		_ddm.addEventListener('ddmUpdate',Proxy.create(this,onDDMUpdated));
		_ddm.addEventListener('ddmBeforeUpdate',Proxy.create(this,onDDMBeforeUpdate));
		
        //Register view with model to receive update events
		canvasModel.addObserver(canvasView);

        //Set the position by setting the model which will call update on the view
        canvasModel.setPosition(x,y);
        //Initialise size to the designed size
        canvasModel.setSize(w,h);
		
		//if in monitor, dont do it!
		initBin();
		
	}

    /**
    * Event dispatched from the view once it's loaded
    */
    public function viewLoaded(evt:Object) {
        if(evt.type=='load') {
			
			canvasModel.activeView = evt.target;
			if(evt.target instanceof CanvasBranchView) {
				evt.target.open();
				
				canvasModel.setDirty();
			} else {
		
				var autosave_config_interval = Config.getInstance().getItem(AUTOSAVE_CONFIG);
				if(autosave_config_interval > 0) {
					if(CookieMonster.cookieExists(AUTOSAVE_TAG + _root.userID)) {
						canvasModel.autoSaveWait = true;
					}
					setInterval(Proxy.create(this,autoSave), autosave_config_interval);
				}
				
				clearCanvas(true);
				
				dispatchEvent({type:'load',target:this});
			}
			
		} else {
            Debugger.log('Event type not recognised : ' + evt.type,Debugger.CRITICAL,'viewLoaded','Canvas');
        }
    }
	
	/**
    * Opens the help->about dialog
    */
    public function openAboutLams() {
		
		var controller:CanvasController = canvasView.getController();
		
		var dialog:MovieClip = PopUpManager.createPopUp(Application.root, LFWindow, true,{title:Dictionary.getValue('about_popup_title_lbl', [Dictionary.getValue('stream_reference_lbl')]),closeButton:true,scrollContentPath:'AboutLams'});
		dialog.addEventListener('contentLoaded',Delegate.create(controller, controller.openDialogLoaded));
		
	}
	
	/**
	* Opens a design using workspace and user to select design ID
	* passes the callback function to recieve selected ID
	*/
	public function openDesignBySelection(){
        //Work space opens dialog and user will select view
		if(_ddm.modified){
			LFMessage.showMessageConfirm(Dictionary.getValue('cv_design_unsaved'), Proxy.create(this,doOpenDesignBySelection), null);
		} else {
			doOpenDesignBySelection();
		}
	}
    
	public function doOpenDesignBySelection():Void{
		var callback:Function = Proxy.create(this, openDesignById);
		var ws = Application.getInstance().getWorkspace();
        ws.userSelectItem(callback);
	}
	
	/**
	 * Request design from server using supplied ID.
	 * @usage   
	 * @param   designId 
	 * @return  
	 */
    public function openDesignById(workspaceResultDTO:Object){
		Application.getInstance().getWorkspace().getWV().clearDialog();
		ObjectUtils.toString(workspaceResultDTO);
		var designId:Number = workspaceResultDTO.selectedResourceID;

        var callback:Function = Proxy.create(this,setDesign);
		Application.getInstance().getComms().getRequest('authoring/author.do?method=getLearningDesignDetails&learningDesignID='+designId,callback, false);
	
    }
	
	/**
	 * Request imported design from server
	 * 
	 * @usage   
b	 * @param   learningDesignID 
	 * @return  
	 */
	
	public function openDesignByImport(learningDesignID:Number){
		var callback:Function = Proxy.create(this,setDesign, true);
        canvasModel.importing = true;
		Application.getInstance().getComms().getRequest('authoring/author.do?method=getLearningDesignDetails&learningDesignID='+learningDesignID,callback, false);
		
	}
	
	/**
	 * Request runtime-sequence design from server to be editted.
	 * 
	 * @usage   
	 * @param   learningDesignID 
	 * @return  
	 */
	
	public function openDesignForEditOnFly(learningDesignID:Number){
		var callback:Function = Proxy.create(this,setDesign, true);
        canvasModel.editing = true;
		
		Application.getInstance().getComms().getRequest('authoring/author.do?method=getLearningDesignDetails&learningDesignID='+learningDesignID,callback, false);
		
	}
	
	public function openBranchView(ba){
		
		var cx:Number = ba._x + ba.getVisibleWidth()/2;
		var cy:Number = ba._y + ba.getVisibleHeight()/2;
		
		var _branchView_mc:MovieClip = _canvasView_mc.content.createChildAtDepth("canvasBranchView", DepthManager.kTop, {_x: cx, _y: cy, _canvasBranchingActivity:ba});	
		var branchView:CanvasBranchView = CanvasBranchView(_branchView_mc);
		branchView.init(canvasModel,undefined);
		
		//Add listener to view so that we know when it's loaded
        branchView.addEventListener('load', Proxy.create(this,viewLoaded));
		
		canvasModel.addObserver(branchView);
		
		ba.branchView = branchView;
		
	}
	
	public function closeBranchView() {
		canvasModel.activeView = canvasView;
		canvasModel.currentBranchingActivity = null;
	}
	
	/** deprecated */
	private function fadeOtherOnCanvas(ba) {
		
		var k:Array = canvasModel.activitiesDisplayed.values();
		for (var i=0; i<k.length; i++){
			if(k[i] != ba) {
			var tweenObj:Object = new Tween(k[i], "_alpha", Strong.easeIn, 100, 20, 0.7, true);
			
			}
		}
		
	}
	
	/**
	 * Auto-saves current DDM (Learning Design on Canvas) to SharedObject
	 * 
	 * @usage   
	 * @return  
	 */
	
	private function autoSave(){
		if(!canvasModel.autoSaveWait && (canvasModel.activitiesDisplayed.size() > 0)) {
			if(!ddm.readOnly) {
				var tag:String = AUTOSAVE_TAG + _root.userID;
				
				var dto:Object = _ddm.getDesignForSaving();
				dto.lastModifiedDateTime = new Date();
				dto.readOnly = true;
				
				// remove existing auto-saved ddm
				if (CookieMonster.cookieExists(tag)) {
					CookieMonster.deleteCookie(tag);
				}
				
				// auto-save existing ddm
				var res = CookieMonster.save(dto,tag,true);
				
				if(!res){
					// error auto-saving
					var msg:String = Dictionary.getValue('cv_autosave_err_msg');
					LFMessage.showMessageAlert(msg);
				}
			}
		} else if(canvasModel.autoSaveWait) {
			discardAutoSaveDesign();
		}
		
	}
	
	/**
	 * Show auto-save confirmation message
	 * 
	 * @usage   
	 * @return  
	 */

	public function showRecoverMessage() {
		var recData:Object = CookieMonster.open(AUTOSAVE_TAG + _root.userID,true);
		
		LFMessage.showMessageConfirm(Dictionary.getValue('cv_autosave_rec_msg'), Proxy.create(this, recoverDesign, recData), Proxy.create(this, discardAutoSaveDesign), null, null, Dictionary.getValue('cv_autosave_rec_title'));
	}
	
	
	/**
	 * Recover design data from SharedObject and save.
	 * 
	 * @usage   
	 * @return  
	 */
	
	public function recoverDesign(recData:Object) {
		setDesign(recData);
		discardAutoSaveDesign();
	}
	
	private function discardAutoSaveDesign() {
		canvasModel.autoSaveWait = false;
		CookieMonster.deleteCookie(AUTOSAVE_TAG + _root.userID);
		LFMenuBar.getInstance().enableRecover(false);
	}
	
	public function saveDesign(){
		if((_ddm.learningDesignID == undefined || _ddm.learningDesignID == "" || _ddm.learningDesignID == null || _ddm.learningDesignID =="undefined") || _ddm.learningDesignID == Config.NUMERIC_NULL_VALUE && (_ddm.title == "" || _ddm.title == undefined || _ddm.title == null)){
			
			// raise alert if design is empty
			if (canvasModel.activitiesDisplayed.size() < 1){
				Cursor.showCursor(Application.C_DEFAULT);
				var msg:String = Dictionary.getValue('al_empty_design');
				LFMessage.showMessageAlert(msg);
			}else {
				saveDesignToServerAs(Workspace.MODE_SAVE);
			}
		
		}else if(_ddm.readOnly && !_ddm.editOverrideLock){
			saveDesignToServerAs(Workspace.MODE_SAVEAS);
		}else if(_ddm.editOverrideLock){
			var errors:Array = canvasModel.validateDesign();
			
			if(errors.length > 0) {
				var errorPacket = new Object();
				errorPacket.messages = errors;
				
				var msg:String = Dictionary.getValue('cv_invalid_design_on_apply_changes');
				var okHandler = Proxy.create(this,showDesignValidationIssues, errorPacket);
				LFMessage.showMessageConfirm(msg,okHandler,null,Dictionary.getValue('cv_show_validation'));
				Cursor.showCursor(Application.C_DEFAULT);
			} else {
				saveDesignToServer();	// design is valid, save normal
			}
		
		}else{
			saveDesignToServer();
		}
	}
 
	/**
	 * Launch workspace browser dialog and set the design metat data for saving
	 * E.g. Title, Desc, Folder etc... also license if required?
	 * @usage   
	 * @param	tabToShow	The tab to be selected when the dialogue opens.
	 * @param 	mode		save mode
	 * @return  
	 */
	public function saveDesignToServerAs(mode:String){
		// if design as not been previously saved then we should use SAVE mode
		if(_ddm.learningDesignID == null) { mode = Workspace.MODE_SAVE }
		else { 
			//hold exisiting learningDesignID value in model (backup)
			_ddm.prevLearningDesignID = _ddm.learningDesignID;
			
			//clear the learningDesignID so it will not overwrite the existing one
			_ddm.learningDesignID = null;
		}
		
		
        var onOkCallback:Function = Proxy.create(this, saveDesignToServer);
		var ws = Application.getInstance().getWorkspace();
        ws.setDesignProperties("LOCATION", mode, onOkCallback);
		

	}
	/**
	 * Updates the design with the detsils form the workspace :
	 * 	* <code>
	*	_resultDTO.selectedResourceID 	//The ID of the resource that was selected when the dialogue closed
	*	_resultDTO.resourceName 		//The contents of the Name text field
	*	_resultDTO.resourceDescription 	//The contents of the description field on the propertirs tab
	*	_resultDTO.resourceLicenseText 	//The contents of the license text field
	*	_resultDTO.resourceLicenseID 	//The ID of the selected license from the drop down.
    *</code>
	* And then saves the design to the sever by posting XML via comms class
	 * @usage   
	 * @return  
	 */
	public function saveDesignToServer(workspaceResultDTO:Object):Boolean{
		_global.breakpoint();

		//TODO: Set the results from wsp into design.
		if(workspaceResultDTO != null){
			if(workspaceResultDTO.selectedResourceID != null){
				//must be overwriting an existing design as we have a new resourceID
				_ddm.learningDesignID = workspaceResultDTO.selectedResourceID;
			}
			_ddm.workspaceFolderID = workspaceResultDTO.targetWorkspaceFolderID;
			_ddm.title = workspaceResultDTO.resourceName;
			_ddm.description = workspaceResultDTO.resourceDescription;
			_ddm.licenseText = workspaceResultDTO.resourceLicenseText;
			_ddm.licenseID = workspaceResultDTO.resourceLicenseID;
		}
		var mode:String = Application.getInstance().getWorkspace().getWorkspaceModel().currentMode;
		
		_ddm.saveMode = (mode == Workspace.MODE_SAVEAS) ? 1 : 0;
		
		Debugger.log('SAVE MODE:'+_ddm.saveMode,Debugger.CRITICAL,'saveDesignToServer','Canvas');
		
		
		var dto:Object = _ddm.getDesignForSaving();
		
		var callback:Function = Proxy.create(this,onStoreDesignResponse);
		
		Application.getInstance().getComms().sendAndReceive(dto,"servlet/authoring/storeLearningDesignDetails",callback,false);
		
		return true;
	}
	
	/**
	 * now contains a validation response packet
	 * Displays to the user the results of the response.
	 * @usage   
	 * @param   r //the validation response
	 * @return  
	 */
	public function onStoreDesignResponse(r):Void{
		Application.getInstance().getWorkspace().getWV().clearDialog();
		
		if(r instanceof LFError){
			// reset old learning design ID if failed completing a save-as operation
			if(_ddm.prevLearningDesignID != null && _ddm.saveMode == 1) {
				_ddm.prevLearningDesignID = null;
			}
			
			Cursor.showCursor(Application.C_DEFAULT);
			r.showErrorAlert();
		}else{
			discardAutoSaveDesign();

			_ddm.learningDesignID = r.learningDesignID;
			_ddm.validDesign = r.valid;
			
			if(_ddm.saveMode == 1){
				Debugger.log('save mode: ' +_ddm.saveMode,Debugger.GEN,'onStoreDesignResponse','Canvas');		
				Debugger.log('updating activities.... ',Debugger.GEN,'onStoreDesignResponse','Canvas');		
			
				updateToolActivities(r);
				
				_ddm.readOnly = false;
				_ddm.copyTypeID = DesignDataModel.COPY_TYPE_ID_AUTHORING;
			
			} else {
				Debugger.log('save mode: ' +_ddm.saveMode,Debugger.GEN,'onStoreDesignResponse','Canvas');		
			
			}
			
			_ddm.modified = false;
			
			ApplicationParent.extCall("setSaved", "true");
			
			LFMenuBar.getInstance().enableExport(true);
			Debugger.log('_ddm.learningDesignID:'+_ddm.learningDesignID,Debugger.GEN,'onStoreDesignResponse','Canvas');		
			
			
			if(_ddm.validDesign){
				var msg:String = Dictionary.getValue('cv_valid_design_saved');
				var _requestSrc = _root.requestSrc;
				if(_requestSrc != null) {
					//show the window, on load, populate it
					var cc:CanvasController = canvasView.getController();
					var saveConfirmDialog = PopUpManager.createPopUp(Application.root, LFWindow, true,{title:Dictionary.getValue('al_alert'),closeButton:false,scrollContentPath:"SaveConfirmDialog",msg:msg, requestSrc:_requestSrc, canvasModel:canvasModel,canvasController:cc});
	
				} else if(_ddm.editOverrideLock) {
					var finishEditHandler = Proxy.create(this,finishEditOnFly);
					msg = Dictionary.getValue('cv_eof_changes_applied');
					LFMessage.showMessageAlert(msg, finishEditHandler);
				} else {
					LFMessage.showMessageAlert(msg);
				}
			} else {
				var msg:String = Dictionary.getValue('cv_invalid_design_saved');
				var okHandler = Proxy.create(this,showDesignValidationIssues,r);
				LFMessage.showMessageConfirm(msg,okHandler,null,Dictionary.getValue('cv_show_validation'));
			}
			
			checkValidDesign();
			checkReadOnlyDesign();
			Cursor.showCursor(Application.C_DEFAULT);
		}
	}
	
	public function showDesignValidationIssues(responsePacket){
		Debugger.log(responsePacket.messages.length+' issues',Debugger.GEN,'showDesignValidationIssues','Canvas');
		var dp = new Array();
		for(var i=0; i<responsePacket.messages.length;i++){
			var dpElement = {};
			dpElement.Issue = responsePacket.messages[i].message;
			dpElement.Activity =  _ddm.getActivityByUIID(responsePacket.messages[i].UIID).title;
			dpElement.uiid = responsePacket.messages[i].UIID;
			dp.push(dpElement);
		}
		//show the window, on load, populate it
		var cc:CanvasController = canvasView.getController();
		var validationIssuesDialog = PopUpManager.createPopUp(Application.root, LFWindow, false,{title:Dictionary.getValue('ld_val_title'),closeButton:true,scrollContentPath:"ValidationIssuesDialog",validationIssues:dp, canvasModel:canvasModel,canvasController:cc});
	}
	
	/**
	 * Close Window
	 * 
	 * @usage   
	 * @return  
	 */
	
	public function closeReturnExt() {
		ApplicationParent.extCall("closeWindow", null);
	}
	
	/**
	 * Reopen Monitor client
	 * 
	 * @usage   
	 * @param   lessonID 	Lesson to load in Monitor
	 * @return  
	 */
	
	public function reopenMonitor(lessonID) {
		Debugger.log('finishing and closing Edit On The Fly',Debugger.CRITICAL,'finishEditOnFly','Canvas');
		
		ApplicationParent.extCall("openMonitorLesson", lessonID);
	}
	
	/**
	 * Finish Edit-On-The-Fly
	 * 
	 * @usage   
	 * @param   forced 
	 * @return  
	 */
	
	public function finishEditOnFly(forced:Boolean) {
		Debugger.log('finishing and closing Edit On The Fly',Debugger.CRITICAL,'finishEditOnFly','Canvas');
		Debugger.log('valid design: ' + _ddm.validDesign,Debugger.CRITICAL,'finishEditOnFly','Canvas');
		Debugger.log('modified: ' + _ddm.modified,Debugger.CRITICAL,'finishEditOnFly','Canvas');
		
		var callback:Function = Proxy.create(this,reopenMonitor);
        canvasModel.editing = false;
		
		if(forced) {
			ApplicationParent.extCall("setSaved", "true");
			finishLearningDesignCall(callback);
			return;
		}
		
		if(!_ddm.modified) {
			if(_ddm.validDesign) finishLearningDesignCall(callback);
			else LFMessage.showMessageAlert(Dictionary.getValue("cv_eof_finish_invalid_msg"));
		} else LFMessage.showMessageConfirm(Dictionary.getValue("cv_eof_finish_modified_msg"), Proxy.create(this,finishEditOnFly, true), null);
	}
	
	private function finishLearningDesignCall(callback:Function) {
		Application.getInstance().getComms().getRequest('authoring/author.do?method=finishLearningDesignEdit&learningDesignID='+_ddm.learningDesignID,callback, false);
	}
	
	/**
	 * 
	 * 
	 * @usage   
	 * @param   acts 
	 * @return  
	 */
	
	public function updateToolActivities(responsePacket){
		Debugger.log(responsePacket.activities.length+' activities to be updated...',Debugger.GEN,'updateToolActivities','Canvas');
		for(var i=0; i<responsePacket.activities.length; i++){
			var ta:ToolActivity = ToolActivity(_ddm.getActivityByUIID(responsePacket.activities[i].activityUIID));
			ta.toolContentID = responsePacket.activities[i].toolContentID;
			ta.readOnly = responsePacket.activities[i].readOnly;
			Debugger.log('setting new tool content ID for activity ' + ta.activityID + ' (toolContentID:' + ta.toolContentID + ')',Debugger.GEN,'updateToolActivities','Canvas');
		
		}
		
		canvasModel.setDirty();
	}
	
	public function checkValidDesign(){
		if(_ddm.validDesign){
			Application.getInstance().getToolbar().setButtonState('preview',true);
			LFMenuBar.getInstance().enableExport(true);
		}else{
			Application.getInstance().getToolbar().setButtonState('preview',false);
			LFMenuBar.getInstance().enableExport(false);
		}
		
	}
	
	public function checkReadOnlyDesign(){
		if(_ddm.readOnly){
			if(!_ddm.editOverrideLock) {
				LFMenuBar.getInstance().enableSave(false);
				canvasView.showReadOnly(true);
			} else {
				canvasView.showEditOnFly(true);
			}
			
		} else {
			LFMenuBar.getInstance().enableSave(true);
			canvasView.showReadOnly(false);
		}
		canvasModel.setDesignTitle();
	}
	
	/**
	 * Called when a template activity is dropped onto the canvas
	 * @usage   
	 * @param   ta TemplateActivity
	 * @return  
	 */
	public function setDroppedTemplateActivity(ta:TemplateActivity, taParent:Number):Void{
		
		var actToCopy:Activity = ta.mainActivity;
		//loosly typed this var as it might be any type of activity
		var actToAdd:Activity;
		var actType:String;
		Debugger.log('actToCopy.activityTypeID:'+actToCopy.activityTypeID,Debugger.GEN,'setDroppedTemplateActivity','Canvas');			
		
		switch(actToCopy.activityTypeID){
			
			case(Activity.TOOL_ACTIVITY_TYPE):
				actType = "Tool"
				 actToAdd = ToolActivity(actToCopy.clone());
				//give it a new UIID:
				actToAdd.activityUIID = _ddm.newUIID();
			break;
			case(Activity.OPTIONAL_ACTIVITY_TYPE):
				actToAdd = Activity(actToCopy.clone());
				//give it a new UIID:
				actToAdd.activityUIID = _ddm.newUIID();
			
			case(Activity.PARALLEL_ACTIVITY_TYPE):
				actType = "Parallel"
				actToAdd = Activity(actToCopy.clone());
				
				//give it a new UIID:
				actToAdd.activityUIID = _ddm.newUIID();
				
				
			Debugger.log('parallel activity given new UIID of:'+actToAdd.activityUIID ,Debugger.GEN,'setDroppedTemplateActivity','Canvas');			
			//now get this acts children and add them to the design (WHINEY VOICE:"will somebody pleeeease think of the children.....")
			for(var i=0;i<ta.childActivities.length;i++){
					
					//Note: The next few line os code is now execute in the setNewChildContentID method
					//Find out if other types of activity can be held by complex acts 
					
					var child:Activity = ToolActivity(ta.childActivities[i].clone());
					child.activityUIID = _ddm.newUIID();
					//tell it who's the daddy (set its parent UIID)
					child.parentUIID = actToAdd.activityUIID;
					Debugger.log('child.parentUIID:'+child.parentUIID,Debugger.GEN,'setDroppedTemplateActivity','Canvas');			
					child.learningDesignID = _ddm.learningDesignID;
					//does not need mouse co-ords as in in container act.
					
					_ddm.addActivity(child);
					var callback:Function = Proxy.create(this,setNewChildContentID, child);
					var passChildToolID = ta.childActivities[i].toolID;
					Application.getInstance().getComms().getRequest('authoring/author.do?method=getToolContentID&toolID='+passChildToolID,callback, false);
			}				 
			break;
			
			
			default:
				new LFError("NOT ready to handle activity this Activivty type","Canvas.setDroppedTemplateActivity",this,ObjectUtils.printObject(ta));
				
		}
		
		//Set up the main activity for the canvas:
		
		
		//assign it the LearningDesignID
		actToAdd.learningDesignID = _ddm.learningDesignID;
		
		//give it the mouse co-ords
		if (actType = "Parallel"){
			actToAdd.xCoord = canvasModel.activeView.content._xmouse - (complexActWidth/2);
			actToAdd.yCoord = canvasModel.activeView.content._ymouse;
		}
		if(actType = "Tool"){
			actToAdd.xCoord = canvasModel.activeView.content._xmouse - (toolActWidth/2);
			actToAdd.yCoord = canvasModel.activeView.content._ymouse - (toolActHeight/2);
		}
				
		Debugger.log('actToAdd:'+actToAdd.title+':'+actToAdd.activityUIID + ":seq" + canvasModel.activeView.defaultSequenceActivity,4,'setDroppedTemplateActivity','Canvas');		
		
		if(canvasModel.activeView.defaultSequenceActivity != null) {
			actToAdd.parentUIID = canvasModel.activeView.defaultSequenceActivity.activityUIID;
		}

		_ddm.addActivity(actToAdd);
		
		//refresh the design
		canvasModel.setDirty();
		canvasModel.selectedItem = (canvasModel.activitiesDisplayed.get(actToAdd.activityUIID));
		
		//select the new thing
		if (taParent != undefined || taParent != null){
			actToAdd.parentUIID = taParent;
			canvasModel.removeActivity(actToAdd.activityUIID);
			canvasModel.removeActivity(taParent);
		}
		
		canvasModel.setDirty();
		
	}
	
	private function setNewChildContentID(r, ta:ToolActivity){
		if(r instanceof LFError){
			r.showMessageConfirm();
		}else{
			_newChildToolContentID = r;
			ta.toolContentID = _newChildToolContentID;
		}
		
	}

	/**
	 * Removes an activity from Design Data Model using its activityUIID.  
	 * Called by the bin
	 * @usage   
	 * @param   activityUIID 
	 * @return  
	 */
	public function removeActivity(activityUIID:Number){
		Debugger.log('activityUIID:'+activityUIID,4,'removeActivity','Canvas');
		
		// remove transitions connected to this activity being removed
		_ddm.removeTransitionByConnection(activityUIID);
		_ddm.removeActivity(activityUIID);
		canvasModel.setDirty();
		canvasModel.selectedItem = null;
	}
	
	/**
	 * Removes an transition by using its transitionUIID.  
	 * Called by the bin
	 * @usage   
	 * @param   transitionUIID 
	 * @return  
	 */
	public function removeTransition(transitionUIID:Number){	
		_ddm.removeTransition(transitionUIID);
		canvasModel.setDirty();	
		canvasModel.selectedItem = null;
	}
	
	
	
	/**
	 * Called by Comms after a design has been loaded, usually set as the call back of something like openDesignByID.
	 * Will accept a learningDesign DTO and then render it all out.
	 * @usage   
	 * @param   designData 
	 * @return  
	 */
    public function setDesign(designData:Object){
       
		Debugger.log('designData.title:'+designData.title+':'+designData.learningDesignID,4,'setDesign','Canvas');
		
		if(clearCanvas(true)){
			
			_ddm.setDesign(designData);
			
			if(canvasModel.importing){ 
				Application.getInstance().getWorkspace().getWorkspaceModel().clearWorkspaceCache(_ddm.workspaceFolderID);
				canvasModel.importing = false;
			} else if(canvasModel.editing){
				// TODO: stuff to do before design is displayed
				// do we need editing flag in CanvasModel?
			}
			
			checkValidDesign();
			checkReadOnlyDesign();
			canvasModel.setDesignTitle();
			canvasModel.setDirty();
			LFMenuBar.getInstance().enableExport(!canvasModel.autoSaveWait);
		
		}else{
			Debugger.log('Set design failed as old design could not be cleared',Debugger.CRITICAL,"setDesign",'Canvas');		
		}
    }
	
	/**
	 * Clears the design in the canvas.but leaves other state variables (undo etc..)
	 * @usage   
	 * @param   noWarn 
	 * @return  
	 */
	public function clearCanvas(noWarn:Boolean):Boolean{
		var s = false;
		var ref = this;
		Debugger.log('noWarn:'+noWarn,4,'clearCanvas','Canvas');
		if(noWarn){
			_ddm = new DesignDataModel();
			
			//as its a new instance of the ddm,need to add the listener again
			_ddm.addEventListener('ddmUpdate',Proxy.create(this,onDDMUpdated));
			_ddm.addEventListener('ddmBeforeUpdate',Proxy.create(this,onDDMBeforeUpdate));
			checkValidDesign();
			checkReadOnlyDesign();
			
			if(canvasModel.activeView instanceof CanvasBranchView) {
				canvasModel.activeView.removeMovieClip();
				closeBranchView();
			}
			
			canvasModel.setDirty();
			
			createContentFolder();
			
			return true;
		}else{
			var fn:Function = Proxy.create(ref,confirmedClearDesign, ref);
			LFMessage.showMessageConfirm(Dictionary.getValue('new_confirm_msg'), fn,null);
			Debugger.log('Set design failed as old design could not be cleared',Debugger.CRITICAL,"setDesign",'Canvas');		
		}
		
		
		
		
	}
	
	/**
	 * Revert canvas back to last saved design
	 * 
	 * @usage   
	 * @return  
	 */
	
	public function revertCanvas():Boolean {
		// can only revert canvas if design is saved
		if(_ddm.learningDesignID != null) {
			
			openDesignForEditOnFly(_ddm.learningDesignID);
			
		} else {
			// throw error alert
			return false;
		}
	}
	
	
	/**
	 * Returns canvas to init state, ready for new design
	 * @usage   
	 * @param   noWarn 
	 * @return  
	 */
	public function resetCanvas(noWarn:Boolean):Boolean{
		_undoStack = new Array();
		_redoStack = new Array();
		
		var r = clearCanvas(noWarn);
		
		return r;
		
	}
	
	/**
	 * Called when a user confirms its ok to clear the design
	 * @usage   
	 * @param   ref 
	 * @return  
	 */
	public function confirmedClearDesign(ref):Void{
		var fn:Function = Proxy.create(ref,clearCanvas,true);
		fn.apply();
	}
	
	/**
	 * Called when the user initiates a paste.  recieves a reference to the item to be copied
	 * @usage   
	 * @param   o Item to be copied
	 * @return  
	 */
	public function setPastedItem(o:Object){
		if (o.data instanceof CanvasActivity){
			Debugger.log('instance is CA',Debugger.GEN,'setPastedItem','Canvas');
			var callback:Function = Proxy.create(this,setNewContentID, o);
			Application.getInstance().getComms().getRequest('authoring/author.do?method=copyToolContent&toolContentID='+o.data.activity.toolContentID+'&userID='+_root.userID,callback, false);
		
		} else if(o.data instanceof ToolActivity){
			Debugger.log('instance is Tool',Debugger.GEN,'setPastedItem','Canvas');
			var callback:Function = Proxy.create(this,setNewContentID, o);
			Application.getInstance().getComms().getRequest('authoring/author.do?method=copyToolContent&toolContentID='+o.toolContentID+'&userID='+_root.userID,callback, false);
			
		} else{
			Debugger.log('Cant paste this item!',Debugger.GEN,'setPastedItem','Canvas');
		}
	}
	
	private function setNewContentID(r, o){
		if(r instanceof LFError){
			r.showMessageConfirm();
		}else{
			_newToolContentID = r;
			if (o.data instanceof CanvasActivity){
				return pasteItem(o.data.activity, o, _newToolContentID);
			}else if(o.data instanceof ToolActivity){
				return pasteItem(o.data, o, _newToolContentID);
			}
		}
		
	}
	
	private function pasteItem(toolToCopy:ToolActivity, o:Object, newToolContentID:Number):Object{
		//clone the activity
		var newToolActivity:ToolActivity = toolToCopy.clone();
		newToolActivity.activityUIID = _ddm.newUIID();
		if (newToolContentID != null || newToolContentID != undefined){
			newToolActivity.toolContentID = newToolContentID;
		}
		newToolActivity.xCoord = o.data.activity.xCoord + 10
		newToolActivity.yCoord = o.data.activity.yCoord + 10
		canvasModel.selectedItem = newToolActivity;
			
		if(o.type == Application.CUT_TYPE){ 
			Application.getInstance().setClipboardData(newToolActivity, Application.COPY_TYPE);
			removeActivity(o.data.activity.activityUIID); 
		} else {
			if(o.count <= 1) { newToolActivity.title = Dictionary.getValue('prefix_copyof')+newToolActivity.title; }
			else { newToolActivity.title = Dictionary.getValue('prefix_copyof_count', [o.count])+newToolActivity.title; }
		}
		
		_ddm.addActivity(newToolActivity);
		canvasModel.setDirty();
		
		return newToolActivity;
	}
		
	/**
	 * Called from the toolbar usually - starts or stops the gate tool
	 * @usage   
	 * @return  
	 */
	public function toggleGroupTool():Void{
		var c:String = Cursor.getCurrentCursor();
		if(c==ApplicationParent.C_GROUP){
			stopGroupTool();
		}else{
			startGroupTool();
		}
	}
	
	public function toggleBranchTool():Void{
		var c:String = Cursor.getCurrentCursor();
		if(c==ApplicationParent.C_BRANCH){
			stopBranchTool();
		}else{
			startBranchTool();
		}
	}
	
	public function toggleGateTool():Void{
		var c:String = Cursor.getCurrentCursor();
		if(c==ApplicationParent.C_GATE){
			stopGateTool();
		}else{
			startGateTool();
		}
	}
	
	public function toggleOptionalActivity():Void{
		var c:String = Cursor.getCurrentCursor();
		if(c==ApplicationParent.C_OPTIONAL){
			stopOptionalActivity();
		}else{
			startOptionalActivity();
		}
	}
	
	public function toggleTransitionTool():Void{
		Debugger.log('Switch on Transition Tool', Debugger.GEN,'toogleTransitionTool','Canvas');
		var c:String = Cursor.getCurrentCursor();
		Debugger.log('Current Cursor: ' + c, Debugger.GEN, 'toogleTransitionTool', 'Canvas');
		
		if(c==ApplicationParent.C_TRANSITION) {
				stopTransitionTool();
		} else {
				startTransitionTool();
		}
		
	}
	
	public function stopActiveTool(){
		Debugger.log('Stopping Active Tool: ' + canvasModel.activeTool, Debugger.GEN,'stopActiveTool','Canvas');
		switch(canvasModel.activeTool){
			case CanvasModel.GATE_TOOL :
				stopGateTool();
				break;
			case CanvasModel.OPTIONAL_TOOL :
				stopOptionalActivity();
				break;
			case CanvasModel.GROUP_TOOL :
				stopGroupTool();
				break;
			case CanvasModel.TRANSITION_TOOL :
				stopTransitionTool();
				break;
			case CanvasModel.BRANCH_TOOL :
				stopBranchTool();
				break;
			default :
				Debugger.log('No tool active. Setting Default.', Debugger.GEN,'stopActiveTool','Canvas');
				Cursor.showCursor(ApplicationParent.C_DEFAULT);
				canvasModel.activeTool = "none";

		}
	}
	
	public function startGateTool(){
		Debugger.log('Starting gate tool',Debugger.GEN,'startGateTool','Canvas');
		Cursor.showCursor(ApplicationParent.C_GATE);
		canvasModel.activeTool = CanvasModel.GATE_TOOL;
	}
		
	public function stopGateTool(){
		Debugger.log('Stopping gate tool',Debugger.GEN,'stopGateTool','Canvas');
		Cursor.showCursor(ApplicationParent.C_DEFAULT);
		canvasModel.activeTool = "none";
	}
	
	
	public function startOptionalActivity(){
		Debugger.log('Starting Optioanl Activity',Debugger.GEN,'startOptionalActivity','Canvas');
		Cursor.showCursor(ApplicationParent.C_OPTIONAL);
		canvasModel.activeTool = CanvasModel.OPTIONAL_TOOL;
	}
		
	public function stopOptionalActivity(){
		Debugger.log('Stopping Optioanl Activity',Debugger.GEN,'stopOptionalActivity','Canvas');
		Cursor.showCursor(ApplicationParent.C_DEFAULT);
		canvasModel.activeTool = "none";
	}
	public function startGroupTool(){
		Debugger.log('Starting group tool',Debugger.GEN,'startGateTool','Canvas');
		Cursor.showCursor(ApplicationParent.C_GROUP);
		canvasModel.activeTool = CanvasModel.GROUP_TOOL;
	}
	
	public function stopGroupTool(){
		Debugger.log('Stopping group tool',Debugger.GEN,'startGateTool','Canvas');
		Cursor.showCursor(ApplicationParent.C_DEFAULT);
		canvasModel.activeTool = "none";
	}
	
	public function startBranchTool(){
		Debugger.log('Starting branch tool',Debugger.GEN,'startGateTool','Canvas');
		Cursor.showCursor(ApplicationParent.C_BRANCH);
		canvasModel.activeTool = CanvasModel.BRANCH_TOOL;
	}
	
	public function stopBranchTool(){
		Debugger.log('Stopping branch tool',Debugger.GEN,'startBranchTool','Canvas');
		Cursor.showCursor(ApplicationParent.C_DEFAULT);
		canvasModel.activeTool = "none";
	}
	
	/**
	 * Called by the top menu bar and the tool bar to start the transition tool, switches cursor.
	 * @usage   
	 * @return  
	 */
	public function startTransitionTool():Void{
		Debugger.log('Starting transition tool',Debugger.GEN,'startTransitionTool','Canvas');			
		Cursor.showCursor(ApplicationParent.C_TRANSITION);
		canvasModel.lockAllComplexActivities();
		canvasModel.startTransitionTool();
		
	}
	
	/**
	 * Called by the top menu bar and the tool bar to stop the transition tool, switches cursor.
	 * @usage   
	 * @return  
	 */
	public function stopTransitionTool():Void{
		Debugger.log('Stopping transition tool',Debugger.GEN,'stopTransitionTool','Canvas');			
		Cursor.showCursor(ApplicationParent.C_DEFAULT);
		canvasModel.unlockAllComplexActivities();
		canvasModel.stopTransitionTool();
	}
	
		/**
	 * Method to open Preview popup window.
	 */
	public function launchPreviewWindow():Void{
		if(_ddm.validDesign){
			Debugger.log('Launching Preview Window (initialising)',Debugger.GEN,'launchPreviewWindow','Canvas');
			var callback:Function = Proxy.create(this,onInitPreviewResponse); 
			Application.getInstance().getComms().sendAndReceive(_ddm.getDataForPreview(Dictionary.getValue('preview_btn'),"started%20automatically"),"monitoring/initializeLesson",callback,false)
		}
	}

	public function onInitPreviewResponse(r):Void{
		if(r instanceof LFError) {
			r.showMessageConfirm();
		} else {
			Debugger.log('Launching Preview Window (starting lesson ' + r + ')',Debugger.GEN,'onInitPreviewResponse','Canvas');
			var callback:Function = Proxy.create(this,onLaunchPreviewResponse); 
			Application.getInstance().getComms().getRequest('monitoring/monitoring.do?method=startPreviewLesson&lessonID='+r,callback, false);
		}
	}

	/**
	 * now contains a Lession ID response from wddx packet
	 * Returns the lessionID to send it to popup method in JsPopup .
	 * @usage   http://localhost:8080/lams/learning/learner.do?method=joinLesson&userId=4&lessonId=12 
	 * @param   r //the validation response
	 * @return  
	 */
	public function onLaunchPreviewResponse(r):Void{
		if(r instanceof LFError){
			r.showMessageConfirm();
		}else{
			var uID = Config.getInstance().userID;
			var serverUrl = Config.getInstance().serverUrl;

			// open preview in new window
			ApplicationParent.extCall("openPreview", r);
			Debugger.log('Recieved Lesson ID: '+r ,Debugger.GEN,'onLaunchPreviewResponse','Canvas');
		}
	}
	
	/**
	* Method to open Import popup window
	*/
	public function launchImportWindow():Void{
		Debugger.log('Launching Import Window',Debugger.GEN,'launchImportWindow','Canvas');
		if(_ddm.modified){
			LFMessage.showMessageConfirm(Dictionary.getValue('cv_design_unsaved'), Proxy.create(this,doImportLaunch), null);
		} else {
			doImportLaunch();
		}
	}
	
	public function doImportLaunch():Void{
		var serverUrl = Config.getInstance().serverUrl;
		JsPopup.getInstance().launchPopupWindow(serverUrl+'authoring/importToolContent.do?method=import', 'Import', 298, 800, true, true, false, false, false);
	}
	
	/**
	* Method to open Export popup window
	*/
	public function launchExportWindow():Void{
		Debugger.log('Launching Export Window',Debugger.GEN,'launchExportWindow','Canvas');
		
		if(_ddm.learningDesignID == null) {
			LFMessage.showMessageAlert(Dictionary.getValue('cv_design_export_unsaved'), null);
		}else if(_ddm.modified){
			LFMessage.showMessageConfirm(Dictionary.getValue('cv_design_unsaved'), Proxy.create(this,doExportLaunch), null);
		} else {
			doExportLaunch();
		}
		
	}
	
	public function doExportLaunch():Void{
		var serverUrl = Config.getInstance().serverUrl;
		var learningDesignID = _ddm.learningDesignID;
		JsPopup.getInstance().launchPopupWindow(serverUrl+'authoring/exportToolContent.do?learningDesignID=' + learningDesignID, 'Export', 298, 712, true, true, false, false, false);
	}
	
	private function createContentFolder():Void{
		var callback:Function = Proxy.create(this,setNewContentFolderID);
		Application.getInstance().getComms().getRequest('authoring/author.do?method=createUniqueContentFolder&userID='+_root.userID,callback, false);
		
	}
	
	private function setNewContentFolderID(o:Object) {
		if(o instanceof LFError){
			o.showMessageConfirm();
		}else{
			if(StringUtils.isNull(_ddm.contentFolderID)) { _ddm.contentFolderID = String(o); }
		}
		
	}
		
	/**
	* Used by application to set the size
	* @param width The desired width
	* @param height the desired height
	*/
	public function setSize(width:Number,height:Number):Void{
		canvasModel.setSize(width, height);
	}
	
	/**
	 * Sts up the bin
	 * @usage   
	 * @return  
	 */
	public function initBin():Void{
		var cc:CanvasController = canvasView.getController();
		_bin = _canvasView_mc.attachMovie("Bin", "Bin", _canvasView_mc.getNextHighestDepth(),{_canvasController:cc,_canvasView:canvasView});
	}
	
	public function addBin():Void{
		// var cc:CanvasController = canvasView.getController();
		// return view.attachMovie("Bin", "Bin", view.getNextHighestDepth(),{_controller:cc,_view:cv}
	}
	
	/**
	 * recieves event fired after update to the DDM
	 * @usage   
	 * @param   evt 
	 * @return  
	 */
	public function onDDMUpdated(evt:Object):Void{
		
		Debugger.log('DDM has been updated, _ddm.validDesign:'+_ddm.validDesign,Debugger.GEN,'onDDMUpdated','Canvas');
		//if its valid, its not anymore!
		if(_ddm.validDesign){
			_ddm.validDesign = false;
			checkValidDesign();
		}
		
		_ddm.modified = true;
		
		ApplicationParent.extCall('setSaved', 'false');
	}
	
	
	/**
	 * recieves event fired before updating the DDM
	 * @usage   
	 * @param   evt 
	 * @return  
	 */
	public function onDDMBeforeUpdate(evt:Object):Void{
		
		Debugger.log('DDM about to be updated',Debugger.GEN,'onDDMBeforeUpdate','Canvas');
		//take a snapshot of the design and save it in the undoStack
		var snapshot:Object = _ddm.toData();
		_undoStack.push(snapshot);
		_redoStack = new Array();
	}
	
	
	/**
	 * Undo the last change to the DDM.
	 * TODO: Does not handle moving activities on the canvas, only when actual change to activities or transitions.  
	 * Need to generate update event when re-position activities
	 * @usage   
	 * @return  
	 */
	public function undo():Void{
		
		if(_undoStack.length>0){
			//get the last state off the stack
			var snapshot = _undoStack.pop();
			
			//get a copy of the current design and stick it in redo
			_redoStack.push(_ddm.toData());
			
			clearCanvas(true);
			//set the current design to the snapshot value
			_ddm.setDesign(snapshot,true);
			canvasModel.setDirty();
			
		}else{
			Debugger.log("Cannot Undo! no data on stack!",Debugger.GEN,'redo','Canvas');
		}
	}
	
	/**
	 * Redo last what was undone by the undo method.
	 * NOTE: if a new edit is made, the re-do stack is cleared
	 * @usage   
	 * @return  
	 */
	public function redo():Void{
		
		if(_redoStack.length > 0){
			//get the last state off the stack
			var snapshot = _redoStack.pop();
			
			_undoStack.push(_ddm.toData());
			
			clearCanvas(true);
			
			_ddm.setDesign(snapshot,true);
			canvasModel.setDirty();
			
		}else{
			Debugger.log("Cannot Redo! no data on stack!",Debugger.GEN,'redo','Canvas');
		}
	
	}
	
	/**
	 * Open the Help page for the selected Tool (Canvas) Activity
	 *  
	 * @param   ca 	CanvasActivity
	 * @return  
	 */
	
	public function getHelp(ca:CanvasActivity) {

		if(ca.activity.helpURL != undefined || ca.activity.helpURL != null) {
			Debugger.log("Opening help page with locale " + _root.lang + _root.country + ": " + ca.activity.helpURL,Debugger.GEN,'getHelp','Canvas');
			var locale:String = _root.lang + _root.country;
			
			ApplicationParent.extCall("openURL", ca.activity.helpURL + app.module + "#" + ca.activity.toolSignature + app.module + "-" + locale);
		
		} else {
			if (ca.activity.activityTypeID == Activity.GROUPING_ACTIVITY_TYPE){
				var callback:Function = Proxy.create(this, openGroupHelp);
				app.getHelpURL(callback)
			}else if (ca.activity.activityTypeID == Activity.SYNCH_GATE_ACTIVITY_TYPE || ca.activity.activityTypeID == Activity.SCHEDULE_GATE_ACTIVITY_TYPE || ca.activity.activityTypeID == Activity.PERMISSION_GATE_ACTIVITY_TYPE){
				var callback:Function = Proxy.create(this, openGateHelp);
				app.getHelpURL(callback)
			}else {
				LFMessage.showMessageAlert(Dictionary.getValue('cv_activity_helpURL_undefined', [ca.activity.toolDisplayName]));
			}
		}
	}
	
	private function openGroupHelp(url:String){
		var actToolSignature:String = Application.FLASH_TOOLSIGNATURE_GROUP
		var locale:String = _root.lang + _root.country;
		var target:String = actToolSignature + app.module + '#' + actToolSignature+ app.module + '-' + locale;
		
		ApplicationParent.extCall("openURL", url + target);
	}
	
	private function openGateHelp(url:String){
		var actToolSignature:String = Application.FLASH_TOOLSIGNATURE_GATE
		var locale:String = _root.lang + _root.country;
		var target:String = actToolSignature + app.module + '#' + actToolSignature + app.module + '-' + locale;
		ApplicationParent.extCall("openURL", url + target);
	}
	
	public function get toolActivityWidth():Number{
		return toolActWidth;
	}
	
	public function get toolActivityHeight():Number{
		return toolActHeight;
	}
	
	public function get complexActivityWidth():Number{
		return complexActWidth;
	}
	
	private function setBusy():Void{
		if(_isBusy){
			//Debugger.log('!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!',1,'checkBusy','org.lamsfoundation.lams.common.util.Hashtable');
			//Debugger.log('!!!!!!!!!!!!!!!!!!!! HASHTABLE ACCESED WHILE BUSY !!!!!!!!!!!!!!!!',1,'checkBusy','org.lamsfoundation.lams.common.util.Hashtable');
			//Debugger.log('!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!',1,'checkBusy','org.lamsfoundation.lams.common.util.Hashtable');
		}
		_isBusy=true;
	}
	
	private function clearBusy():Void{
		_isBusy=false;
	}
	/**
	* Used by application to set the Position
	* @param x
	* @param y
	*/
	public function setPosition(x:Number,y:Number):Void{
		canvasModel.setPosition(x,y);
	}
	
	public function get model():CanvasModel{
		return getCanvasModel();
	}
	
	public function getCanvasModel():CanvasModel{
			return canvasModel;
	}
	
	public function get view():MovieClip{
		return getCanvasView();
	}
		
	
	public function getCanvasView():MovieClip{
		return canvasView;
	}
	
	public function get className():String{
		return 'Canvas';
	}
	
	public function get ddm():DesignDataModel{
		return _ddm;
	}
	
	public function get taWidth():Number{
		return toolActWidth	
	}
	
	public function get taHeight():Number{
		return toolActHeight
	}

	/**
	 * 
	 * @usage   
	 * @return  
	 */
	public function get bin ():MovieClip {
		return _bin;
	}

	
	
}

﻿import mx.controls.*
import mx.utils.*
import mx.managers.*
import mx.events.*
import org.lamsfoundation.lams.common.ws.*
import org.lamsfoundation.lams.common.util.*
import org.lamsfoundation.lams.common.dict.*
import org.lamsfoundation.lams.common.style.*
import it.sephiroth.XML2Object

/**
* @author      DI
*/
class WorkspaceDialog extends MovieClip{
 
    //private static var OK_OFFSET:Number = 50;
    //private static var CANCEL_OFFSET:Number = 50;

    //References to components + clips 
    private var _container:MovieClip;       //The container window that holds the dialog
    private var ok_btn:Button;              //OK+Cancel buttons
    private var cancel_btn:Button;
    private var panel:MovieClip;            //The underlaying panel base
    private var treeview:Tree;              //Treeview for navigation through workspace folder structure
    private var datagrid:DataGrid;          //The details grid
    private var myLabel_lbl:Label;          //Text labels
    private var input_txt:TextInput;        //Text labels
    private var combo:ComboBox;             //Text labels
    
    private var fm:FocusManager;            //Reference to focus manager
    private var themeManager:ThemeManager;  //Theme manager
	
	private var _workspaceView:WorkspaceView;


    
    //Dimensions for resizing
    private var xOkOffset:Number;
    private var yOkOffset:Number;
    private var xCancelOffset:Number;
    private var yCancelOffset:Number;
    
    private var _okCallBack:Function;
    private var _selectedDesignId:Number;
    
    //These are defined so that the compiler can 'see' the events that are added at runtime by EventDispatcher
    private var dispatchEvent:Function;     
    public var addEventListener:Function;
    public var removeEventListener:Function;
    
    
    /**
    * constructor
    */
    function WorkspaceDialog(){
        //trace('WorkSpaceDialog.constructor');
        //Set up this class to use the Flash event delegation model
        EventDispatcher.initialize(this);
        
        //Create a clip that will wait a frame before dispatching init to give components time to setup
        this.onEnterFrame = init;
    }

    /**
    * Called a frame after movie attached to allow components to initialise
    */
	private function init(){
        //Delete the enterframe dispatcher
        delete this.onEnterFrame;
        
        //TODO DI 25/05/05 ID set as 1 is just a stub, selected id from dialog should replace
        //_selectedDesignId = 1;
        
       
        
        //set the reference to the StyleManager
        themeManager = ThemeManager.getInstance();
        
        //Set the container reference
        Debugger.log('container=' + _container,Debugger.GEN,'init','org.lamsfoundation.lams.wsDialog');

        //Set the text on the labels
        myLabel_lbl.text = 'Enter the ID of the design you want to open:';
        
        //Set the text for buttons
        ok_btn.label = Dictionary.getValue('ws_dlg_ok_button');
        cancel_btn.label = Dictionary.getValue('ws_dlg_cancel_button');
        
        //get focus manager + set focus to OK button, focus manager is available to all components through getFocusManager
        fm = _container.getFocusManager();
        fm.enabled = true;
        ok_btn.setFocus();
        //fm.defaultPushButton = ok_btn;
        
        Debugger.log('ok_btn.tabIndex: '+ok_btn.tabIndex,Debugger.GEN,'init','org.lamsfoundation.lams.WorkspaceDialog');
        
        //Add event listeners for ok, cancel and close buttons
        ok_btn.addEventListener('click',Delegate.create(this, ok));
        cancel_btn.addEventListener('click',Delegate.create(this, cancel));
        //Tie parent click event (generated on clicking close button) to this instance
        _container.addEventListener('click',this);
        //Register for LFWindow size events
        _container.addEventListener('size',this);
		
		
        
        //Debugger.log('setting offsets',Debugger.GEN,'init','org.lamsfoundation.lams.common.ws.WorkspaceDialog');

        //work out offsets from bottom RHS of panel
        xOkOffset = panel._width - ok_btn._x;
        yOkOffset = panel._height - ok_btn._y;
        xCancelOffset = panel._width - cancel_btn._x;
        yCancelOffset = panel._height - cancel_btn._y;
        
        //Register as listener with StyleManager and set Styles
        themeManager.addEventListener('themeChanged',this);
		//TODO: Make setStyles more efficient
		//setStyles();
        
        //Fire contentLoaded event, this is required by all dialogs so that creator of LFWindow can know content loaded
        _container.contentLoaded();
    }
	
	/**
	 * Called by the worspaceView after the content has loaded
	 * @usage   
	 * @return  
	 */
	public function setUpContent():Void{
		
		
		//register to recive updates form the model
		WorkspaceModel(_workspaceView.getModel()).addEventListener('viewUpdate',this);
		
		//Set up the treeview
        setUpTreeview();
		
	}
	
	/**
	 * Recieved update events from the WorkspaceModel. Dispatches to relevent handler depending on update.Type
	 * @usage   
	 * @param   event
	 */
	public function viewUpdate(event:Object):Void{
		Debugger.log('Recived an Event dispather UPDATE!, updateType:'+event.updateType+', target'+event.target,4,'viewUpdate','WorkspaceView');
		 //Update view from info object
        //Debugger.log('Recived an UPDATE!, updateType:'+infoObj.updateType,4,'update','CanvasView');
       var wm:WorkspaceModel = event.target;
	   //set a ref to the controller for ease (sorry mvc guru)
	  
	   switch (event.updateType){

			case 'REFRESH_TREE' :
                refreshTree(event.data,wm);
                break;
           
            default :
                Debugger.log('unknown update type :' + event.updateType,Debugger.GEN,'viewUpdate','org.lamsfoundation.lams.WorkspaceDialog');
		}

	}
	
	public function refreshTree(changedNode:XMLNode,wm:WorkspaceModel){
		 Debugger.log('Refreshing tree....:' ,Debugger.GEN,'refreshTree','org.lamsfoundation.lams.WorkspaceDialog');
		 //we have to set the new nodes to be branches, if they are branches
		if(changedNode.attributes.isBranch){
			treeview.setIsBranch(changedNode,true);
			//do its kids
			for(var i=0; i<changedNode.childNodes.length; i++){
				var cNode:XMLNode = changedNode.getTreeNodeAt(i);
				if(cNode.attributes.isBranch){
					treeview.setIsBranch(cNode,true);
				}
			}
		}
		 
		 treeview.refresh();
	}
	
	
    
    /**
    * Event fired by StyleManager class to notify listeners that Theme has changed
    * it is up to listeners to then query Style Manager for relevant style info
    */
    public function themeChanged(event:Object){
        if(event.type=='themeChanged') {
            //Theme has changed so update objects to reflect new styles
            setStyles();
        }else {
            Debugger.log('themeChanged event broadcast with an object.type not equal to "themeChanged"',Debugger.CRITICAL,'themeChanged','org.lamsfoundation.lams.WorkspaceDialog');
        }
    }
    
    /**
    * Called on initialisation and themeChanged event handler
    */
    private function setStyles(){
        //LFWindow, goes first to prevent being overwritten with inherited styles.
        var styleObj = themeManager.getStyleObject('LFWindow');
        _container.setStyle('styleName',styleObj);

        //Get the button style from the style manager
        styleObj = themeManager.getStyleObject('button');
        
        //apply to both buttons
        Debugger.log('styleObject : ' + styleObj,Debugger.GEN,'setStyles','org.lamsfoundation.lams.WorkspaceDialog');
        ok_btn.setStyle('styleName',styleObj);
        cancel_btn.setStyle('styleName',styleObj);
        
        //Get label style and apply to label
        styleObj = themeManager.getStyleObject('label');
        myLabel_lbl.setStyle('styleName',styleObj);

        //Apply treeview style 
        styleObj = themeManager.getStyleObject('treeview');
        treeview.setStyle('styleName',styleObj);

        //Apply datagrid style 
        styleObj = themeManager.getStyleObject('datagrid');
        datagrid.setStyle('styleName',styleObj);

        //Apply combo style 
        styleObj = themeManager.getStyleObject('combo');
        combo.setStyle('styleName',styleObj);
    }

    /**
    * Called by the cancel button 
    */
    private function cancel(){
        trace('Cancel');
        //close parent window
        _container.deletePopUp();
    }
    
    /**
    * Called by the OK button 
    */
    private function ok(){
        trace('OK');
		//set the selectedDesignId
		_selectedDesignId = Number(input_txt.text);
       //If validation successful commit + close parent window
       //Fire callback with selectedId
       dispatchEvent({type:'okClicked',target:this});
       _container.deletePopUp();
    }
    
    /**
    * Event dispatched by parent container when close button clicked
    */
    private function click(e:Object){
        trace('WorkspaceDialog.click');
        e.target.deletePopUp();
    }
    
	/**
	 * Recursive function to set any folder with children to be a branch
	 * TODO: Might / will have to change this behaviour once designs are being returned into the mix
	 * @usage   
	 * @param   node 
	 * @return  
	 */
    private function setBranches(node:XMLNode){
		if(node.hasChildNodes()){
			treeview.setIsBranch(node, true);
			for (var i = 0; i<node.childNodes.length; i++) {
				var cNode = node.getTreeNodeAt(i);
				treeview.setIsBranch(cNode, true);
				setBranches(cNode);
			}
		}
	}
	

	
	
	/**
	 * Sets up the treeview with whatever datya is in the treeDP
	 * TODO - extend this to make it recurse all the way down the tree
	 * @usage   
	 * @return  
	 */
	private function setUpTreeview(){
			
		var converter:XML2Object = new XML2Object();
	
		//Debugger.log('_workspaceView:'+_workspaceView,Debugger.GEN,'setUpTreeview','org.lamsfoundation.lams.common.ws.WorkspaceDialog');
		treeview.dataProvider = WorkspaceModel(_workspaceView.getModel()).treeDP;
		
		Debugger.log('WorkspaceModel(_workspaceView.getModel()).treeDP:'+WorkspaceModel(_workspaceView.getModel()).treeDP.toString(),Debugger.GEN,'setUpTreeview','org.lamsfoundation.lams.common.ws.WorkspaceDialog');
		
		//get the 1st child
		var fNode = treeview.dataProvider.firstChild;
		
		
		
			/*
	* 
	
		//loop thorigh th childresn to see if they are branches
		for (var i = 0; i<fNode.childNodes.length; i++) {
			var node:XMLNode = fNode.getTreeNodeAt(i);
			// Set each of the 3 initial child nodes to be branches
			if(node.attributes.isBranch){
				treeview.setIsBranch(node,true);
				//also check this branches children to see if they have isBranch set
				if(node.hasChildNodes()){
					for(var j=0; j<node.childNodes.length; j++){
						var cNode:XMLNode = node.getTreeNodeAt(j);
						if(cNode.attributes.isBranch){
							treeview.setIsBranch(cNode,true);
							
						}
						
					}
					
					
				}
				
				
				
			}
			
			
		}
	*/
		
		setBranches(fNode);
		
		
		
		Debugger.log('_workspaceView:'+_workspaceView,Debugger.GEN,'setUpTreeview','org.lamsfoundation.lams.common.ws.WorkspaceDialog');
		var wsc:WorkspaceController = _workspaceView.getController();
		Debugger.log('wsc:'+wsc,Debugger.GEN,'setUpTreeview','org.lamsfoundation.lams.common.ws.WorkspaceDialog');
		Debugger.log('wsc.onTreeNodeOpen:'+wsc.onTreeNodeOpen,Debugger.GEN,'setUpTreeview','org.lamsfoundation.lams.common.ws.WorkspaceDialog');
		
		
		treeview.addEventListener("nodeOpen", Delegate.create(wsc, wsc.onTreeNodeOpen));
		treeview.addEventListener("nodeClose", Delegate.create(wsc, wsc.onTreeNodeClose));
		treeview.refresh();
		
    }
    
    /**
    * XML onLoad handler for treeview data
 */
    private function tvXMLLoaded (ok:Boolean,rootXML:XML){
        if(ok){
            /*
			//Set the XML as the data provider for the tree
            treeview.dataProvider = rootXML.firstChild;
            treeview.addEventListener("change", Delegate.create(this, onTvChange));
            
            //Add this function to prevent displaying [type function],[type function] when label attribute missing from XML
            treeview.labelFunction = function(node) {
                    return node.nodeType == 1 ? node.nodeName : node.nodeValue;
            };
            */
        }
    }
    
     
    /**
    * Main resize method, called by scrollpane container/parent
    */
    public function setSize(w:Number,h:Number){
        //Debugger.log('setSize',Debugger.GEN,'setSize','org.lamsfoundation.lams.common.ws.WorkspaceDialog');
        //Size the panel
        panel.setSize(w,h);

        //Buttons
        ok_btn.move(w-xOkOffset,h-yOkOffset);
        cancel_btn.move(w-xCancelOffset,h-yCancelOffset);
    }
    
    //Gets+Sets
    /**
    * set the container refernce to the window holding the dialog
    */
    function set container(value:MovieClip){
        _container = value;
    }
	
	/**
	 * 
	 * @usage   
	 * @param   newworkspaceView 
	 * @return  
	 */
	public function set workspaceView (newworkspaceView:WorkspaceView):Void {
		_workspaceView = newworkspaceView;
	}
	
	/**
	 * 
	 * @usage   
	 * @return  
	 */
	public function get workspaceView ():WorkspaceView {
		return _workspaceView;
	}
	
    
    function get selectedDesignId():Number { 
        return _selectedDesignId;
    }
}
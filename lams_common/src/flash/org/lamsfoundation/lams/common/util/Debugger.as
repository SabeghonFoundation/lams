﻿import org.lamsfoundation.lams.common.util.*
import mx.events.*/**  
* Debug  
* Can be used to print message to a floating windoe and to trace windoe.  SHoudl be used over trace().  
* Usage:
* import org.lamsfoundation.lams.common.util.Debug;
* 
* Debug.log('_toolkit_mc:'+_toolkit_mc,5,'createToolkit','ToolkitView');
* 
* 
*/  
class Debugger {  
	
    //Declarations  
    public static var CRITICAL:Number = 1;      //level constants
    public static var HIGH:Number = 2;
    public static var MED:Number = 3;
    public static var GEN:Number = 4;
    public static var COMP:Number = 5;
    
    private static var _severityLevel:Number = 5;
	private static var _allowDebug:Boolean = true;
	
	private static var _currentClass:String;
    private static var _instance:Debugger = null;
    
    //These are defined so that the compiler can 'see' the events that are added at runtime by EventDispatcher
    private var dispatchEvent:Function;     
    public var addEventListener:Function;
    public var removeEventListener:Function;
    
    private static var _msgLog:Array;


    //TODO DI 07/06/05 Add code to get dump of flash from _root
    
	//Constructor  
	private function Debugger() {  
        //Set up this class to use the Flash event delegation model
        EventDispatcher.initialize(this);
        _msgLog = [];
	}  
    
    /**
    * Retrieves an instance of the Application singleton
    */ 
    public static function getInstance():Debugger{
        if(Debugger._instance == null){
            Debugger._instance = new Debugger();
        }
        return Debugger._instance;
    }
	
	
	public function set allowDebug(arg:Boolean):Void{
			_allowDebug = arg;
	}
	
	public function get allowDebug():Boolean{
			return _allowDebug;
	}
	
	public function set severityLevel(sLevel:Number):Void{
			_severityLevel = sLevel;
	}
	
	public function get severityLevel():Number{
			return _severityLevel;
	}
    
			/**
	* Method to print a message to the output - trace or window...
	* @param msg 			The actual message to be printed
	* @param level 			(Optional) Severity of this messgae:
	* 						1=critical error > 4 = general debugging message, 5 = component debug message
	* @param fname 			(Optional) Name of the function calling this log message
	* @param currentClass 	(Optional)Name of the class
	*/
	public static function log(msg:String,level:Number,fname:String,currentClass:Object):Void{
        //Ensure we have an instance
        getInstance();
        
		if(_allowDebug){
			if(arguments.length == 1){
				level = 4;
			}			
			if(_severityLevel >= level){
				//trace('currentClass :' + currentClass);
				//if the class name has changed, then print it out
				if(_currentClass != currentClass){
					_currentClass = String(currentClass);
					trace("-----------In:"+_currentClass+"-----------");
					
				}
				//Write to trace
                msg = "["+fname+"]"+msg
				trace(msg);
                var date:Date = new Date();
                
                //Write to log
                _msgLog.push({date:date,msg:msg,level:level});
                //Dispatch update event
                _instance.dispatchEvent({type:'update',target:_instance});
			}
		}
	}
	/**
	* Legacy Method to print a message to the output - trace or window...
	* @param msg 			The actual message to be printed
	* 						1=critical error > 4 = general debugging message, 5 = component debug message
	* @param fname 			(Optional) Name of the function calling this log message
	*/
	public static function debug(msg:String,fname:String):Void{
		if(_allowDebug){
			if(arguments.length == 1){
				fname = "";
			}
			log(msg,4,fname,undefined);
		}
	}
    
    /**
    * @param format:Object  An object containing various format options for viewing the log
    * @returns the message log formatted
    */
    public function getFormattedMsgLog(format:Object):String {
        var ret:String;
        if(!format){
            format = {};
            format.date = false;
        }
        //Loop through messages and build return string
        for(var i=0;i<_msgLog.length;i++) {
            ret += buildMessage(format,_msgLog[i]);
        }
        return ret;
    }
    
    /**
    * Construct a message from the msgObject using the format object
    * 
    * @returns String - the formatted string
    */
    private function buildMessage(format:Object,msgObj:Object):String {
        var ret:String;
        var colour:String;
        //Get the color for the level
        switch (msgObj.level) {
            case CRITICAL :
                colour='#ff0000';
                break;
            case GEN :
                colour='#0000ff';
                break;
            case GEN :
                colour='#00ff00';
                break;
            default:
                colour='#555555';
        }

        //Build font tags
        var fontOpenTag:String = '<font color= "' + colour + '">'
        var fontCloseTag:String = '</font>'
        //Include date?
        if(format.date) {
            ret = fontOpenTag + msgObj.msg + ' date : ' + msgObj.date.toString() + fontCloseTag + '<BR>';
        } else {
            ret = fontOpenTag +  msgObj.msg + fontCloseTag + '<BR>';
        }
        return ret;
    }
    
    /**
    */
    function get msgLog(){
        return _msgLog;   
    }
    
}
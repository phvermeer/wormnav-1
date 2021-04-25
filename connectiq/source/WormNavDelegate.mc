using Toybox.WatchUi;
using Transform;

var eventText;
const TRACK_MODE = 0;
const DATA_MODE = 1;

var mode;
var dataPage = 0;

class ExitConfirmationDelegate extends WatchUi.ConfirmationDelegate {
    
    function initialize() {
        ConfirmationDelegate.initialize();
    }
    
    
    function onResponse(response) {
        if (response == WatchUi.CONFIRM_NO) {
            //System.println("Cancel");
            //return true;
        } else {
            //System.println("Confirm");
              System.exit();
              return true;             
        }
        return false;
    }
}

class WormNavDelegate extends WatchUi.BehaviorDelegate {


    // Workaround on FR230 and 235 where holding menu button also fires onPreviousPage 
    // see https://forums.garmin.com/developer/connect-iq/f/discussion/4294/fr230-holding-menu-button-triggers-onpreviouspage-and-then-onmenu
    var onPrevPageTick = 0;
    var isOnPrevPageLastEvent = false;

    function initialize() {
        BehaviorDelegate.initialize();
    }

    function onKey(keyEvent) {
        // System.println("onKey:" + keyEvent.getKey());
        if(keyEvent.getKey() == WatchUi.KEY_ENTER || keyEvent.getKey == WatchUi.KEY_START) {
            startStopActivity();
            return true;
        }
        return false;
    }

    // When a next page behavior occurs, onNextPage() is called.
    // @return [Boolean] true if handled, false otherwise
    function onNextPage() {
        //System.println("onNextPage()");
        isOnPrevPageLastEvent = false;
        switch(mode) {
            case TRACK_MODE:
                Transform.setZoomLevel(-2);
                updateView();
                break;
            case DATA_MODE:
                dataPageChange(1);
                break;
        }
        return true;
    }

    // When a previous page behavior occurs, onPreviousPage() is called.
    // @return [Boolean] true if handled, false otherwise
    function onPreviousPage() {
        //System.println("onPreviousPage()");
        switch(mode) {
            case TRACK_MODE:
                Transform.setZoomLevel(-1);
                updateView();
                $.trackViewCounter = 0;
                break;
            case DATA_MODE:
                dataPageChange(-1);
                break;
        }
        onPrevPageTick = $.appTimerTicks;
        isOnPrevPageLastEvent = true;
        return true;
    }

    // When a back behavior occurs, onBack() is called.
    // @return [Boolean] true if handled, false otherwise
    function onBack() {
		//System.println("onBack");
		// If active session is stopped asked for discard/save/resume
        isOnPrevPageLastEvent = false;
		if( $.session != null  &&  $.session.isRecording() == false ) {
        	WatchUi.pushView(new Rez.Menus.SaveMenu(), new SaveMenuDelegate(), WatchUi.SLIDE_UP);
            return true;    
        }
        
        // If there is no session exit;
        if ($.session == null || $.session.isRecording() == false) {
        	//System.exit();
        	
        	//ASK_USER:
            //var message = "Exit App?";
            var message = WatchUi.loadResource(Rez.Strings.msg_exit_app);
                    
            var dialog = new WatchUi.Confirmation(message);
            WatchUi.pushView(
                        dialog,
                        new ExitConfirmationDelegate(),
                        WatchUi.SLIDE_IMMEDIATE
                    ); 
            
            return true;       	
        	
        }
        if(mode==TRACK_MODE && Data.activeDataScreens.size() > 0 ) {
            if(dataView==null) {
            	dataView = new DataView(Data.activeDataScreens[dataPage]);
            }
            mode=DATA_MODE;
            WatchUi.switchToView(dataView, self, WatchUi.SLIDE_IMMEDIATE);             
        }
        else if(mode==DATA_MODE) {
        	mode=TRACK_MODE;
            WatchUi.switchToView(trackView, self, WatchUi.SLIDE_IMMEDIATE);
        }      
        return true;
    }

    // When a next mode behavior occurs, onNextMode() is called.
    // @return [Boolean] true if handled, false otherwise
    function onNextMode() {
        //System.println("onNextMode()");
        return true;
    }

    // When a previous mode behavior occurs, onPreviousMode() is called.
    // @return [Boolean] true if handled, false otherwise
    function onPreviousMode() {
        //System.println("onPreviousMode()");
        return true;
    }

    function onMenu() {
        // System.println("onMenu()");
        // Workaround for issue
        if(isOnPrevPageLastEvent && $.appTimerTicks - onPrevPageTick < 3) {
            // counteract 
            onNextPage(); 
        }
        isOnPrevPageLastEvent = false;
        var menu = new Rez.Menus.MainMenu();
        WatchUi.pushView(menu, new MainMenuDelegate(), WatchUi.SLIDE_IMMEDIATE);
        return true;
    }

    private function startStopActivity() {
       if( Toybox has :ActivityRecording ) {
            if( ( $.session == null ) || ( $.session.isRecording() == false ) ) {
                if($.session==null) {
                    $.session = ActivityRecording.createSession({:name => "WormNavActivity", :sport => $.activityType});
                }
                $.session.start();
                $.sessionEvent = 1;
                updateView();
                if (Attention has :playTone) {
                    Attention.playTone(Attention.TONE_START);
                }
            }
            else if( ( $.session != null ) && $.session.isRecording() ) {
                // System.println("stop session");
                $.session.stop();
                $.sessionEvent = 2;
                updateView();
                if (Attention has :playTone) {
                    Attention.playTone(Attention.TONE_STOP);
                }
            }
        }  
    }

    private function dataPageChange(n) {
        if(Data.activeDataScreens.size() == 0) {
            // this might happen when data screen settings have been changed
            onBack();
        } else {
            dataPage = (dataPage + n) % Data.activeDataScreens.size();
            if(dataPage<0) {
                dataPage = (Data.activeDataScreens.size()-1) % Data.activeDataScreens.size();
            }
            dataView.setDataFields(Data.activeDataScreens[dataPage]);
            WatchUi.switchToView(dataView, self, WatchUi.SLIDE_IMMEDIATE);
        }
        return;
    }

    private function updateView() {
        if(mode == TRACK_MODE) {
            $.trackViewCounter = 0;
        } else if (mode == DATA_MODE) {
            $.dataViewCounter = 0;
        }
        WatchUi.requestUpdate();
    }

}
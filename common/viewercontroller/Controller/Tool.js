/**
 * @class 
 * @constructor
 * @description The class for controls 
 * @param id The id of the tool
 * @param frameworkObject The frameworkspecific object, to store as a reference
 * @param type The type of tool to be created
 */
Ext.define("viewer.viewercontroller.controller.Tool",{
    extend: "Ext.util.Observable",
    events: [],
    config :{
        id: "id",
        frameworkObject: new Object(),
        type: -1
    },
    constructor: function (config){
        this.initConfig(config);
        this.addEvents(viewer.viewercontroller.controller.Event.ON_CLICK,viewer.viewercontroller.controller.Event.ON_EVENT_DOWN,viewer.viewercontroller.controller.Event.ON_EVENT_UP);
        return this;
    },
    
    fire : function (event,options){
        this.fireEvent(event,this,options);
    },

    registerEvent : function (event,handler){
        this.addListener(event,handler);
    },
    statics:{
        // The different types of tools
        DRAW_FEATURE               : 0,
        NAVIGATION_HISTORY         : 1,
        ZOOMIN_BOX                 : 2,
        ZOOMOUT_BOX                : 3,
        PAN                        : 4,
        SUPERPAN                   : 5,
        BUTTON                     : 6,
        TOGGLE                     : 7,
        CLICK                      : 8,
        LOADING_BAR                : 9,
        GET_FEATURE_INFO           : 10,
        MEASURE                    : 11,
        SCALEBAR                   : 12,
        ZOOM_BAR                   : 13,
        LAYER_SWITCH               : 14,

        DRAW_FEATURE_POINT         : 15,
        DRAW_FEATURE_LINE          : 16,
        DRAW_FEATURE_POLYGON       : 17,
        PREVIOUS_EXTENT            : 18,
        NEXT_EXTENT                : 19,
        FULL_EXTENT                : 20
    },
    getFrameworkTool : function(){
        return this.frameworkTool;
    },

    getType : function(){
        return this.type;
    },

    getId : function(){
        return this.id;
    },

    setVisible : function(){
        throw("Tool.setVisible() not implemented! Must be implemented in sub-class");
    },

    isActive : function(){
        throw("Tool.isActive() not implemented! Must be implemented in sub-class");
    }
});

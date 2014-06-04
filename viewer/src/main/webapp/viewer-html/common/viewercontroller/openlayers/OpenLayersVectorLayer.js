/* 
 * Copyright (C) 2012-2013 B3Partners B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * @class 
 * @constructor
 * @description A drawable vector layer
 */
Ext.define("viewer.viewercontroller.openlayers.OpenLayersVectorLayer",{
    extend: "viewer.viewercontroller.controller.VectorLayer",
    mixins: {
        openLayersLayer: "viewer.viewercontroller.openlayers.OpenLayersLayer"
    },
    point:null,
    line:null,
    polygon:null,
    circle: null,
    drawFeatureControls:null,
    modifyFeature:null,
    constructor : function (config){
        config.colorPrefix = '#';
        viewer.viewercontroller.openlayers.OpenLayersVectorLayer.superclass.constructor.call(this, config);
        this.mixins.openLayersLayer.constructor.call(this,config);
   
        var styleMap = new OpenLayers.StyleMap (
        {
            "default" :this.getCurrentStyleHash(),
            "temporary": this.getCurrentStyleHash(),
            "select":{
                'strokeColor' : '#FF0000',
                'strokeWidth': 2,
                'fillColor' : this.colorPrefix + config.style['fillcolor'],
                'fillOpacity': 0.4,
                'strokeOpacity':0.8,
                'pointRadius': 6
            }
        },{extendDefault:false});
        config.styleMap = styleMap;
        
        // Delete style from config, because it messes up the styling in the vectorlayer.
        delete config.style;
        this.frameworkLayer = new OpenLayers.Layer.Vector(config.id, config);
        this.type=viewer.viewercontroller.controller.Layer.VECTOR_TYPE;
        
        // Make all drawFeature controls: the controls to draw features on the vectorlayer
        this.point =  new OpenLayers.Control.DrawFeature(this.frameworkLayer, OpenLayers.Handler.Point, {
            displayClass: 'olControlDrawFeaturePoint'
        });
        this.line = new OpenLayers.Control.DrawFeature(this.frameworkLayer, OpenLayers.Handler.Path, {
            displayClass: 'olControlDrawFeaturePath'
        });
        this.polygon =  new OpenLayers.Control.DrawFeature(this.frameworkLayer, OpenLayers.Handler.Polygon, {
            displayClass: 'olControlDrawFeaturePolygon'
        });
        this.circle = new OpenLayers.Control.DrawFeature(this.frameworkLayer,OpenLayers.Handler.RegularPolygon,{
            handlerOptions: {
                sides: 40}
        });
        this.drawFeatureControls = new Array();
        this.drawFeatureControls.push(this.circle);
        this.drawFeatureControls.push(this.polygon);
        this.drawFeatureControls.push(this.line);
        this.drawFeatureControls.push(this.point);
        
        // The modifyfeature control allows us to edit and select features.
        this.modifyFeature = new OpenLayers.Control.ModifyFeature(this.frameworkLayer, { createVertices : true, standalone:false });
        
        var map = this.viewerController.mapComponent.getMap().getFrameworkMap();
        map.addControl(this.point);
        map.addControl(this.line);
        map.addControl(this.polygon);
        map.addControl(this.circle);
        map.addControl(this.modifyFeature);
        
        this.frameworkLayer.events.register("afterfeaturemodified", this, this.featureModified);
        this.frameworkLayer.events.register("featuremodified", this, this.featureModified);
        this.frameworkLayer.events.register("featureadded", this, this.featureAdded);
        
        this.modifyFeature.activate();
    },
    /**
     * Get the current hash with all the stylesettings. To be used in olFeature.style
     */
    getCurrentStyleHash : function(){
          var hash = {
           'strokeColor' : this.colorPrefix+ this.style['strokecolor'],
            'strokeWidth': 3,
            'pointRadius': 6,
            'fillColor' : this.colorPrefix + this.style['fillcolor'],
            'fillOpacity': this.style['fillopacity'] / 100
        };
        return hash;
    },
    
    /**
     * Does nothing, but is needed for API compliance
     */
    adjustStyle : function(){
    },
    /**
     * Removes all features and all 'sketches' (not finished geometries)
     */
    removeAllFeatures : function(){
        this.getFrameworkLayer().removeAllFeatures();
         // Quite ugly, but seems better than iterating over every feature and unselecting it at the modifyFeature: that has the unwanted side-effect of render it 
         // to the default intent. Had expected that ModifyFeature control had a removeAll() function, but it didn't
        this.modifyFeature.feature=null;   
        this.stopDrawing();
    },

    getActiveFeature : function(){        
        var olFeature = this.getFrameworkLayer().features[0];
        if (olFeature){
            var feature = this.fromOpenLayersFeature(olFeature);
            return feature;
        }else{
            return null;
        }
    },

    getFeature : function(index){
        return this.getFrameworkLayer().features[index];
    },

    /**
     * Gets a feature by the id. 
     *
     */
    getFeatureById : function (id){
        return this.fromOpenLayersFeature(this.getFrameworkLayer().getFeatureById(id));
    },

    getAllFeatures : function(){
        var olFeatures = this.getFrameworkLayer().features;
        var features = new Array();
        for(var i = 0 ; i < olFeatures.length;i++){
            var olFeature = olFeatures[i];
            var feature = this.fromOpenLayersFeature(olFeature);
            features.push(feature);
        }
        return features;
    },

    addFeature : function(feature){
        var features = new Array();
        features.push(feature);
        this.addFeatures(features);
    },

    /**
     * Removes the given feature from this vectorlayer
     * @param feature The feature to be removed
     */
    removeFeature : function (feature){
        var olFeature = this.getFrameworkLayer().getFeatureById(feature.getId());
        this.modifyFeature.unselectFeature(olFeature);
        this.getFrameworkLayer().removeFeatures([olFeature]);
    },
    
    addFeatures : function(features){
        var olFeatures = new Array();
        for(var i = 0 ; i < features.length ; i++){
            var feature = features[i];
            var olFeature = this.toOpenLayersFeature(feature);
            olFeatures.push(olFeature);
            olFeature.style = this.getCurrentStyleHash();
            // Check if framework independed feature has a label. If so, set it to the style
            if(feature.label){
                olFeature.style['label'] = feature.label;
            }
        }
        return this.getFrameworkLayer().addFeatures(olFeatures);
    },

    drawFeature : function(type){
        if(type == "Point"){
            this.point.activate();
        }else if(type == "LineString"){
            this.line.activate();
        }else if(type == "Polygon"){
            this.polygon.activate();
        }else if(type == "Circle"){
            this.circle.activate();
        }else{
           this.viewerController.logger.warning("Feature type >" + type + "< not implemented!");
        }
    },
    /**
     * Stop the drawing controls
     */
    stopDrawing: function(){
        //also stop drawing
        if (this.point.active){
            this.point.cancel();
            this.point.deactivate();
        }if (this.line.active){
            this.line.cancel();
            this.line.deactivate();
        }if (this.polygon.active){
            this.polygon.cancel();
            this.polygon.deactivate();
        }if (this.circle.active){
            this.circle.cancel();
            this.circle.deactivate();
        }
    },
    
    /**
     * Called when a feature is selected
     */
    activeFeatureChanged : function (object){
        var feature = this.fromOpenLayersFeature (object.feature);
        this.modifyFeature.selectFeature(object.feature);
        this.fireEvent(viewer.viewercontroller.controller.Event.ON_ACTIVE_FEATURE_CHANGED,this,feature);
    },
    
    /**
     * Called when a feature is modified.
     */
    featureModified : function (evt){
        var featureObject = this.fromOpenLayersFeature(evt.feature);
        this.fireEvent(viewer.viewercontroller.controller.Event.ON_FEATURE_ADDED,this,featureObject);
    },
    
    /**
     * Called when a feature is added to the vectorlayer. It deactivates all drawFeature controls, makes the added feature editable and fires @see viewer.viewercontroller.controller.Event.ON_FEATURE_ADDED
     */
    featureAdded : function (object){
        var feature = this.fromOpenLayersFeature (object.feature);
        for ( var i = 0 ; i < this.drawFeatureControls.length ; i++ ){
            var control = this.drawFeatureControls[i];
            control.deactivate();
        }
        // If no stylehash is set for the feature, set it to the current settings
        if(!object.feature.style){
            object.feature.style = this.getCurrentStyleHash();
        }
        this.editFeature(object.feature);
        this.fireEvent(viewer.viewercontroller.controller.Event.ON_FEATURE_ADDED,this,feature);
    },
    
    /**
     * Puts an openlayersfeature in editmode and fires an event: viewer.viewercontroller.controller.Event.ON_ACTIVE_FEATURE_CHANGED
     * TODO: fix the selecting of a newly added point (after adding another geometry first)
     */
    editFeature : function (feature){
        this.modifyFeature.selectFeature(feature);
        var featureObject = this.fromOpenLayersFeature (feature);
        this.fireEvent(viewer.viewercontroller.controller.Event.ON_ACTIVE_FEATURE_CHANGED,this,featureObject);
    },
    
    /**
     * Converts this feature to a OpenLayersFeature
     * @return The OpenLayerstype feature
     */
    toOpenLayersFeature : function(feature){
        var geom = OpenLayers.Geometry.fromWKT(feature.wktgeom);
        var style = this.frameworkLayer.styleMap.styles["default"];    
        style.label = feature.label;
        var olFeature = new OpenLayers.Feature.Vector(geom,{id: feature.id},{style:style});
        return olFeature;
    },

    /**
     * Helper function: Converts the given OpenLayers Feature to the generic feature.
     * @param openLayersFeature The OpenLayersFeature to be converted
     * @return The generic feature
     */
    fromOpenLayersFeature : function(openLayersFeature){
        var feature = new viewer.viewercontroller.controller.Feature(
        {
            id:openLayersFeature.id,
            wktgeom: openLayersFeature.geometry.toString()
        });
        if(openLayersFeature.style){
            feature.label = openLayersFeature.style.label;
            var color = openLayersFeature.style.fillColor;
            if(color.indexOf("#") !== -1){
                color = color.substring(color.indexOf("#")+1, color.length);
            }
            feature.color = color;
        }
        return feature;
    }, 
    
    setLabel : function (id, label){
        var olFeature = this.getFrameworkLayer().getFeatureById(id);
        if(olFeature){
            olFeature.style.label = label;
            this.reload();
        }
    },
    
    /******** overwrite functions to make use of the mixin functions **********/    
    /**
     * @see viewer.viewercontroller.openlayers.OpenLayersLayer#setVisible
     */
    setVisible: function(vis){
        this.mixins.openLayersLayer.setVisible.call(this,vis);
    },
    /**
     * @see viewer.viewercontroller.openlayers.OpenLayersLayer#setVisible
     */
    getVisible: function(){        
       return this.mixins.openLayersLayer.getVisible.call(this);
    },
    /**
     * @see viewer.viewercontroller.OpenLayers.OpenLayersLayer#setAlpha
     */
    setAlpha: function (alpha){
        this.mixins.openLayersLayer.setAlpha.call(this,alpha);
    },
    /**
     * @see viewer.viewercontroller.OpenLayers.OpenLayersLayer#reload
     */
    reload: function (){
        this.mixins.openLayersLayer.reload.call(this);
    },
    /**
     * @see viewer.viewercontroller.OpenLayers.OpenLayersLayer#addListener
     */
    addListener: function (event,handler,scope){
        this.mixins.openLayersLayer.addListener.call(this,event,handler,scope);
    },
    /**
     * @see viewer.viewercontroller.OpenLayers.OpenLayersLayer#removeListener
     */
    removeListener: function (event,handler,scope){
        this.mixins.openLayersLayer.removeListener.call(this,event,handler,scope);
    },
    /**
     *Get the type of the layer
     */
    getType : function (){
        return this.mixins.openLayersLayer.getType.call(this);
    },
    /**
     * @see viewer.viewercontroller.OpenLayers.OpenLayersLayer#destroy
     */
    destroy: function (){
        this.mixins.openLayersLayer.destroy.call(this);
    }
});

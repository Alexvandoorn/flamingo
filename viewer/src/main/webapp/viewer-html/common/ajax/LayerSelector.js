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
 * LayerSelector
 * A generic component to retrieve the layers
 * @author <a href="mailto:meinetoonen@b3partners.nl">Meine Toonen</a>
 */
Ext.define ("viewer.components.LayerSelector",{
    extend: "viewer.components.Component",
    popupWin:null,
    layerList : null,
    layerArray : null,
    combobox : null,
    div: null,
    // An array of layers whom visibility must be forced in the layerSelector
    forcedLayers : null,
    config: {
        viewerController: new Object(),
        restriction : null,
        layers:null
    }, 
    constructor: function (conf){
        viewer.components.LayerSelector.superclass.constructor.call(this, conf);
        this.initConfig(conf);
        
        this.forcedLayers = new Array();
        var layers = Ext.create('Ext.data.Store', {
            fields: ['layerId', 'title','layer'],
            data : []
        });

        var comboboxConfig = {
            fieldLabel: 'Kies kaartlaag',
            emptyText:'Maak uw keuze',
            store: layers,
            queryMode: 'local',
            displayField: 'title',
            valueField: 'layer',
            listeners :{
                change:{
                    fn: this.changed,
                    scope: this
                }
            }
        };
        if(this.config.div) {
            comboboxConfig.renderTo = this.config.div;
        }
        if(this.config.padding) {
            comboboxConfig.padding = this.config.padding;
        }
        this.combobox = Ext.create('Ext.form.ComboBox', comboboxConfig);
        var requestPath= actionBeans["layerlist"];
        var requestParams = {};
        requestParams[this.config.restriction]= true;
        requestParams["appId"]= appId;
            var me = this;
        if(this.config.layers != null && this.config.layers.length > 0){
            requestParams["layers"]= this.config.layers;
            requestParams["hasConfiguredLayers"]= true;    
        }
        
        Ext.Ajax.request({ 
            url: requestPath, 
            params: requestParams, 
            success: function ( result, request ) {
                me.layerList = Ext.JSON.decode(result.responseText);
                me.initLayers();
            },
            failure: function(a,b,c) {
                Ext.MessageBox.alert("Foutmelding", "Er is een onbekende fout opgetreden waardoor de lijst met kaartlagen niet kan worden weergegeven");
            }
        });
        this.config.viewerController.mapComponent.getMap().addListener(viewer.viewercontroller.controller.Event.ON_LAYER_VISIBILITY_CHANGED, this.layerVisibilityChanged, this);
        return this;
    },
    /**
     * @param forcedLayer the application layer that needs to be forced
     */
    addForcedLayer : function (forcedLayer){
        var dupe = false;
        for ( var i = 0 ; i < this.forcedLayers.length; i++){
            if(this.forcedLayers[i] == forcedLayer){
                dupe = true;
                break;
            }
        }
        if(!dupe){
            this.forcedLayers.push(forcedLayer);
        }
    },
    removeForcedLayer : function (forcedLayer){
        for( var i = this.forcedLayers.length -1 ; i >= 0 ; i--){
            if(this.forcedLayers[i]==forcedLayer){
                this.forcedLayers.splice(i,1);
            }
        }
    },
    initLayers : function (){
        this.layerArray = new Array();
        var visibleLayers = this.config.viewerController.getVisibleLayers();
        for(var i = 0 ; i < this.forcedLayers.length; i++){
            visibleLayers.push(this.forcedLayers[i].id);
        }
        var store = this.combobox.getStore();
        store.removeAll();
        var addedLayers = 0;
        if(this.layerList != null){
            for (var i = 0 ; i < this.layerList.length ;i++){
                var l = this.layerList[i];
                for ( var j = 0 ; j < visibleLayers.length ;j++){
                    //var appLayer = this.config.viewerController.getAppLayerById(visibleLayers[j]);                    
                    if (visibleLayers[j] == l.id || visibleLayers[j] == (""+l.id)){                
                        store.add({
                            layerId: l.id,
                            title: l.alias || l.layerName,
                            layer: l
                        });
                        addedLayers++;
                        break;
                    }
                }
            }
        }
        if(addedLayers === 0) {
            // this.combobox.inputEl.dom.placeholder='Geen kaartlagen beschikbaar';
            this.combobox.setDisabled(true);
        } else {
            // this.combobox.inputEl.dom.placeholder='Maak uw keuze';
            this.combobox.setDisabled(false);
        }
        
        this.fireEvent(viewer.viewercontroller.controller.Event.ON_LAYERSELECTOR_INITLAYERS,store,this);
    },
    changed :function (combobox,appLayer,previousSelected){
        // Retrieve appLayer from config.viewerController. Because the applayers in the comboBox are not the same as in the viewercontroller but copies. So by retrieving the ones
        // from the ViewerController you get the correct appLayer
        var al = null;
        if(appLayer){
            al = this.config.viewerController.getAppLayerById(appLayer.id);
        }
        this.fireEvent(viewer.viewercontroller.controller.Event.ON_LAYERSELECTOR_CHANGE,al,previousSelected,this);
        
    },
    /**
     * Retrieve the AppLayer that is selected.
     */
    getValue : function (){
        // Retrieve appLayer from viewerController. Because the applayers in the comboBox are not the same as in the viewercontroller but copies. So by retrieving the ones
        // from the ViewerController you get the correct appLayer
        var val = this.combobox.getValue();
        if(val){
            var al = this.config.viewerController.getAppLayerById(val.id);
            return al;
        }else{
            return null;
        }
    },
    setValue : function (appLayer, preventEvent){
        this.combobox.setValue(appLayer, preventEvent);
    },
    /**
     * @deprecated use getValue()
     */
    getSelectedAppLayer : function (){
        return this.getValue();
    },
    getExtComponents: function() {
        return [ this.combobox.getId() ];
    },
    layerVisibilityChanged : function (map,object){
        this.initLayers();
    }
});
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
 * Bookmark component
 * @author <a href="mailto:meinetoonen@b3partners.nl">Meine Toonen</a>
 */
Ext.define ("viewer.components.ExtentFilter",{
    extend: "viewer.components.SpatialFilter",
    layers:null,
    config:{
    },
    constructor: function (conf){ 
        this.initConfig(conf);
        this.layers = [];
        this.initializeLayers();
        this.viewerController.addListener(viewer.viewercontroller.controller.Event.ON_SELECTEDCONTENT_CHANGE,this.initializeLayers,this );
        this.viewerController.mapComponent.getMap().addListener(viewer.viewercontroller.controller.Event.ON_FINISHED_CHANGE_EXTENT, this.changedExtent, this);
        return this;
    },
    changedExtent:function(map,obj){
        var extent = obj.extent;
        var polygon = extent.toWKT();
        for(var i = 0 ; i < this.layers.length ;i++){
            this.setFilter(polygon, this.layers[i]);
        }
        var a = 0; 
    },
    initializeLayers : function(){
        this.layers = [];
        var me = this;
        this.viewerController.traverseSelectedContent(Ext.emptyFn, function(appLayer) {
            me.layers.push(appLayer);
        });
    },
    getExtComponents: function() {
        return [ this.form.getId() ];
    }
});
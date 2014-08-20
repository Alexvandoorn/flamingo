describe("Flamingo components tests", function() {
    var viewerController;
    beforeEach(function(){
        viewer.LayoutManager.prototype.getMapId= function(){
            return "test";
        };
        viewerController = new viewer.viewercontroller.ViewerController("openlayers", "test", {
            layout:{
                autoRender: false
            }
        }, {},{});
    });
    describe("Spatial Filter tests", function() {
        var spatialFilter = null;
        beforeEach(function() {
            spatialFilter = Ext.create("viewer.components.SpatialFilter", {
                title: "SpatialFilterTitle",
                iconUrl: "",
                tooltip: "SpatialFilterTooltip",
                viewerController:viewerController,
                layers: [],
                applyDirect: false,
                multiGeometries: true,
                label: "",
                details:{
                    width: 100,
                    height: 100
                }
            });
        });

        it("and so is a spec", function() {
            a = true;

            expect(a).toBe(true);
        });
    });
});
/* 
 * Copyright (C) 2012 B3Partners B.V.
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
 * Custom configuration object for Buffer configuration
 * @author <a href="mailto:roybraam@b3partners.nl">Roy Braam</a>
 */
Ext.define("viewer.components.CustomConfiguration", {
    extend: "viewer.components.SelectionWindowConfig",
    constructor: function(parentId, configObject) {
        if (configObject === null)
            configObject = {};
        configObject.showLabelconfig = true;
        viewer.components.CustomConfiguration.superclass.constructor.call(this, parentId, configObject);
        var checkboxLabelWidth= 55;
        this.form.add([{
                xtype: 'label',
                text: 'Toon \'deel\' knoppen in venster',
                style: {
                    fontWeight: 'bold'
                }
            },{
                xtype : 'container',
                layout : {
                    type: 'table',
                    columns: 3
                },
                items: [
                    {
                        xtype: 'checkbox',
                        boxLabel: 'E-mail',
                        name: 'shareEmail',
                        /*columnWidth : 0.5,*/
                        value: true,
                        inputValue: true,
                        checked: this.configObject.shareEmail != undefined ? this.configObject.shareEmail : false,
                        style: {
                            marginRight: "90px"
                        }
                    },{
                        xtype: 'checkbox',
                        boxLabel: 'Twitter',
                        name: 'shareTwitter',
                        /*columnWidth : 0.5,*/
                        value: true,
                        inputValue: true,
                        checked: this.configObject.shareTwitter != undefined ? this.configObject.shareTwitter : false,
                        labelWidth: checkboxLabelWidth,
                        style: {
                            marginRight: "90px"
                        }
                    },{
                        xtype: 'checkbox',
                        boxLabel: 'LinkedIn',
                        name: 'shareLinkedIn',
                        /*columnWidth : 0.5,*/
                        value: true,
                        inputValue: true,
                        checked: this.configObject.shareLinkedIn != undefined ? this.configObject.shareLinkedIn : false,
                        style: {
                            marginRight: "90px"
                        }
                    },{
                        xtype: 'checkbox',
                        boxLabel: 'Google+',
                        name: 'shareGooglePlus',
                        /*columnWidth : 0.5,*/
                        value: true,
                        inputValue: true,
                        checked: this.configObject.shareGooglePlus != undefined ? this.configObject.shareGooglePlus : false,
                        style: {
                            marginRight: "90px"
                        }
                    },{
                        xtype: 'checkbox',
                        boxLabel: 'Facebook',
                        name: 'shareFacebook',
                        /*columnWidth : 0.5,*/
                        value: true,
                        inputValue: true,
                        checked: this.configObject.shareFacebook != undefined ? this.configObject.shareFacebook : false,
                        style: {
                            marginRight: "90px"
                        }
                    }
                ]
            }
        ]);
    }
});


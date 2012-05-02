<%--
Copyright (C) 2011 B3Partners B.V.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@include file="/WEB-INF/jsp/taglibs.jsp"%>

<stripes:layout-render name="/WEB-INF/jsp/templates/ext.jsp">
    <stripes:layout-component name="head">
        <title>Bewerk Attribuutbron</title>
    </stripes:layout-component>
    <stripes:layout-component name="body">
        <div id="formcontent">
        <p>
            <stripes:errors/>
            <stripes:messages/>
        </p>
            <stripes:form beanclass="nl.b3p.viewer.admin.stripes.AttributeSourceActionBean">
                <c:choose>
                    <c:when test="${actionBean.context.eventName == 'edit'}">
                    <h1 id="headertext">Attribuutbron bewerken</h1>

                    <stripes:hidden name="featureSource" value="${actionBean.featureSource.id}"/>
                    <table class="formtable">
                        <tr>
                            <td>Naam *:</td>
                            <td><stripes:text name="name" maxlength="255" size="30"/></td>
                        </tr>
                        <tr>
                            <td>Bron URL *:</td>
                            <td>
                                <stripes:text name="url" maxlength="255" size="30" disabled="true"/>
                            </td>
                        </tr>
                        <tr>
                            <td>Type *:</td>
                            <td>
                                <stripes:select name="protocol" disabled="true">
                                    <stripes:option value="wfs">WFS</stripes:option>
                                    <stripes:option value="arcgis">ArcGIS Server</stripes:option>
                                    <stripes:option value="arcxml">ArcXml</stripes:option>
                                    <stripes:option value="jdbc">JDBC</stripes:option>
                                </stripes:select>
                            </td>
                        </tr>
                        <tr>
                            <td>Gebruikersnaam:</td>
                            <td><stripes:text name="username"  maxlength="255" size="30"/></td>
                        </tr>
                        <tr>
                            <td>Wachtwoord:</td>
                            <td><stripes-dynattr:password name="password" autocomplete="off" maxlength="255" size="30"/></td>
                        </tr>
                    </table>
                    <div class="submitbuttons">
                        <stripes:submit name="saveEdit" value="Opslaan"/>
                        <stripes:submit name="cancel" value="Annuleren"/>
                    </div>
                </c:when>
                <c:when test="${actionBean.context.eventName == 'newAttributeSource' || actionBean.context.eventName == 'save'}">

                            <h1 id="headertext">Nieuwe attribuutbron toevoegen</h1>
                            <p>
                            <script type="text/javascript">
                                function checkProtocol() {
                                    var protocol = Ext.query("select[name='protocol']")[0].value;

                                    Ext.query("*[class='dbTr']").forEach(function(e) {
                                        e.style.visibility = protocol == "jdbc" ? "visible" : "hidden";
                                    });
                                    Ext.query("*[class='wfsTr']").forEach(function(e) {
                                        e.style.visibility = protocol == "wfs" ? "visible" : "hidden";
                                    });

                                    checkDefaults();
                                }

                                function checkDefaults() {
                                    // XXX impossible to use a Oracle server on PostgreSQL port
                                    // and vice versa, but automatic filling in of port is useful
                                    var dbType = Ext.query("select[name='dbtype']")[0].value;
                                    var port = Ext.query("input[name='port']")[0];
                                    if(dbType == "oracle") {
                                        if(port.value == "" || port.value == "5432") {
                                            port.value = "1521";
                                        }
                                    } else if(dbType == "postgis") {
                                        if(port.value == "" || port.value == "1521") {
                                            port.value = "5432";
                                        }
                                    }
                                }
                                Ext.onReady(checkProtocol);
                            </script>
                            <table class="formtable">
                                <tr>
                                    <td>Type:</td>
                                    <td>
                                        <stripes:select name="protocol" onchange="checkProtocol()" onkeyup="checkProtocol()">
                                            <stripes:option value="jdbc">Database (JDBC)</stripes:option>
                                            <stripes:option value="wfs">WFS</stripes:option>
                                        </stripes:select>
                                    </td>
                                </tr>
                                <tr>
                                    <td>Naam:</td>
                                    <td><stripes:text name="name" maxlength="255" size="30"/></td>
                                </tr>
                                <tr class="wfsTr">
                                    <td>URL:</td>
                                    <td><stripes:text name="url" maxlength="255" size="30"/></td>
                                </tr>
                                <tr class="dbTr">
                                    <td>Database type:</td>
                                    <td>
                                        <stripes:select name="dbtype" onchange="checkDefaults()" onkeyup="checkDefaults()">
                                            <stripes:option value="oracle">Oracle</stripes:option>
                                            <stripes:option value="postgis">PostGIS</stripes:option>
                                        </stripes:select>
                                    </td>
                                </tr>
                                <tr class="dbTr">
                                    <td>Adres database server:</td>
                                    <td>
                                        <stripes:text name="host" maxlength="255" size="30"/>
                                    </td>
                                </tr>
                                <tr class="dbTr">
                                    <td>Poort database server:</td>
                                    <td>
                                        <stripes:text name="port" maxlength="255" size="30"/>
                                    </td>
                                </tr>
                                <tr class="dbTr">
                                    <td>Database:</td>
                                    <td>
                                        <stripes:text name="database" maxlength="255" size="30"/>
                                    </td>
                                </tr>
                                <tr class="dbTr">
                                    <td>Schema:</td>
                                    <td>
                                        <stripes:text name="schema" maxlength="255" size="30"/>
                                    </td>
                                </tr>
                                <tr>
                                    <td>Gebruikersnaam:</td>
                                    <td><stripes:text name="username" maxlength="255" size="30"/></td>
                                </tr>
                                <tr>
                                    <td>Wachtwoord:</td>
                                    <td><stripes-dynattr:password name="password" autocomplete="off" maxlength="255" size="30"/></td>
                                </tr>
                            </table>
                            <div class="submitbuttons">
                                <stripes:submit name="save" value="Opslaan"/>
                                <stripes:submit name="cancel" value="Annuleren"/>
                            </div>
                </c:when>
                <c:when test="${actionBean.context.eventName == 'save' || actionBean.context.eventName == 'saveEdit'}">
                    <script type="text/javascript">
                        var frameParent = getParent();
                        if(frameParent && frameParent.reloadGrid) {
                            frameParent.reloadGrid();
                        }
                    </script>
                    <stripes:submit name="newAttributeSource" value="Nieuwe attribuutbron"/>
                </c:when>
                <c:when test="${actionBean.context.eventName == 'delete'}">
                    <script type="text/javascript">
                        var frameParent = getParent();
                        if(frameParent && frameParent.reloadGrid) {
                            frameParent.reloadGrid();
                        }
                    </script>
                    <stripes:submit name="newAttributeSource" value="Nieuwe attribuutbron"/>
                </c:when>
                <c:otherwise>
                    <script type="text/javascript">
                        var frameParent = getParent();
                        if(frameParent && frameParent.reloadGrid) {
                            frameParent.reloadGrid();
                        }
                    </script>
                    <stripes:submit name="newAttributeSource" value="Nieuwe attribuutbron"/>
                </c:otherwise>
            </c:choose>
        </stripes:form>
        </div>
        <script type="text/javascript">
            Ext.onReady(function() {
                appendPanel('headertext', 'formcontent');
            });
        </script>
    </stripes:layout-component>
</stripes:layout-render>
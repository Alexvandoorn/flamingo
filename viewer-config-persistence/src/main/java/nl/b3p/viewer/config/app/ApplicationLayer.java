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
package nl.b3p.viewer.config.app;

import javax.persistence.*;
import java.util.*;
import nl.b3p.viewer.config.ClobElement;
import nl.b3p.viewer.config.services.AttributeDescriptor;
import nl.b3p.viewer.config.services.FeatureTypeRelation;
import nl.b3p.viewer.config.services.GeoService;
import nl.b3p.viewer.config.services.Layer;
import nl.b3p.viewer.config.services.SimpleFeatureType;
import org.apache.commons.beanutils.BeanUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Matthijs Laan
 */
@Entity
public class ApplicationLayer {
    @Id
    private Long id;

    /**
     * Should this app layer be visible in the application by default
     */    
    private boolean checked;
    
    private Integer selectedIndex;   

    @ManyToOne
    private GeoService service;

    /**
     * Not direct association to a layer but the name of the layer. No foreign
     * key so the GeoService layers can be refreshed without breaking relations.
     * Although the name of a layer is not guaranteed unique, ignore this for
     * now.
     */
    @Basic(optional=false)
    private String layerName;
    
    @ElementCollection
    @Column(name="role_name")
    private Set<String> readers = new HashSet<String>();

    @ElementCollection
    @Column(name="role_name")
    private Set<String> writers = new HashSet<String>();

    @ElementCollection
    @JoinTable(joinColumns=@JoinColumn(name="application_layer"))
    // Element wrapper required because of http://opensource.atlassian.com/projects/hibernate/browse/JPA-11    
    private Map<String,ClobElement> details = new HashMap<String,ClobElement>();

    @ManyToMany(cascade=CascadeType.ALL) // Actually @OneToMany, workaround for HHH-1268    
    @JoinTable(inverseJoinColumns=@JoinColumn(name="attribute_"))
    @OrderColumn(name="list_index")
    private List<ConfiguredAttribute> attributes = new ArrayList<ConfiguredAttribute>();

    //<editor-fold defaultstate="collapsed" desc="getters en setters">
    public List<ConfiguredAttribute> getAttributes() {
        return attributes;
    }
    
    public void setAttributes(List<ConfiguredAttribute> attributes) {
        this.attributes = attributes;
    }
    
    public boolean isChecked() {
        return checked;
    }
    
    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public Integer getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(Integer selectedIndex) {
        this.selectedIndex = selectedIndex;
    }
    
    public Map<String, ClobElement> getDetails() {
        return details;
    }
    
    public void setDetails(Map<String, ClobElement> details) {
        this.details = details;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Set<String> getReaders() {
        return readers;
    }
    
    public void setReaders(Set<String> readers) {
        this.readers = readers;
    }
    
    public Set<String> getWriters() {
        return writers;
    }
    
    public void setWriters(Set<String> writers) {
        this.writers = writers;
    }
    
    
    public GeoService getService() {
        return service;
    }
    
    public void setService(GeoService service) {
        this.service = service;
    }
    
    public String getLayerName() {
        return layerName;
    }
    
    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }
    //</editor-fold>
    
    /**
     * Get all the attributes from this applicationLayer that are from the given 
     * SimpleFeatureType (or the SimpleFeatureType == null, for configured attributes
     * that don't have a SimpleFeatureType set yet)
     * Optional the joined SimpleFeatureType attributes that are joined can be added (includeJoined=true)
     */
    public List<ConfiguredAttribute> getAttributes(SimpleFeatureType sft){
        return getAttributes(sft, false, new ArrayList<ConfiguredAttribute>());
    }
    public List<ConfiguredAttribute> getAttributes(SimpleFeatureType sft, boolean includeJoined){
        return getAttributes(sft, includeJoined, new  ArrayList<ConfiguredAttribute>());
    }
    private List<ConfiguredAttribute> getAttributes(SimpleFeatureType sft,boolean includeJoined, List<ConfiguredAttribute> attri){
        for(ConfiguredAttribute att: this.attributes) {
            if(att.getFeatureType()==null || att.getFeatureType().getId().equals(sft.getId())) {
                if (attri.contains(att)){
                    return attri;
                }
                attri.add(att);
            }
        }
        if (includeJoined){
            for (FeatureTypeRelation rel : sft.getRelations()){
                if (FeatureTypeRelation.JOIN.equals(rel.getType())){
                    attri = getAttributes(rel.getForeignFeatureType(),true,attri);                                        
                }
            }
        }
        return attri;
    }
    public ConfiguredAttribute getAttribute(SimpleFeatureType sft,String name){
         for(ConfiguredAttribute att: attributes) {
            if(att.getAttributeName().equals(name) && 
                    (att.getFeatureType()==null || att.getFeatureType().getId().equals(sft.getId()))) {
                return att;
            }
        }
        return null;
    }
    public String getDisplayName() {
        if(ClobElement.isNotBlank(getDetails().get("titleAlias"))) {
            return getDetails().get("titleAlias").getValue();
        } else {
            Layer l = getService() == null ? null : getService().getLayer(getLayerName());
            if(l != null) {
                return l.getDisplayName();
            } else {
                return getLayerName();
            }
        }
    }

    public JSONObject toJSONObject() throws JSONException {
        return toJSONObject(false, false);
    }
    
    public JSONObject toJSONObject(boolean includeAttributes, boolean includeRelations) throws JSONException {

        JSONObject o = new JSONObject();
        o.put("id", getId());
        o.put("checked", isChecked());
        o.put("layerName", getLayerName());
        if(getService() != null) {
            o.put("serviceId", getService().getId());
        }
        o.put("alias", getDisplayName());

        /* TODO add attribute if writeable according to al.getWriters() */

        if(!getDetails().isEmpty()) {
            JSONObject d = new JSONObject();
            o.put("details", d);
            for(Map.Entry<String,ClobElement> e: getDetails().entrySet()) {
                d.put(e.getKey(), e.getValue().getValue());
            }
        }
        
        if(includeAttributes) {
            addAttributesJSON(o, includeRelations);
        }

        return o;
    }
    
    public void addAttributesJSON(JSONObject json, boolean includeRelations) throws JSONException {
        Layer layer = getService().getSingleLayer(getLayerName());
        Map<String,AttributeDescriptor> featureTypeAttributes = new HashMap<String,AttributeDescriptor>();        
        SimpleFeatureType ft = null;
        if(layer != null) {
            ft = layer.getFeatureType();
            if(ft != null) {
                featureTypeAttributes = makeAttributeDescriptorList(ft);
            }            
        }

        Integer geometryAttributeIndex = null;
        JSONArray jattributes = new JSONArray();
        json.put("attributes", jattributes);

        for(ConfiguredAttribute ca: getAttributes()) {
            JSONObject j = ca.toJSONObject();                
            AttributeDescriptor ad = featureTypeAttributes.get(ca.getFullName());
            if(ad != null) {
                j.put("alias", ad.getAlias());
                j.put("type", ad.getType());

                if(ft != null && ca.getAttributeName().equals(ft.getGeometryAttribute())) {
                    geometryAttributeIndex = jattributes.length();
                }
            }
            jattributes.put(j);
        }

        if(ft != null) {
            json.put("geometryAttribute", ft.getGeometryAttribute());
            if(includeRelations) {
                json.put("relations", getRelationsJSON());
            }
        }
        if(geometryAttributeIndex != null) {
            json.put("geometryAttributeIndex", geometryAttributeIndex);
        }        
    }
    
    public JSONArray getRelationsJSON() throws JSONException {
        JSONArray j = new JSONArray();
        Layer layer = getService().getSingleLayer(getLayerName());
        if(layer != null && layer.getFeatureType() != null) {
            for(FeatureTypeRelation rel: layer.getFeatureType().getRelations()){
                JSONObject jRel = rel.toJSONObject();
                j.put(jRel);
            }
        }
        return j;
    }
    
    /**
     * Makes a list of al the attributeDescriptors of the given FeatureType and
     * all the child FeatureTypes (related by join/relate)
     */
    private Map<String, AttributeDescriptor> makeAttributeDescriptorList(SimpleFeatureType ft) {
        Map<String,AttributeDescriptor> featureTypeAttributes = new HashMap<String,AttributeDescriptor>();
        for(AttributeDescriptor ad: ft.getAttributes()) {
            String name=ft.getId()+":"+ad.getName();
            //stop when already added. Stop a infinite configurated loop
            if (featureTypeAttributes.containsKey(name)){
                return featureTypeAttributes;
            }
            featureTypeAttributes.put(name, ad);
        }
        if (ft.getRelations()!=null){
            for (FeatureTypeRelation rel : ft.getRelations()){
                featureTypeAttributes.putAll(makeAttributeDescriptorList(rel.getForeignFeatureType()));                
            }
        }
        return featureTypeAttributes;
    }    

    ApplicationLayer deepCopy(Map originalToCopy) throws Exception {
        ApplicationLayer copy = (ApplicationLayer) BeanUtils.cloneBean(this);
        originalToCopy.put(this, copy);
        copy.setId(null);
        
        // service reference is not deep copied, of course
        
        copy.setReaders(new HashSet<String>(readers));
        copy.setWriters(new HashSet<String>(writers));
        copy.setDetails(new HashMap<String,ClobElement>(details));
        
        copy.setAttributes( new ArrayList<ConfiguredAttribute>());
        for(ConfiguredAttribute a: attributes) {
            copy.getAttributes().add(a.deepCopy());
        }
        return copy;
    }
    
    @Override
    public String toString() {
        return String.format("Application layer [id=%d, service id=%d, layer=%s]",
                id,
                service == null ? null : service.getId(),
                layerName);
    }
}

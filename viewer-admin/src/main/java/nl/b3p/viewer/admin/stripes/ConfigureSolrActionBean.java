/*
 * Copyright (C) 2013 B3Partners B.V.
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
package nl.b3p.viewer.admin.stripes;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletResponse;
import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.After;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.StrictBinding;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidateNestedProperties;
import nl.b3p.viewer.admin.SolrInitializer;
import nl.b3p.viewer.config.security.Group;
import nl.b3p.viewer.config.services.AttributeDescriptor;
import nl.b3p.viewer.config.services.FeatureSource;
import nl.b3p.viewer.config.services.SimpleFeatureType;
import nl.b3p.viewer.config.services.SolrConfiguration;
import nl.b3p.viewer.config.services.WFSFeatureSource;
import nl.b3p.web.WaitPageStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SpellCheckResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.geotools.data.Query;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opengis.feature.simple.SimpleFeature;
import org.stripesstuff.plugin.waitpage.WaitPage;
import org.stripesstuff.stripersist.Stripersist;

/**
 *
 * @author Meine Toonen
 */
@UrlBinding("/action/configuresolr")
@StrictBinding
@RolesAllowed({Group.ADMIN, Group.REGISTRY_ADMIN})
public class ConfigureSolrActionBean implements ActionBean {
    private static final Log log = LogFactory.getLog(ConfigureSolrActionBean.class);

    private static final String JSP = "/WEB-INF/jsp/services/solrconfig.jsp";
    private static final String EDIT_JSP = "/WEB-INF/jsp/services/editsolrsource.jsp";
    
    private static final String PROTOTYPE_JSP = "/WEB-INF/jsp/services/searchPrototype.jsp";
    
    private List<FeatureSource> featureSources = new ArrayList();
    private List<SimpleFeatureType> featureTypes = new ArrayList();
    private ActionBeanContext context;
    
    @Validate
    @ValidateNestedProperties({
            @Validate(field= "simpleFeatureType"),
            @Validate(field= "name")
    })
    private SolrConfiguration solrConfiguration;
    
    @Validate
    private Long simpleFeatureTypeId;
    
    @Validate
    private Long[] attributes;
    
    private WaitPageStatus status;
    
    @Validate
    private String term;
    
    //<editor-fold defaultstate="collapsed" desc="getters & setters">
    @Override
    public ActionBeanContext getContext() {
        return context;
    }

    @Override
    public void setContext(ActionBeanContext context) {
        this.context = context;
    }

    public SolrConfiguration getSolrConfiguration() {
        return solrConfiguration;
    }

    public void setSolrConfiguration(SolrConfiguration solrConfiguration) {
        this.solrConfiguration = solrConfiguration;
    }

    public List<FeatureSource> getFeatureSources() {
        return featureSources;
    }

    public void setFeatureSources(List<FeatureSource> featureSources) {
        this.featureSources = featureSources;
    }

    public Long[] getAttributes() {
        return attributes;
    }

    public void setAttributes(Long[] attributes) {
        this.attributes = attributes;
    }

    public List<SimpleFeatureType> getFeatureTypes() {
        return featureTypes;
    }

    public void setFeatureTypes(List<SimpleFeatureType> featureTypes) {
        this.featureTypes = featureTypes;
    }

    public Long getSimpleFeatureTypeId() {
        return simpleFeatureTypeId;
    }

    public void setSimpleFeatureTypeId(Long simpleFeatureTypeId) {
        this.simpleFeatureTypeId = simpleFeatureTypeId;
    }

    public WaitPageStatus getStatus() {
        return status;
    }

    public void setStatus(WaitPageStatus status) {
        this.status = status;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }
    
    //</editor-fold>
    
    @DefaultHandler
    public Resolution view() throws SolrServerException {
        SolrServer server=  SolrInitializer.getServerInstance();
        
      /* SolrQuery query = new SolrQuery();
        query.setQuery("*:*");
        query.addSort("textsuggest", SolrQuery.ORDER.asc);
        QueryResponse rsp = server.query(query);
        SolrDocumentList docs = rsp.getResults();*/
        return new ForwardResolution(JSP);
    }

    public Resolution edit() {
        return new ForwardResolution(EDIT_JSP);
    }
    
    
    public Resolution prototype() {
        return new ForwardResolution(PROTOTYPE_JSP);
    }
    
    public Resolution autosuggest() throws JSONException, SolrServerException {
        SolrServer server = SolrInitializer.getServerInstance();
        
        JSONObject obj = new JSONObject();
        JSONObject response = new JSONObject();
        JSONArray respDocs = new JSONArray();
        response.put("docs", respDocs);
        obj.put("response", response);


        SolrQuery query = new SolrQuery();
        query.setQuery(term);
        query.setRequestHandler("/suggest");
        //query.addSort("values", SolrQuery.ORDER.asc);
        QueryResponse rsp = server.query(query);
        SpellCheckResponse sc = rsp.getSpellCheckResponse();
        List<SpellCheckResponse.Suggestion> suggestions = sc.getSuggestions();
        for (SpellCheckResponse.Suggestion suggestion : suggestions) {
            List<String> alternatives = suggestion.getAlternatives();
            for (String alt : alternatives) {
                JSONObject sug = new JSONObject();
                sug.put("suggestion", alt);
                respDocs.put(sug);
            }
        }
        response.put("docs", respDocs);

        return new StreamingResolution("application/json", obj.toString(4));
    }
    
    public Resolution search() throws IOException, JSONException, SolrServerException {
       /* JSONObject json = new JSONObject();
        json.put("success", Boolean.FALSE);
        String url = "http://webkaart.b3p.nl/solr/autosuggest/ac?wt=json&q=";
        URLConnection c = new URL(url + URLEncoder.encode(term, "UTF-8")).openConnection();
        c.connect();*/

        SolrServer server = SolrInitializer.getServerInstance();

        JSONObject obj = new JSONObject();
        JSONObject response = new JSONObject();
        obj.put("response", response);

        SolrQuery query = new SolrQuery();
        query.setQuery(term);
        //query.setParam("qt", "/suggest");
        query.setRequestHandler("/select");
        QueryResponse rsp = server.query(query);
        SolrDocumentList list = rsp.getResults();
        
        JSONArray respDocs = new JSONArray();
        for (SolrDocument solrDocument : list) {
            JSONObject doc = new JSONObject();
            for (String key : solrDocument.keySet()) {
                doc.put(key, solrDocument.get(key));
            }
            respDocs.put(doc);
        }
        
        response.put("docs", respDocs);
        obj.put("success", Boolean.TRUE);
        /*String result = IOUtils.toString(c.getInputStream(), c.getContentEncoding());
        json.put("result", new JSONObject(result));
        json.put("success", Boolean.TRUE);*/
        return new StreamingResolution("application/json", new StringReader(obj.toString(4)));
    }
    
    
    @WaitPage(path = "/WEB-INF/jsp/waitpage.jsp", delay = 2000, refresh = 1000, ajax = "/WEB-INF/jsp/waitpageajax.jsp")
    public Resolution addToIndex() throws InterruptedException {
        org.geotools.data.FeatureSource fs = null;
        try {
            SolrServer server = SolrInitializer.getServerInstance();
            if(server == null){
                throw new Exception("No solr server initialized.");
            }
            EntityManager em = Stripersist.getEntityManager();
            solrConfiguration = em.find(SolrConfiguration.class, solrConfiguration.getId());
            status = new WaitPageStatus();
            status.setCurrentAction("Features ophalen");
            
            status.setProgress(10);
            SimpleFeatureType sft = solrConfiguration.getSimpleFeatureType();
            fs = sft.openGeoToolsFeatureSource();
            
            Query q = new Query();
            if(sft.getFeatureSource() instanceof WFSFeatureSource){
                q.setMaxFeatures(5000);
            }
            FeatureCollection fc = fs.getFeatures(q);
            List<AttributeDescriptor> attributesToIndex =  solrConfiguration.getAttributes();
            List<SolrInputDocument> docs = new ArrayList();
            
            FeatureIterator<SimpleFeature>  iterator = fc.features();
            double size = fc.size();
            double percentagesForAdding = 50;
            double intervalPerDoc = percentagesForAdding/size;
            Double total = (double)status.getProgress();
            try {
                while (iterator.hasNext()) {
                    SimpleFeature feature = iterator.next();
                    SolrInputDocument doc = new SolrInputDocument();
                    for (AttributeDescriptor attr : attributesToIndex) {
                        String attributeName = attr.getName();
                        Object col = feature.getAttribute( attributeName);
                        String field = "values";
                        doc.addField("columns", attributeName);
                        doc.addField(field, col);
                    }
                    doc.addField("id", feature.getID());
                    doc.addField("searchConfig",solrConfiguration.getId());
                    docs.add(doc);
                    total += intervalPerDoc;
                    status.setProgress(total.intValue());
                }
            } finally {
                iterator.close();
            }
            status.setCurrentAction("Features toevoegen aan solr index");
            
            status.setProgress(60);
            server.add(docs);
            server.commit();
            status.setProgress(100);
            status.setFinished(true);
        } catch (Exception ex) {
            log.error("Cannot add configuration to index", ex);
            status.setCurrentAction("Mislukt.");
        }finally{
            if(fs != null && fs.getDataStore() != null){
                fs.getDataStore().dispose();
            }
        }
        return new ForwardResolution(EDIT_JSP);
    }

    public Resolution cancel() {
        return new ForwardResolution(EDIT_JSP);
    }

    public Resolution save() {
        EntityManager em =Stripersist.getEntityManager();
        solrConfiguration.getAttributes().clear();
        for (int i = 0; i < attributes.length; i++) {
            Long attributeId = attributes[i];
            AttributeDescriptor attribute = em.find(AttributeDescriptor.class, attributeId);
            solrConfiguration.getAttributes().add(attribute);
        }
        em.persist(solrConfiguration);
        em.getTransaction().commit();
        
        return new ForwardResolution(EDIT_JSP);
    }

    public Resolution newSearchConfig() {
        solrConfiguration = new SolrConfiguration();
        return new ForwardResolution(EDIT_JSP);
    }
    
    @After(on = {"edit", "save", "newSearchConfig"}, stages = LifecycleStage.BindingAndValidation)
    public void loadLists(){
        
        featureSources = Stripersist.getEntityManager().createQuery("from FeatureSource").getResultList();
        if(solrConfiguration != null && solrConfiguration.getSimpleFeatureType() != null){
            featureTypes = solrConfiguration.getSimpleFeatureType().getFeatureSource().getFeatureTypes();
        }
    }

    public Resolution delete() {
        EntityManager em = Stripersist.getEntityManager();
        solrConfiguration.getAttributes().clear();
        em.remove(solrConfiguration);
        em.getTransaction().commit();
        return new ForwardResolution(EDIT_JSP);
    }

    public Resolution getGridData() throws JSONException {
        EntityManager em =Stripersist.getEntityManager();
        List<SolrConfiguration> configs = em.createQuery("FROM SolrConfiguration").getResultList();
        JSONArray gridRows = new JSONArray();
        for (SolrConfiguration solrConfig : configs) {
            JSONObject config= solrConfig.toJSON();
            config.put("lastprocessed", "1-2-2012");
            gridRows.put(config);
        }
        
        JSONObject json = new JSONObject();
        json.put("totalCount", configs.size());
        json.put("gridrows", gridRows);
        return new StreamingResolution("application/json", json.toString(4));
    }
    
    public Resolution getSearchconfigData() throws JSONException {
        EntityManager em =Stripersist.getEntityManager();
        List<SolrConfiguration> configs = em.createQuery("FROM SolrConfiguration").getResultList();
        JSONArray searchconfigs = new JSONArray();
        for (SolrConfiguration solrConfig : configs) {
            JSONObject config = new JSONObject();
            config.put("id", solrConfig.getId());
            config.put("name", solrConfig.getName());
            searchconfigs.put(config);
        }
        return new StreamingResolution("application/json", searchconfigs.toString(4));
    }
    
    public Resolution getAttributesList() throws JSONException { 
        JSONArray jsonData = new JSONArray();
                
        List<SimpleFeatureType> featureTypes= new ArrayList();
        if(simpleFeatureTypeId != null && simpleFeatureTypeId != -1){
            SimpleFeatureType sft = (SimpleFeatureType)Stripersist.getEntityManager().find(SimpleFeatureType.class, simpleFeatureTypeId);
            if (sft!=null){
                featureTypes.add(sft);
            }
        }else{
            throw new IllegalArgumentException ("No simpleFeatureType id provided");
        }
        
        Session sess = (Session)Stripersist.getEntityManager().getDelegate();
        Criteria c = sess.createCriteria(AttributeDescriptor.class);
        
 
        /* Criteria for the all attribute descriptor ids of the feature types 
         * in featureTypes
         */
        DetachedCriteria c2 = DetachedCriteria.forClass(SimpleFeatureType.class);
        Collection ftIds = new ArrayList<Long>();
        for (SimpleFeatureType sft : featureTypes) {
            ftIds.add(sft.getId());
        }
        c2.add(Restrictions.in("id", ftIds));
        c2.createAlias("attributes", "attr");
        c2.setProjection(Projections.property("attr.id"));

        c.add(org.hibernate.criterion.Property.forName("id").in(c2));
        int rowCount = c.list().size();
 
        
        List<AttributeDescriptor> attrs = c.list();

        for(Iterator<AttributeDescriptor> it = attrs.iterator(); it.hasNext();){
            AttributeDescriptor attr = it.next();
            boolean checked = false;
            if(solrConfiguration != null){
                for (AttributeDescriptor configAttribuut : solrConfiguration.getAttributes()) {
                    if(configAttribuut.getId() == attr.getId()){
                        checked=  true;
                        break;
                    }
                }
            }
            JSONObject j = new JSONObject();
            j.put("id", attr.getId().intValue());
            j.put("alias", attr.getAlias());
            j.put("attribute", attr.getName());
            j.put("checked", checked);
            jsonData.put(j);
        }
        
        final JSONObject grid = new JSONObject();
        grid.put("totalCount", rowCount);
        grid.put("gridrows", jsonData);
    
        return new StreamingResolution("application/json") {
           @Override
           public void stream(HttpServletResponse response) throws Exception {
               response.getWriter().print(grid.toString());
           }
        };
    }

}
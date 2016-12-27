/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_1_0;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.xsd.XSDSchema;
import org.eclipse.xsd.util.XSDResourceImpl;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSDescribeFeatureTypeOutputFormat_ISO;
import org.geoserver.wfs.request.DescribeFeatureTypeRequest;
import org.geoserver.wfs.xml.FeatureTypeSchemaBuilder;
import org.geoserver.wfs.xml.ISOFeatureTypeSchemaBuilder;


public class XmlSchemaEncoder_ISO extends WFSDescribeFeatureTypeOutputFormat_ISO {
    
    /** the catalog */
    Catalog catalog;

    /** the geoserver resource loader */
    GeoServerResourceLoader resourceLoader;
    
    /** schema builder */
    ISOFeatureTypeSchemaBuilder schemaBuilder;

    public XmlSchemaEncoder_ISO(String mimeType, GeoServer gs, ISOFeatureTypeSchemaBuilder schemaBuilder) {
        super(gs, mimeType);
        
       
        this.catalog = gs.getCatalog();
        this.resourceLoader = catalog.getResourceLoader();
        this.schemaBuilder = schemaBuilder;
    }
    
    public XmlSchemaEncoder_ISO(Set<String> mimeTypes, GeoServer gs, ISOFeatureTypeSchemaBuilder schemaBuilder) {
        super(gs, mimeTypes);
        
       
        this.catalog = gs.getCatalog();
        this.resourceLoader = catalog.getResourceLoader();
        this.schemaBuilder = schemaBuilder;
    }

    public String getMimeType(Object value, Operation operation)
        throws ServiceException {
        return getOutputFormat();
        //return "text/xml; subtype=gml/3.1.1";
    }

    protected String getWFSNamespaceURI() {
        return WFS.NAMESPACE;
    }

    protected void write(FeatureTypeInfo[] featureTypeInfos, OutputStream output,
        Operation describeFeatureType) throws IOException {
        
        //hack for SOAP request, when encoding as SOAP response the schema is actually required
        // to be encoded in base64
        if (Dispatcher.REQUEST.get() != null && Dispatcher.REQUEST.get().isSOAP()) {
            
            
            output.write(("<wfs:DescribeFeatureTypeResponse xmlns:wfs='" 
                + getWFSNamespaceURI() + "'>").getBytes());
            
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            doWrite(featureTypeInfos, bout, describeFeatureType);
            output.write(Base64.encodeBase64(bout.toByteArray()));
            
            output.write("</wfs:DescribeFeatureTypeResponse>".getBytes());
        }
        else {
            //normal write
            doWrite(featureTypeInfos, output, describeFeatureType);
        }
    }
    
    protected void doWrite(FeatureTypeInfo[] featureTypeInfos, OutputStream output,
            Operation describeFeatureType) throws IOException {

        //create the schema
        Object request = describeFeatureType.getParameters()[0];
        DescribeFeatureTypeRequest req = DescribeFeatureTypeRequest.adapt(request);
        
        XSDSchema schema = schemaBuilder.build(featureTypeInfos, req.getBaseURL());

        //serialize
        schema.updateElement();
        final String encoding = gs.getSettings().getCharset();
        XSDResourceImpl.serialize(output, schema.getElement(), encoding);
    }
    
    public static class V20 extends XmlSchemaEncoder_ISO {
        static Set<String> MIME_TYPES = new LinkedHashSet<String>();
        static {
            MIME_TYPES.add("text/xml; subtype=gml/3.2");
            MIME_TYPES.add("application/gml+xml; version=3.2");
        }
        public V20(GeoServer gs) {
            super(MIME_TYPES, gs, new ISOFeatureTypeSchemaBuilder.GML32(gs));
        }
        
        @Override
        protected String getWFSNamespaceURI() {
            return org.geotools.wfs.v2_0.WFS.NAMESPACE;
        }
    }
    
    public static class V11 extends XmlSchemaEncoder_ISO {

        public V11(GeoServer gs) {
            super("text/xml; subtype=gml/3.1.1",gs,new ISOFeatureTypeSchemaBuilder.GML3(gs));
        }
        
    }
    
    public static class V10 extends XmlSchemaEncoder_ISO {

        public V10(GeoServer gs) {
            super("XMLSCHEMA", gs, new ISOFeatureTypeSchemaBuilder.GML2(gs));
        }
        
        @Override
        public String getMimeType(Object arg0, Operation arg1) throws ServiceException {
            return "text/xml";
        }
        
    }
}

/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_1_0;

import java.io.Reader;
import java.util.Map;

import javax.xml.namespace.QName;

import org.geoserver.config.GeoServer;
import org.geoserver.ows.XmlRequestReader;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.xml.WFSURIHandler;
import org.geoserver.wfs.xml.WFSXmlUtils;
import org.geoserver.util.EntityResolverProvider;
import org.geotools.util.Version;
import org.geotools.xml.Configuration;
import org.geotools.xml.Parser;

/**
 * Xml reader for wfs 1.1.0 xml requests.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 *
 * TODO: there is too much duplication with the 1.0.0 reader, factor it out.
 */
public class WfsXmlReader_ISO extends XmlRequestReader {
    /**
     * WFs configuration
     */
    WFSInfo wfs;

    /**
     * Xml Configuration
     */
    Configuration configuration;
    
    /**
     * geoserver configuartion
     */
    GeoServer geoServer;

    EntityResolverProvider entityResolverProvider;
    
    public WfsXmlReader_ISO(String element, GeoServer gs, Configuration configuration) {
        this(element, gs, configuration, "wfs3d");        
    }
    
    protected WfsXmlReader_ISO(String element, GeoServer gs, Configuration configuration, String serviceId) {
        super(new QName(org.geoserver.wfs.xml.v1_1_0.WFS.NAMESPACE, element), new Version("1.1.0"),
            serviceId);
        this.geoServer = gs;
        this.wfs = gs.getService( WFSInfo.class );
        this.configuration = configuration;
        this.entityResolverProvider = new EntityResolverProvider(geoServer);
    }
    
    public Object read(Object request, Reader reader, Map kvp) throws Exception {
        //TODO: make this configurable?
        configuration.getProperties().add(Parser.Properties.PARSE_UNKNOWN_ELEMENTS);

        Parser parser = new Parser(configuration);
        parser.setEntityResolver(entityResolverProvider.getEntityResolver());
        
        WFSXmlUtils.initRequestParser(parser, wfs, geoServer, kvp);
        Object parsed = WFSXmlUtils.parseRequest(parser, reader, wfs);
        
        WFSXmlUtils.checkValidationErrors(parser, this);
        
        return parsed;
    }
}

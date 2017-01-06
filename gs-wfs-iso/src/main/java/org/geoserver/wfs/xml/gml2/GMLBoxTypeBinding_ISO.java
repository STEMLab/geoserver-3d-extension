/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.gml2;

import java.net.URI;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.ISOGeometryBuilder;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Subclass of {@link GMLBoxTypeBinding_ISO} that parses srsName and 
 * can inherit the CRS from the containing elements
 * 
 * @author Andrea Aime
 */
public class GMLBoxTypeBinding_ISO extends org.geotools.gml2.iso.bindings.GMLBoxTypeBinding {

    public GMLBoxTypeBinding_ISO(ISOGeometryBuilder gBuilder) {
		super(gBuilder);
		// TODO Auto-generated constructor stub
	}
    
    public GMLBoxTypeBinding_ISO() {
		super(new ISOGeometryBuilder(DefaultGeographicCRS.WGS84_3D));
		// TODO Auto-generated constructor stub
	}

	CoordinateReferenceSystem crs;

    public void setCRS(CoordinateReferenceSystem crs) {
        this.crs = crs;
    }

    @Override
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        Envelope envelope = (Envelope) super.parse(instance, node, value);

        // handle the box CRS
        CoordinateReferenceSystem crs = this.crs;
        if (node.hasAttribute("srsName")) {
            URI srs = (URI) node.getAttributeValue("srsName");
            crs = CRS.decode(srs.toString());
        }
        
        if(crs != null) {
            return ReferencedEnvelope.create(envelope, crs);
        } else {
            return envelope;
        }
    }
}

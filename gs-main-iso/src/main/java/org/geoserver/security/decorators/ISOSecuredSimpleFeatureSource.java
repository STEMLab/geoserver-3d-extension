/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.io.IOException;

import org.geoserver.security.WrapperPolicy;
import org.geotools.data.ISODataUtilities;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

/**
 * Secure version of {@link ISOSecuredFeatureSource}
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class ISOSecuredSimpleFeatureSource extends
        ISOSecuredFeatureSource<SimpleFeatureType, SimpleFeature> implements SimpleFeatureSource {

    protected ISOSecuredSimpleFeatureSource(SimpleFeatureSource delegate, WrapperPolicy policy) {
        super(delegate, policy);
    }
    
    @Override
    public SimpleFeatureCollection getFeatures() throws IOException {
        return ISODataUtilities.simple(super.getFeatures());
    }
    
    @Override
    public SimpleFeatureCollection getFeatures(Filter filter)
            throws IOException {
        return ISODataUtilities.simple(super.getFeatures(filter));
    }
    
    @Override
    public SimpleFeatureCollection getFeatures(Query query)
            throws IOException {
        return ISODataUtilities.simple(super.getFeatures(query));
    }

}

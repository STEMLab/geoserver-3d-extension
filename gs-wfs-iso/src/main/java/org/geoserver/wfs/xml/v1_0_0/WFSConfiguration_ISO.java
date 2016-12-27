package org.geoserver.wfs.xml.v1_0_0;
/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */


import java.util.Map;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.wfs.CatalogFeatureTypeCache;
import org.geoserver.wfs.xml.ISOFeatureTypeSchemaBuilder;
import org.geoserver.wfs.xml.ISOWFSHandlerFactory;
import org.geoserver.wfs.xml.PropertyTypePropertyExtractor;
import org.geoserver.wfs.xml.WFSXmlUtils_ISO;
import org.geoserver.wfs.xml.gml2.GMLBoxTypeBinding_ISO;
import org.geotools.data.DataAccess;
import org.geotools.filter.iso.v1_0.OGCBBOXTypeBinding;
import org.geotools.filter.iso.v1_0.OGCConfiguration_ISO;
import org.geotools.filter.v1_1.OGC;
import org.geotools.geometry.GeometryBuilder;
import org.geotools.gml2.FeatureTypeCache;
import org.geotools.gml2.GML;
import org.geotools.gml2.iso.GMLConfiguration_ISO;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.geotools.xml.Configuration;
import org.geotools.xml.OptionalComponentParameter;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.feature.type.FeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.Parameter;
import org.picocontainer.defaults.SetterInjectionComponentAdapter;

import net.opengis.ows10.Ows10Factory;
import net.opengis.wfs.WfsFactory;

/**
 * Parser configuration for wfs 1.0.
 *
 * @author Justin Deoliveira, The Open Planning Project
 * TODO: this class duplicates a lot of what is is in the 1.1 configuration, merge them
 */
public class WFSConfiguration_ISO extends Configuration {
    /**
     * logger
     */
    static Logger LOGGER = Logging.getLogger( "org.geoserver.wfs");

    
    Catalog catalog;
    ISOFeatureTypeSchemaBuilder schemaBuilder;

    public WFSConfiguration_ISO(Catalog catalog, ISOFeatureTypeSchemaBuilder schemaBuilder, final WFS_ISO wfs) {
        super( wfs );

        this.catalog = catalog;
        this.schemaBuilder = schemaBuilder;

        catalog.addListener(new CatalogListener() {

            public void handleAddEvent(CatalogAddEvent event) {
                if (event.getSource() instanceof FeatureTypeInfo) {
                    reloaded();
                }
            }

            public void handleModifyEvent(CatalogModifyEvent event) {
                if (event.getSource() instanceof DataStoreInfo ||
                    event.getSource() instanceof FeatureTypeInfo || 
                    event.getSource() instanceof NamespaceInfo) {
                    reloaded();
                }
            }

            public void handlePostModifyEvent(CatalogPostModifyEvent event) {
            }

            public void handleRemoveEvent(CatalogRemoveEvent event) {
            }

            public void reloaded() {
                wfs.dispose();
            }
                
        });
        catalog.getResourcePool().addListener(new ResourcePool.Listener() {
            
            public void disposed(FeatureTypeInfo featureType, FeatureType ft) {
            }
            
            public void disposed(CoverageStoreInfo coverageStore, GridCoverageReader gcr) {
            }
            
            public void disposed(DataStoreInfo dataStore, DataAccess da) {
                wfs.dispose();
            }
        });
        
        addDependency(new OGCConfiguration_ISO());
        addDependency(new GMLConfiguration_ISO());
    }

    public Catalog getCatalog() {
      return catalog;
    }

    protected void registerBindings(MutablePicoContainer container) {
      //Types
        container.registerComponentImplementation(WFS_ISO.ALLSOMETYPE, AllSomeTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.DELETEELEMENTTYPE,
            DeleteElementTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.DESCRIBEFEATURETYPETYPE,
            DescribeFeatureTypeTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.EMPTYTYPE, EmptyTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.FEATURECOLLECTIONTYPE,
            FeatureCollectionTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.FEATURESLOCKEDTYPE,
            FeaturesLockedTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.FEATURESNOTLOCKEDTYPE,
            FeaturesNotLockedTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.GETCAPABILITIESTYPE,
            GetCapabilitiesTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.GETFEATURETYPE, GetFeatureTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.GETFEATUREWITHLOCKTYPE,
            GetFeatureWithLockTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.INSERTELEMENTTYPE,
            InsertElementTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.INSERTRESULTTYPE,
            InsertResultTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.LOCKFEATURETYPE, LockFeatureTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.LOCKTYPE, LockTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.NATIVETYPE, NativeTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.PROPERTYTYPE, PropertyTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.QUERYTYPE, QueryTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.STATUSTYPE, StatusTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.TRANSACTIONRESULTTYPE,
            TransactionResultTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.TRANSACTIONTYPE, TransactionTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.UPDATEELEMENTTYPE,
            UpdateElementTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.WFS_LOCKFEATURERESPONSETYPE,
            WFS_LockFeatureResponseTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.WFS_TRANSACTIONRESPONSETYPE,
            WFS_TransactionResponseTypeBinding.class);
    }
    
    public void configureContext(MutablePicoContainer context) {
        super.configureContext(context);

        context.registerComponentInstance(Ows10Factory.eINSTANCE);
        context.registerComponentInstance(WfsFactory.eINSTANCE);
        context.registerComponentInstance(new ISOWFSHandlerFactory(catalog, schemaBuilder));
        context.registerComponentInstance(catalog);
        context.registerComponentImplementation(PropertyTypePropertyExtractor.class);
        
        //TODO: this code is copied from the 1.1 configuration, FACTOR IT OUT!!!
        //seed the cache with entries from the catalog
        context.registerComponentInstance(FeatureTypeCache.class, new CatalogFeatureTypeCache(getCatalog()));
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void configureBindings(Map bindings) {
      //override the GMLAbstractFeatureTypeBinding
        bindings.put(GML.AbstractFeatureType,
            GMLAbstractFeatureTypeBinding.class);
        
        WFSXmlUtils_ISO.registerAbstractGeometryTypeBinding(this, bindings, GML.AbstractGeometryType);
        
        bindings.put(
                GML.BoxType,
            new SetterInjectionComponentAdapter( 
                GML.BoxType, GMLBoxTypeBinding_ISO.class,
                new Parameter[]{ new OptionalComponentParameter(CoordinateReferenceSystem.class)} 
            )
        );
        
        // use setter injection for OGCBBoxTypeBinding to allow an 
        // optional crs to be set in teh binding context for parsing, this crs
        // is set by the binding of a parent element.
        // note: it is important that this component adapter is non-caching so 
        // that the setter property gets updated properly every time
        bindings.put(
            OGC.BBOXType,
            new SetterInjectionComponentAdapter(OGC.BBOXType,
                OGCBBOXTypeBinding.class,
                new Parameter[] { new OptionalComponentParameter(CoordinateReferenceSystem.class) }));
        
    }
}

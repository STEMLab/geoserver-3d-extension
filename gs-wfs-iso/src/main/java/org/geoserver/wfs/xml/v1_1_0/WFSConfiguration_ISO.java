/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_1_0;

import java.util.List;
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
import org.geoserver.config.ConfigurationListenerAdapter;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.ows.xml.v1_0.OWSConfiguration;
import org.geoserver.wfs.CatalogFeatureTypeCache;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.xml.ISOFeatureTypeSchemaBuilder;
import org.geoserver.wfs.xml.ISOWFSHandlerFactory;
import org.geoserver.wfs.xml.PropertyTypePropertyExtractor;
import org.geoserver.wfs.xml.WFSXmlUtils_ISO;
import org.geoserver.wfs.xml.XSQNameBinding;
import org.geoserver.wfs.xml.filter.v1_1.FilterTypeBinding;
import org.geoserver.wfs.xml.filter.v1_1.PropertyNameTypeBinding;
import org.geoserver.wfs.xml.gml3.CircleTypeBinding;
import org.geotools.data.DataAccess;
import org.geotools.filter.iso.v1_1.OGCConfiguration_ISO;
import org.geotools.filter.v1_0.OGCBBOXTypeBinding;
import org.geotools.filter.v1_1.OGC;
import org.geotools.geometry.jts.CurvedGeometryFactory;
import org.geotools.gml2.FeatureTypeCache;
import org.geotools.gml2.SrsSyntax;
import org.geotools.gml3.GML;
import org.geotools.gml3.iso.GMLConfiguration_ISO;
import org.geotools.util.logging.Logging;
import org.geotools.xml.Configuration;
import org.geotools.xml.OptionalComponentParameter;
import org.geotools.xs.XS;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.feature.type.FeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.Parameter;
import org.picocontainer.defaults.SetterInjectionComponentAdapter;

import net.opengis.wfs.WfsFactory;

public class WFSConfiguration_ISO extends Configuration {
    /**
     * logger
     */
    static Logger LOGGER = Logging.getLogger("org.geoserver.wfs");
    
    /**
     * catalog
     */
    protected Catalog catalog;

    /**
     * Schema builder
     */
    protected ISOFeatureTypeSchemaBuilder schemaBuilder;

    public WFSConfiguration_ISO(GeoServer geoServer, ISOFeatureTypeSchemaBuilder schemaBuilder, final WFS_ISO wfs) {
        super( wfs );

        this.catalog = geoServer.getCatalog();
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
        geoServer.addListener(new ConfigurationListenerAdapter() {
            
            public void reloaded() {
                wfs.dispose();
            }
            
            public void handleServiceChange(ServiceInfo service, List<String> propertyNames,
                    List<Object> oldValues, List<Object> newValues) {
                if (service instanceof WFSInfo) {
                    reloaded();
                }
            }
        });
        addDependency(new OGCConfiguration_ISO());
        addDependency(new OWSConfiguration());
        addDependency(new GMLConfiguration_ISO());
        // OGC and OWS add two extra GML configurations in the mix, make sure to configure them
        // all...
        /*CurvedGeometryFactory gf = new CurvedGeometryFactory(Double.MAX_VALUE);
        for (Object configuration : allDependencies()) {
            if (configuration instanceof GMLConfiguration) {
                GMLConfiguration gml = (GMLConfiguration) configuration;
                gml.setGeometryFactory(gf);
            }
        }*/

    }

    public void setSrsSyntax(SrsSyntax srsSyntax) {
        WFSXmlUtils_ISO.setSrsSyntax(this, srsSyntax);
    }

    public SrsSyntax getSrsSyntax() {
        return WFSXmlUtils_ISO.getSrsSyntax(this);
    }
    
    protected void registerBindings(MutablePicoContainer container) {
        //Types
        container.registerComponentImplementation(WFS_ISO.ACTIONTYPE, ActionTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.ALLSOMETYPE, AllSomeTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.BASE_TYPENAMELISTTYPE,
            Base_TypeNameListTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.BASEREQUESTTYPE, BaseRequestTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.DELETEELEMENTTYPE,
            DeleteElementTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.DESCRIBEFEATURETYPETYPE,
            DescribeFeatureTypeTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.FEATURECOLLECTIONTYPE,
            FeatureCollectionTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.FEATURESLOCKEDTYPE,
            FeaturesLockedTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.FEATURESNOTLOCKEDTYPE,
            FeaturesNotLockedTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.FEATURETYPELISTTYPE,
            FeatureTypeListTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.FEATURETYPETYPE, FeatureTypeTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.GETCAPABILITIESTYPE,
            GetCapabilitiesTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.GETFEATURETYPE, GetFeatureTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.GETFEATUREWITHLOCKTYPE,
            GetFeatureWithLockTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.GETGMLOBJECTTYPE,
            GetGmlObjectTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.GMLOBJECTTYPELISTTYPE,
            GMLObjectTypeListTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.GMLOBJECTTYPETYPE,
            GMLObjectTypeTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.IDENTIFIERGENERATIONOPTIONTYPE,
            IdentifierGenerationOptionTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.INSERTEDFEATURETYPE,
            InsertedFeatureTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.INSERTELEMENTTYPE,
            InsertElementTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.INSERTRESULTSTYPE,
            InsertResultTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.LOCKFEATURERESPONSETYPE,
            LockFeatureResponseTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.LOCKFEATURETYPE, LockFeatureTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.LOCKTYPE, LockTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.METADATAURLTYPE, MetadataURLTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.NATIVETYPE, NativeTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.OPERATIONSTYPE, OperationsTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.OPERATIONTYPE, OperationTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.OUTPUTFORMATLISTTYPE,
            OutputFormatListTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.PROPERTYTYPE, PropertyTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.QUERYTYPE, QueryTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.RESULTTYPETYPE, ResultTypeTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.TRANSACTIONRESPONSETYPE,
            TransactionResponseTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.TRANSACTIONRESULTSTYPE,
            TransactionResultsTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.TRANSACTIONSUMMARYTYPE,
            TransactionSummaryTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.TRANSACTIONTYPE, TransactionTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.TYPENAMELISTTYPE,
            TypeNameListTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.UPDATEELEMENTTYPE,
            UpdateElementTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.WFS_CAPABILITIESTYPE,
            WFS_CapabilitiesTypeBinding.class);
        container.registerComponentImplementation(WFS_ISO.XLINKPROPERTYNAME, 
            XlinkPropertyNameBinding.class);

        //cite specific bindings
        container.registerComponentImplementation(
            FeatureReferenceTypeBinding.FeatureReferenceType, 
            FeatureReferenceTypeBinding.class
        );
    }

    public Catalog getCatalog() {
        return catalog;
    }
    
    public void addDependency(Configuration dependency) {
        //override to make public
        super.addDependency(dependency);
    }

    protected void configureContext(MutablePicoContainer context) {
        super.configureContext(context);

        context.registerComponentInstance(WfsFactory.eINSTANCE);
        context.registerComponentInstance(new ISOWFSHandlerFactory(catalog, schemaBuilder));
        context.registerComponentInstance(catalog);
        context.registerComponentImplementation(PropertyTypePropertyExtractor.class);
        context.registerComponentInstance(getSrsSyntax());

        //seed the cache with entries from the catalog
        context.registerComponentInstance(FeatureTypeCache.class, new CatalogFeatureTypeCache(getCatalog()));

        //context.registerComponentInstance(new CurvedGeometryFactory(Double.MAX_VALUE));

    }

    @SuppressWarnings("unchecked")
    @Override
    protected void configureBindings(Map bindings) {
        //register our custom bindings
        bindings.put(OGC.FilterType, FilterTypeBinding.class);
        bindings.put(OGC.PropertyNameType,
            PropertyNameTypeBinding.class);
        bindings.put(GML.CircleType, CircleTypeBinding.class);

        WFSXmlUtils_ISO.registerAbstractGeometryTypeBinding(this, bindings, GML.AbstractGeometryType);

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
        
        // override XSQName binding
        bindings.put(XS.QNAME, XSQNameBinding.class);
    }
}

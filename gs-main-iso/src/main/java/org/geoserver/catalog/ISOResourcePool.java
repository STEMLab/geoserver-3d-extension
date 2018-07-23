/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import javax.measure.converter.UnitConverter;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.geoserver.feature.retype.ISORetypingFeatureSource;
import org.geotools.data.DataAccess;
import org.geotools.data.DataAccessFactory;
import org.geotools.data.DataAccessFactory.Param;
import org.geotools.data.DataAccessFinder;
import org.geotools.data.DataSourceException;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.ISODataUtilities;
import org.geotools.data.Join;
import org.geotools.data.Repository;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.Hints;
import org.geotools.feature.ISOFeatureTypes;
import org.geotools.feature.simple.ISOSimpleFeatureTypeBuilder;
import org.geotools.measure.Measure;
import org.geotools.referencing.CRS;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.vfny.geoserver.global.GeoServerFeatureLocking;
import org.vfny.geoserver.util.DataStoreUtils;
import org.vfny.geoserver.util.ISODataStoreUtils;
import org.xml.sax.EntityResolver;

/**
 * Provides access to resources such as datastores, coverage readers, and 
 * feature types.
 * <p>
 * Provides caches for:
 * <ul>
 * <li>{@link #crsCache} - quick lookup of CoorrdinateReferenceSystem by srs name</li>
 * <li>{@link #dataStoreCache} - live {@link DataAccess} connections. Responsible for maintaining lifecycle with an
 * appropriate call to {@link DataAccess#dispose()} when no longer in use.</li>
 * <li>{@link #featureTypeCache} </li>
 * <li>{@link #featureTypeAttributeCache} </li>
 * <li>{@link #wmsCache} </li>
 * <li>{@link #coverageReaderCache} </li>
 * <li>{@link #hintCoverageReaderCache} </li>
 * <li>{@link #styleCache} </li>
 * </p>
 * 
 * @author Hyung-Gyu Ryoo, Pusan National Univeristy
 */
public class ISOResourcePool extends ResourcePool {
	
	@Override
    public FeatureSource<? extends FeatureType, ? extends Feature> getFeatureSource( FeatureTypeInfo info, Hints hints ) throws IOException {
        DataAccess<? extends FeatureType, ? extends Feature> dataAccess = getDataStore(info.getStore());
        
        // TODO: support aliasing (renaming), reprojection, versioning, and locking for DataAccess
        if (!(dataAccess instanceof DataStore)) {
            return dataAccess.getFeatureSource(info.getQualifiedName());
        }
        
        DataStore dataStore = (DataStore) dataAccess;
        SimpleFeatureSource fs;
        
        // sql view handling
        FeatureTypeCallback initializer = getFeatureTypeInitializer(info, dataAccess);
        if (initializer != null) {
            initializer.initialize(info, dataAccess, null);
        }
                
        //
        // aliasing and type mapping
        //
        final String typeName = info.getNativeName();
        final String alias = info.getName();
        final SimpleFeatureType nativeFeatureType = dataStore.getSchema( typeName );
        final SimpleFeatureType renamedFeatureType = (SimpleFeatureType) getFeatureType( info, false );
        if ( !typeName.equals( alias ) || ISODataUtilities.compare(nativeFeatureType,renamedFeatureType) != 0 ) {
            // rename and retype as necessary
            fs = ISORetypingFeatureSource.getRetypingSource(dataStore.getFeatureSource(typeName), renamedFeatureType);
        } else {
            //normal case
            fs = dataStore.getFeatureSource(info.getQualifiedName());   
        }

        //
        // reprojection
        //
        Boolean reproject = Boolean.TRUE;
        if ( hints != null ) {
            if ( hints.get( REPROJECT ) != null ) {
                reproject = (Boolean) hints.get( REPROJECT );
            }
        }
        
        //get the reprojection policy
        ProjectionPolicy ppolicy = info.getProjectionPolicy();
        
        //if projection policy says to reproject, but calling code specified hint 
        // not to, respect hint
        if ( ppolicy == ProjectionPolicy.REPROJECT_TO_DECLARED && !reproject) {
            ppolicy = ProjectionPolicy.NONE;
        }
        
        List<AttributeTypeInfo> attributes = info.attributes();
        if (attributes == null || attributes.isEmpty()) { 
            return fs;
        } 
        else {
            CoordinateReferenceSystem resultCRS = null;
            GeometryDescriptor gd = fs.getSchema().getGeometryDescriptor();
            CoordinateReferenceSystem nativeCRS = gd != null ? gd.getCoordinateReferenceSystem() : null;
            
            if (ppolicy == ProjectionPolicy.NONE && nativeCRS != null) {
                resultCRS = nativeCRS;
            } else {
                resultCRS = getCRS(info.getSRS());
            }

            // make sure we create the appropriate schema, with the right crs
            // we checked above we are using DataStore/SimpleFeature/SimpleFeatureType (DSSFSFT)
            SimpleFeatureType schema = (SimpleFeatureType) getFeatureType(info);
            try {
                if (!CRS.equalsIgnoreMetadata(resultCRS, schema.getCoordinateReferenceSystem()))
                    schema = ISOFeatureTypes.transform(schema, resultCRS);
            } catch (Exception e) {
                throw new DataSourceException(
                        "Problem forcing CRS onto feature type", e);
            }

            //
            // versioning
            //
            try {
                // only support versioning if on classpath
                if (VERSIONING_FS != null && GS_VERSIONING_FS != null && VERSIONING_FS.isAssignableFrom( fs.getClass() ) ) {
                    //class implements versioning, reflectively create the versioning wrapper
                    try {
                    Method m = GS_VERSIONING_FS.getMethod( "create", VERSIONING_FS, 
                        SimpleFeatureType.class, Filter.class, CoordinateReferenceSystem.class, int.class );
                        return (FeatureSource) m.invoke(null, fs, schema, info.filter(),
                        resultCRS, info.getProjectionPolicy().getCode());
                    }
                    catch( Exception e ) {
                        throw new DataSourceException(
                                "Creation of a versioning wrapper failed", e);
                    }
                }
            } catch( ClassCastException e ) {
                //fall through
            } 

            //joining, check for join hint which requires us to create a shcema with some additional
            // attributes
            if (hints != null && hints.containsKey(JOINS)) {
                List<Join> joins = (List<Join>) hints.get(JOINS);
                ISOSimpleFeatureTypeBuilder typeBuilder = new ISOSimpleFeatureTypeBuilder();
                typeBuilder.init(schema);
                
                for (Join j : joins) {
                    String attName = j.getAlias() != null ? j.getAlias() : j.getTypeName();
                    typeBuilder.add(attName, SimpleFeature.class);
                }
                schema = typeBuilder.buildFeatureType();
            }

            //return a normal 
            return GeoServerFeatureLocking.create(fs, schema, info.filter(), resultCRS, info
                    .getProjectionPolicy().getCode(), getTolerance(info), info.getMetadata());
        }
    }
	

    /**
     * Returns the underlying resource for a DataAccess, caching the result.
     * <p>
     * In the result of the resource not being in the cache {@link DataStoreInfo#getConnectionParameters()}
     * is used to create the connection.
     * </p>
     * @param info DataStoreMeta providing id used for cache lookup (and connection paraemters if a connection is needed)
     * @throws IOException Any errors that occur connecting to the resource.
     */
	@Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public DataAccess<? extends FeatureType, ? extends Feature> getDataStore( DataStoreInfo info ) throws IOException {
        
        DataStoreInfo expandedStore = clone(info, true);
        
        DataAccess<? extends FeatureType, ? extends Feature> dataStore = null;
        try {
            String id = info.getId();
            dataStore = dataStoreCache.get(id);
            if ( dataStore == null ) {
                synchronized (dataStoreCache) {
                    dataStore = dataStoreCache.get( id );
                    if ( dataStore == null ) {
                        //create data store
                        Map<String, Serializable> connectionParameters = expandedStore.getConnectionParameters();
                        
                        // call this method to execute the hack which recognizes 
                        // urls which are relative to the data directory
                        // TODO: find a better way to do this
                        connectionParameters = ISOResourcePool.getParams(connectionParameters, catalog.getResourceLoader() );
                        
                        // obtain the factory
                        DataAccessFactory factory = null;
                        try {
                            factory = getDataStoreFactory(info);
                        } catch(IOException e) {
                            throw new IOException("Failed to find the datastore factory for " + info.getName() 
                                    + ", did you forget to install the store extension jar?");
                        }
                        if (factory == null) {
                            throw new IOException("Failed to find the datastore factory for "
                                    + info.getName()
                                    + ", did you forget to install the store extension jar?");
                        }
                        Param[] params = factory.getParametersInfo();
                        
                        //ensure that the namespace parameter is set for the datastore
                        if (!connectionParameters.containsKey( "namespace") && params != null) {
                            //if we grabbed the factory, check that the factory actually supports
                            // a namespace parameter, if we could not get the factory, assume that
                            // it does
                            boolean supportsNamespace = true;
                            supportsNamespace = false;
                            
                            for ( Param p : params ) {
                                if ( "namespace".equalsIgnoreCase( p.key ) ) {
                                    supportsNamespace = true;
                                    break;
                                }
                            }
                            
                            if ( supportsNamespace ) {
                                WorkspaceInfo ws = info.getWorkspace();
                                NamespaceInfo ns = info.getCatalog().getNamespaceByPrefix( ws.getName() );
                                if ( ns == null ) {
                                    ns = info.getCatalog().getDefaultNamespace();
                                }
                                if ( ns != null ) {
                                    connectionParameters.put( "namespace", ns.getURI() );
                                }    
                            }
                        }
                        
                        // see if the store has a repository param, if so, pass the one wrapping
                        // the store
                        if(params != null) {
                            for ( Param p : params ) {
                                if(Repository.class.equals(p.getType())) {
                                    connectionParameters.put(p.getName(), repository);
                                }
                            }
                        }
                        
                        // see if the store has a entity resolver param, if so, pass it down
                        EntityResolver resolver = getEntityResolver();
                        if(resolver != null && params != null) {
                            for ( Param p : params ) {
                                if(EntityResolver.class.equals(p.getType())) {
                                    if(!(resolver instanceof Serializable)) {
                                        resolver = new SerializableEntityResolver(resolver);
                                    }
                                    connectionParameters.put(p.getName(), (Serializable) resolver);
                                }
                            }
                        }
                        
                        dataStore = ISODataStoreUtils.getDataAccess(connectionParameters);
                        if (dataStore == null) {
                            /*
                             * Preserve DataStore retyping behaviour by calling
                             * DataAccessFinder.getDataStore after the call to
                             * DataStoreUtils.getDataStore above.
                             * 
                             * TODO: DataAccessFinder can also find DataStores, and when retyping is
                             * supported for DataAccess, we can use a single mechanism.
                             */
                            dataStore = DataAccessFinder.getDataStore(connectionParameters);
                        }
                        
                        if ( dataStore == null ) {
                            throw new NullPointerException("Could not acquire data access '" + info.getName() + "'");
                        }
                        
                        // cache only if the id is not null, no need to cache the stores
                        // returned from un-saved DataStoreInfo objects (it would be actually
                        // harmful, NPE when trying to dispose of them)
                        if(id != null) {
                            dataStoreCache.put( id, dataStore );
                        }
                    }
                } 
            }
            
            return dataStore;
        } catch (Exception e) {
            // if anything goes wrong we have to clean up the store anyways
            if(dataStore != null) {
                try {
                    dataStore.dispose();
                } catch(Exception ex) {
                    // fine, we had to try
                }
            }
            if(e instanceof IOException) {
                throw (IOException) e;
            } else {
                throw (IOException) new IOException().initCause(e);
            }
        }
    }
	
	@Override
    public DataAccessFactory getDataStoreFactory( DataStoreInfo info ) throws IOException {
        DataAccessFactory factory = null;
        
        DataStoreInfo expandedStore = clone(info, true);
        
        if ( info.getType() != null ) {
            factory = ISODataStoreUtils.aquireFactory( expandedStore.getType() );    
        }

        if ( factory == null && expandedStore.getConnectionParameters() != null ) {
            Map<String, Serializable> params = getParams( expandedStore.getConnectionParameters(), catalog.getResourceLoader() );
            factory = ISODataStoreUtils.aquireFactory( params);    
        }
   
        return factory;
    }
	
	private Double getTolerance(FeatureTypeInfo info) {
        // if no curved geometries, do not bother computing a tolerance
        if (!info.isCircularArcPresent()) {
            return null;
        }

        // get the measure, if null, no linearization tolerance is available
        Measure mt = info.getLinearizationTolerance();
        if (mt == null) {
            return null;
        }

        // if the user did not specify a unit of measure, we use it as an absolute value
        if (mt.getUnit() == null) {
            return mt.doubleValue();
        }

        // should not happen, but let's cover all our bases
        CoordinateReferenceSystem crs = info.getCRS();
        if (crs == null) {
            return mt.doubleValue();
        }

        // let's get the target unit
        SingleCRS horizontalCRS = CRS.getHorizontalCRS(crs);
        Unit targetUnit;
        if (horizontalCRS != null) {
            // leap of faith, the first axis is an horizontal one (
            targetUnit = getFirstAxisUnit(horizontalCRS.getCoordinateSystem());
        } else {
            // leap of faith, the first axis is an horizontal one (
            targetUnit = getFirstAxisUnit(crs.getCoordinateSystem());
        }

        if ((targetUnit != null && targetUnit == NonSI.DEGREE_ANGLE)
                || horizontalCRS instanceof GeographicCRS || crs instanceof GeographicCRS) {
            // assume we're working against a type of geographic crs, must estimate the degrees
            // equivalent
            // to the measure, we are going to use a very rough estimate (cylindrical earth model)
            // TODO: maybe look at the layer bbox and get a better estimate computed at the center
            // of the bbox
            UnitConverter converter = mt.getUnit().getConverterTo(SI.METER);
            double tolMeters = converter.convert(mt.doubleValue());
            return tolMeters * OGC_METERS_TO_DEGREES;
        } else if (targetUnit != null && targetUnit.isCompatible(SI.METER)) {
            // ok, we assume the target is not a geographic one, but we might
            // have to convert between meters and feet maybe
            UnitConverter converter = mt.getUnit().getConverterTo(targetUnit);
            return converter.convert(mt.doubleValue());
        } else {
            return mt.doubleValue();
        }
    }
	
    private Unit<?> getFirstAxisUnit(CoordinateSystem coordinateSystem) {
        if (coordinateSystem == null || coordinateSystem.getDimension() > 0) {
            return null;
        }
        return coordinateSystem.getAxis(0).getUnit();
    }
	
}

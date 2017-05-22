/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.request;

import net.opengis.wfs.DeleteElementType;
import net.opengis.wfs.WfsFactory;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.wfs.request.Insert.WFS11;

/**
 * Delete element in a Transaction request.
 *  
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class Delete3D extends TransactionElement {

    protected Delete3D(EObject adaptee) {
        super(adaptee);
    }

    public static class WFS11 extends Delete3D {
        public WFS11(EObject adaptee) {
            super(adaptee);
        }

        public static DeleteElementType unadapt(Delete3D delete) {
            DeleteElementType de = WfsFactory.eINSTANCE.createDeleteElementType();
            de.setHandle(delete.getHandle());
            de.setTypeName(delete.getTypeName());
            de.setFilter(delete.getFilter());
            return de;
        }
    }
    
    public static class WFS20 extends Delete3D {
        public WFS20(EObject adaptee) {
            super(adaptee);
        }
    }

}

/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geoserver.web.wicket.GeoServerDataProvider;

class ISOAttributesProvider extends GeoServerDataProvider<ISOAttributeDescription> {

    /** serialVersionUID */
    private static final long serialVersionUID = -1478240785822735763L;

    List<ISOAttributeDescription> attributes = new ArrayList<ISOAttributeDescription>();

    static final Property<ISOAttributeDescription> NAME = new BeanProperty<ISOAttributeDescription>(
            "name", "name");

    static final Property<ISOAttributeDescription> BINDING = new BeanProperty<ISOAttributeDescription>(
            "binding", "binding");

    static final Property<ISOAttributeDescription> NULLABLE = new BeanProperty<ISOAttributeDescription>(
            "nullable", "nullable");

    static final Property<ISOAttributeDescription> SIZE = new BeanProperty<ISOAttributeDescription>(
            "size", "size");

    static final Property<ISOAttributeDescription> CRS = new BeanProperty<ISOAttributeDescription>(
            "crs", "crs");
    
    static final PropertyPlaceholder<ISOAttributeDescription> UPDOWN = new PropertyPlaceholder<ISOAttributeDescription>("upDown");

    public ISOAttributesProvider() {
    }

    public void addNewAttribute(ISOAttributeDescription attribute) {
        attributes.add(attribute);
    }

    @Override
    protected List<ISOAttributeDescription> getItems() {
        return attributes;
    }

    @Override
    protected List<Property<ISOAttributeDescription>> getProperties() {
        return Arrays.asList(NAME, BINDING, NULLABLE, SIZE, CRS, UPDOWN);
    }

    public void removeAll(List<ISOAttributeDescription> removed) {
        this.attributes.removeAll(removed);
    }

    public boolean isFirst(ISOAttributeDescription attribute) {
        return attributes.get(0).equals(attribute);
    }
    
    public boolean isLast(ISOAttributeDescription attribute) {
        return attributes.get(attributes.size() - 1).equals(attribute);
    }
    
    public void moveUp(ISOAttributeDescription attribute) {
        int idx = attributes.indexOf(attribute);
        attributes.remove(idx);
        attributes.add(idx - 1, attribute);
    }
    
    public void moveDown(ISOAttributeDescription attribute) {
        int idx = attributes.indexOf(attribute);
        attributes.remove(idx);
        attributes.add(idx + 1, attribute);
    }

    public List<ISOAttributeDescription> getAttributes() {
        return attributes;
    }

}

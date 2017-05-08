/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.SelectionRemovalLink;
import org.geoserver.web.wicket.GeoServerDialog;

/**
 * Page listing all the available stores. Follows the usual filter/sort/page approach, provides ways
 * to bulk delete stores and to add new ones
 * 
 * @see ISOStorePanel
 */
@SuppressWarnings("serial")
public class ISOStorePage extends GeoServerSecuredPage {
    ISOStoreProvider provider = new ISOStoreProvider();

    ISOStorePanel table;
    
    SelectionRemovalLink removal;

    GeoServerDialog dialog;

    public ISOStorePage() {
        // the table, and wire up selection change
        table = new ISOStorePanel("table2", provider, true) {
            @Override
            protected void onSelectionUpdate(AjaxRequestTarget target) {
                removal.setEnabled(table.getSelection().size() > 0);
                target.add(removal);
            }  
        };
        table.setOutputMarkupId(true);
        add(table);
        
        // the confirm dialog
        add(dialog = new GeoServerDialog("dialog2"));
        setHeaderPanel(headerPanel());
    }
    
    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header2", this);
        
        // the add button
        header.add(new BookmarkablePageLink("addNew2", ISONewDataPage.class));
        
        // the removal button
        header.add(removal = new SelectionRemovalLink("removeSelected2", table, dialog));
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);
        
        return header;
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
}

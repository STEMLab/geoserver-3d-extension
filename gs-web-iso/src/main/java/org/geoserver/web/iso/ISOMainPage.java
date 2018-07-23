/**
 * 
 */
package org.geoserver.web.iso;

import org.apache.wicket.markup.html.basic.Label;
import org.geoserver.web.GeoServerBasePage;

/**
 * @author hgryoo
 *
 */
public class ISOMainPage extends GeoServerBasePage {
	public ISOMainPage() {
	       add( new Label( "hellolabel", "Hello World!") );
	}
}

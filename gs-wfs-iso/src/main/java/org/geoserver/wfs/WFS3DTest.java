/**
 * 
 */
package org.geoserver.wfs;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author hgryoo
 *
 */
public class WFS3DTest {
	
	public void GetFeature(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		response.getOutputStream().write( "Hello World".getBytes() );
	}
}

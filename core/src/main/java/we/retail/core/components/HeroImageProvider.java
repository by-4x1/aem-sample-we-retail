/*******************************************************************************
 * Copyright 2016 Adobe Systems Incorporated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package we.retail.core.components;

import java.lang.String;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import com.day.cq.dam.commons.util.DamUtil;
import com.adobe.cq.sightly.WCMUsePojo;


/**
 * Hero Image data provider
 */
public class HeroImageProvider extends WCMUsePojo {

    private static final String PROP_DC_TITLE = "dc:title";



	private static final String PROP_JCR_TITLE = "jcr:title";



	public static final Logger log = LoggerFactory.getLogger( HeroImageProvider.class );

    

    public static final String PROP_FULL_WIDTH = "useFullWidth";
    public static final String PROP_KEEP_RATIO = "keepRatio";

    private static final List<String> META_KEYS = Arrays.asList(
                                      "dam:Bitsperpixel", "dam:MIMEtype",
                                      "dc:description", PROP_DC_TITLE,
                                      PROP_JCR_TITLE, "smp:CreatorTool"
                                      );

    private String classList = "";
    private Image image = null;

    /**
     * default constructor
     */
    public HeroImageProvider() {
    	
    }

    @Override
    public void activate() throws Exception {
        classList = getClassList();
        image = getImage();
    }

    /**
     * @return Hero Image CSS style names
     */
    public String getClassList() {
        if (classList != null) {
            return classList;
        }
        ValueMap properties = getProperties();
        classList = "we-HeroImage";
        if ("true".equals(properties.get(PROP_FULL_WIDTH, ""))) {
            classList += " width-full";
        }
        if ("true".equals(properties.get(PROP_KEEP_RATIO, ""))) {
            classList += " ratio-16by9";
        }
        return classList;
    }

    /**
     * @return the image if configured
     */
    public Image getImage() {
        if (image != null) {
            return image;
        }

        Map<String, String> metaVals = getImageMetaValues();
        String title = getImageTitle( metaVals );

        String escapedResourcePath = Text.escapePath( getResource().getPath() );
        long lastModifiedDate = getLastModifiedDate( getProperties() );
        String src = getRequest().getContextPath() + escapedResourcePath + ".img.jpeg" +
                (!getWcmMode().isDisabled() && lastModifiedDate > 0 ? "/" + lastModifiedDate + ".jpeg" : "");

        image = new Image( src, title );
        return image;
    }

    /**
     * @return image meta values
     */
    private Map<String, String> getImageMetaValues() {
        ValueMap properties = getProperties();
        String filePath = StringUtils.trimToNull( properties.get( "fileReference", String.class ) );
        ResourceResolver resourceResolver = getResourceResolver();
        Resource imageResource = (filePath != null ? resourceResolver.getResource( filePath ) : null);
        String imagePath = ( imageResource != null ? imageResource.getPath() : null );
        String imageName = ( imageResource != null ? imageResource.getName() : null );
        Asset imageAsset = ( imageResource != null ? DamUtil.resolveToAsset(imageResource) : null );

        Map<String,String> metaVals = new TreeMap<String,String>();
        for ( String key : META_KEYS ) {
            String val = ( imageAsset != null ? imageAsset.getMetadataValue( key ) : null );
            if ( val == null ) {
                log.info( "Meta '{}' in '{}' not found.", key, imagePath );
                continue;
            } // val 
            metaVals.put( key, val );
        } // key
        log.info( "Got hero image '{}', path '{}', meta vals {} .",
                  imageName, imagePath, metaVals );

        return metaVals;
    }

    /**
     * @param metaVals the image meta values
     * @return image title, if found
     */
    private String getImageTitle( Map<String,String> metaVals ) {
        String title = null;
        List<String> title_keys = Arrays.asList( 
                PROP_JCR_TITLE, PROP_DC_TITLE 
                );
        for ( String key : title_keys ) {
            String val = metaVals.get(key);
            if ( val == null ) {
                continue;
            }  // val

            title = val;
            log.debug( "Found title '{}' from '{}'.", title, key );
            break;
        } // key

        return title;
    }
    
    
    /**
     * image POJO
     */
    public class Image {
        private String src;
        private String title;

        public Image(String src, String title) {
            this.src = src;
            this.title = title;
            log.info( "Use hero image src '{}', title '{}'.", src, title );
        }

        /**
         * @return the image source url
         */
        public String getSrc() {
            return src;
        }

        /**
         * @return the image title
         */
        public String getTitle() {
               return title;
        }

    }  // image

    /**
     * @param properties the JCR properties
     * @return the last modified date, or created date
     */
    private long getLastModifiedDate(ValueMap properties) {
        long lastMod = 0L;
        if (properties.containsKey(JcrConstants.JCR_LASTMODIFIED)) {
            lastMod = properties.get(JcrConstants.JCR_LASTMODIFIED, Calendar.class).getTimeInMillis();
        } else if (properties.containsKey(JcrConstants.JCR_CREATED)) {
            lastMod = properties.get(JcrConstants.JCR_CREATED, Calendar.class).getTimeInMillis();
        }
        return lastMod;
    }

	
}

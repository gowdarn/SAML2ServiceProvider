/* 
 * Copyright 2015 Tirasa.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.tirasa.saml.context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.opensaml.Configuration;
import org.opensaml.DefaultBootstrap;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.parse.BasicParserPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class SPContextListener implements ServletContextListener {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(SPContextListener.class);

    private static final String IDP_FOLDER = "/tmp";

    static {
        try {
            DefaultBootstrap.bootstrap();
        } catch (ConfigurationException e) {
            log.error("Error initializing SAML", e);
        }
    }

    private final BasicParserPool ppool;

    private final COT cot = COT.getInstance();

    public SPContextListener() {
        ppool = new BasicParserPool();
        ppool.setNamespaceAware(true);
        cot.setSp(new SP());
    }

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        final File idpFolder = new File(IDP_FOLDER);
        if (!idpFolder.isDirectory() || idpFolder.list().length == 0) {
            log.info("No configured IdPs");
        } else {
            for (File idp : idpFolder.listFiles(new FilenameFilter() {

                @Override
                public boolean accept(final File dir, final String name) {
                    return name.endsWith(".xml");
                }
            })) {
                log.info("Process IdP file descriptor {}", idp.getAbsolutePath());
                try {
                    final EntityDescriptor descriptor = unmarshall(new FileInputStream(idp));
                    cot.addIdP(descriptor.getEntityID(), new IdP(descriptor));
                } catch (Exception ignore) {
                    log.debug("Error loading IdP {}", idp, ignore);
                    log.info("Invalid IdP {}", idp);
                }
            }
        }
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        // do nothing
    }

    private EntityDescriptor unmarshall(final InputStream is) throws Exception {
        // Parse metadata file
        final Element metadata = ppool.parse(is).getDocumentElement();
        // Get apropriate unmarshaller
        final Unmarshaller unmarshaller = Configuration.getUnmarshallerFactory().getUnmarshaller(metadata);
        // Unmarshall using the document root element, an EntitiesDescriptor in this case
        return EntityDescriptor.class.cast(unmarshaller.unmarshall(metadata));
    }
}

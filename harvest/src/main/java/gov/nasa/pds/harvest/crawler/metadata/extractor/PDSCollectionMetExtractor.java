// Copyright 2006-2010, by the California Institute of Technology.
// ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
// Any commercial use must be negotiated with the Office of Technology Transfer
// at the California Institute of Technology.
//
// This software is subject to U. S. export control laws and regulations
// (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the extent that the software
// is subject to U.S. export control laws and regulations, the recipient has
// the responsibility to obtain export licenses or other export authority as
// may be required before exporting such information to foreign countries or
// providing access to foreign nationals.
//
// $Id$
package gov.nasa.pds.harvest.crawler.metadata.extractor;

import gov.nasa.jpl.oodt.cas.metadata.Metadata;
import gov.nasa.jpl.oodt.cas.metadata.exceptions.MetExtractionException;
import gov.nasa.pds.harvest.constants.Constants;
import gov.nasa.pds.harvest.inventory.InventoryEntry;
import gov.nasa.pds.harvest.inventory.InventoryReaderException;
import gov.nasa.pds.harvest.inventory.InventoryTableReader;
import gov.nasa.pds.harvest.inventory.ReferenceEntry;
import gov.nasa.pds.harvest.logging.ToolsLevel;
import gov.nasa.pds.harvest.logging.ToolsLogRecord;
import gov.nasa.pds.harvest.util.XMLExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Class to extract metadata from a PDS Collection file.
 *
 * @author mcayanan
 *
 */
public class PDSCollectionMetExtractor extends PDSMetExtractor {
    /** Logger object. */
    private static Logger log = Logger.getLogger(
            PDSCollectionMetExtractor.class.getName());

    /** XPath to get the associaton type. */
    public static final String ASSOCIATION_TYPE_XPATH = "//*[starts-with("
        + "name(),'Inventory')]/reference_association_type";

    /**
     * Constructor.
     *
     * @param config The configuration for the metadata extraction.
     */
    public PDSCollectionMetExtractor(PDSMetExtractorConfig config) {
        super(config);
    }

    /**
     * Extract the metadata
     *
     * @param product A PDS4 collection file
     * @return a class representation of the extracted metadata
     *
     */
    public Metadata extractMetadata(File product)
    throws MetExtractionException {
        Metadata metadata = new Metadata();
        String objectType = "";
        String logicalID = "";
        String version = "";
        String title = "";
        String associationType = "";
        XMLExtractor extractor = new XMLExtractor();
        try {
            extractor.parse(product);
        } catch (Exception e) {
            throw new MetExtractionException("Parse failure: "
                    + e.getMessage());
        }
        try {
            objectType = extractor.getValueFromDoc(
                    Constants.coreXpathsMap.get(Constants.OBJECT_TYPE));
            logicalID = extractor.getValueFromDoc(
                    Constants.coreXpathsMap.get(Constants.LOGICAL_ID));
            version = extractor.getValueFromDoc(
                    Constants.coreXpathsMap.get(Constants.PRODUCT_VERSION));
            title = extractor.getValueFromDoc(
                    Constants.coreXpathsMap.get(Constants.TITLE));
            associationType =
                extractor.getValueFromDoc(ASSOCIATION_TYPE_XPATH);
        } catch (XPathExpressionException x) {
            //TODO: getMessage() doesn't always return a message
            throw new MetExtractionException(x.getMessage());
        }
        if (!"".equals(logicalID)) {
            metadata.addMetadata(Constants.LOGICAL_ID, logicalID);
        }
        if (!"".equals(version)) {
            metadata.addMetadata(Constants.PRODUCT_VERSION, version);
        }
        if (!"".equals(title)) {
            metadata.addMetadata(Constants.TITLE, title);
        }
        if (!"".equals(objectType)) {
            metadata.addMetadata(Constants.OBJECT_TYPE, objectType);
        }
        if (!"".equals(objectType)) {
            String xpath = "";
            try {
                xpath = Constants.IDENTIFICATION_AREA_XPATH + "/*";
                NodeList list = extractor.getNodesFromDoc(xpath);
                for (int i = 0; i < list.getLength(); i++) {
                    Node node = list.item(i);
                    if (!metadata.containsKey(node.getLocalName())) {
                        metadata.addMetadata(node.getNodeName(),
                            node.getTextContent());
                    }
                }
            } catch (XPathExpressionException xe) {
                throw new MetExtractionException("Bad XPath Expression: "
                        + xpath);
            }
        }
        List<ReferenceEntry> refEntries = new ArrayList<ReferenceEntry>();
        try {
            InventoryTableReader reader = new InventoryTableReader(product);
            for (InventoryEntry entry = reader.getNext(); entry != null;) {
                ReferenceEntry re = new ReferenceEntry();
                String identifier = entry.getLidvid();
                //Check for a LID or LIDVID
                if (identifier.indexOf("::") != -1) {
                    re.setLogicalID(identifier.split("::")[0]);
                    re.setVersion(identifier.split("::")[1]);
                } else {
                    re.setLogicalID(identifier);
                }
                re.setAssociationType(associationType);
                refEntries.add(re);
                entry = reader.getNext();
             }
        } catch (InventoryReaderException ire) {
            throw new MetExtractionException(ire.getMessage());
        }
        if (refEntries.size() == 0) {
            log.log(new ToolsLogRecord(ToolsLevel.INFO,
                    "No associations found.", product));
        } else {
            metadata.addMetadata(Constants.REFERENCES, refEntries);
        }

        return metadata;
    }
}

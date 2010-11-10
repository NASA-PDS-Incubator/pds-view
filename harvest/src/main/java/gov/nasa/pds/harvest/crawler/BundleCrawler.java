package gov.nasa.pds.harvest.crawler;

import gov.nasa.jpl.oodt.cas.crawl.action.CrawlerActionRepo;
import gov.nasa.pds.harvest.crawler.metadata.extractor.PDSMetExtractorConfig;
import gov.nasa.pds.harvest.inventory.InventoryEntry;
import gov.nasa.pds.harvest.inventory.InventoryReader;
import gov.nasa.pds.harvest.inventory.InventoryReaderException;
import gov.nasa.pds.harvest.inventory.InventoryXMLReader;
import gov.nasa.pds.harvest.logging.ToolsLevel;
import gov.nasa.pds.harvest.logging.ToolsLogRecord;

import java.io.File;
import java.util.logging.Logger;

/**
 * A crawler class for a PDS Bundle file.
 *
 * @author mcayanan
 *
 */
public class BundleCrawler extends CollectionCrawler {
    /** Logger object. */
    private static Logger log = Logger.getLogger(
            BundleCrawler.class.getName());

    /**
     * Constructor.
     *
     * @param extractorConfig A configuration object for the
     * metadata extractor.
     */
    public BundleCrawler(PDSMetExtractorConfig extractorConfig) {
        super(extractorConfig);
    }

    /**
     * Crawl a PDS4 bundle file. The bundle will be registered first, then
     * the method will proceed to crawling the collection file it points to.
     *
     * @param bundle The PDS4 bundle file.
     *
     * @throws InventoryReaderException
     */
    public void crawl(File bundle) {
        //Load crawlerActions first before crawling
        CrawlerActionRepo repo = new CrawlerActionRepo();
        repo.loadActions(getActions());
        setActionRepo(repo);
        handleFile(bundle);
        try {
            InventoryReader reader = new InventoryXMLReader(bundle);
            for (InventoryEntry entry = reader.getNext(); entry != null;) {
                super.crawl(entry.getFile());
                entry = reader.getNext();
            }
        } catch (InventoryReaderException e) {
            e.printStackTrace();
            log.log(new ToolsLogRecord(ToolsLevel.SEVERE, e.getMessage(),
                    bundle));
        }
    }
}

package demo.chess.api.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import demo.chess.api.dto.OpeningCatalogImportResultDto;

/** Installs the offline bundled Lichess opening catalog during backend startup. */
@Component
public class BundledOpeningCatalogInitializer implements InitializingBean {

    private static final Log logger = LogFactory.getLog(
            BundledOpeningCatalogInitializer.class);

    private final BundledOpeningCatalogService catalogService;

    public BundledOpeningCatalogInitializer(
            BundledOpeningCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * Ensures the bundled catalog is available before the application accepts
     * user interaction. This path reads classpath data only and never contacts
     * GitHub or Lichess.
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        OpeningCatalogImportResultDto result =
                catalogService.ensureCatalogInstalled();
        logger.info("Bundled Lichess opening catalog ready: revision="
                + result.sourceRevision()
                + ", entries=" + result.catalogEntries()
                + ", newlyInstalled=" + result.importedCount());
    }
}

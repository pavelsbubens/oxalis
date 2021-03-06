package eu.peppol.statistics;

import eu.peppol.statistics.repository.DownloadRepository;
import eu.peppol.statistics.repository.RepositoryEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Parses an XML document into an object graph, which is persisted into a
 * data warehouse snow flake scheme.
 *
 * @author steinar
 *         Date: 22.03.13
 *         Time: 17:50
 */
public class StatisticsImporter {

    private final DownloadRepository downloadRepository;

    public static final Logger logger = LoggerFactory.getLogger(StatisticsImporter.class);

    public StatisticsImporter(DownloadRepository downloadRepository) {

        this.downloadRepository = downloadRepository;

    }

    public List<ImportResult> loadSaveAndArchive() {

        List<ImportResult> results = new ArrayList<ImportResult>();

        AggregatedStatisticsParser aggregatedStatisticsParser = new AggregatedStatisticsParser();

        // Iterates the downloaded contents
        Collection<RepositoryEntry> repositoryEntries = downloadRepository.listDownloadedData();
        AggregatedStatisticsRepository aggregatedStatisticsRepository = StatisticsRepositoryFactoryProvider.getInstance().getInstanceForAggregatedStatistics();
        try {
        for (RepositoryEntry repositoryEntry : repositoryEntries) {

            File contentsFile = repositoryEntry.getContentsFile();

            ImportResult importResult = null;
            try {
                // Parse into list of object graphs
                Collection<AggregatedStatistics> aggregatedStatisticsEntries = aggregatedStatisticsParser.parse(repositoryEntry.getAccessPointIdentifier(),new FileInputStream(contentsFile));
                for (AggregatedStatistics aggregatedStatistics : aggregatedStatisticsEntries) {
                    Integer pk = aggregatedStatisticsRepository.persist(aggregatedStatistics);
                }

                // Archive this downloaded entry
                downloadRepository.archive(repositoryEntry);

                importResult = new ImportResult(ImportResult.ResultCode.OK, repositoryEntry);
            } catch (FileNotFoundException e) {
                importResult = new ImportResult(ImportResult.ResultCode.FAILED, repositoryEntry, "Unable to read file ", e);
            } catch (Exception e) {
                importResult = new ImportResult(ImportResult.ResultCode.FAILED, repositoryEntry, "Unable to process", e);
            }

            switch (importResult.resultCode) {
                case FAILED:
                    logger.warn("Failed processing for " + repositoryEntry.getContentsFile().getAbsolutePath());
                    break;
                case OK:
                    logger.info("Processed " + repositoryEntry.getContentsFile() + " OK");
                    break;
                default:
                    logger.info("Processed " + repositoryEntry.getContentsFile() + " with status of " + importResult.getResultCode().name());
                    break;
            }
            results.add(importResult);
        }
        } finally {
            aggregatedStatisticsRepository.close();
        }

        return results;
    }


    public static class ImportResult implements Serializable {
        private final ResultCode resultCode;
        private final RepositoryEntry repositoryEntry;
        private final String message;
        private final Throwable cause;

        public ImportResult(ResultCode resultCode, RepositoryEntry repositoryEntry) {

            this.resultCode = resultCode;
            this.repositoryEntry = repositoryEntry;
            message = "OK";
            cause = null;
        }

        public enum ResultCode { OK, FAILED };

        public ImportResult(ResultCode resultCode, RepositoryEntry repositoryEntry, String message, Throwable cause) {
            this.resultCode = resultCode;
            this.repositoryEntry = repositoryEntry;
            this.message = message;
            this.cause = cause;
        }

        public RepositoryEntry getRepositoryEntry() {
            return repositoryEntry;
        }

        public String getMessage() {
            return message;
        }

        public Throwable getCause() {
            return cause;
        }

        public ResultCode getResultCode() {
            return resultCode;
        }
    }
}

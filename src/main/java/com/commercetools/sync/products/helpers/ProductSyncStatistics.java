package com.commercetools.sync.products.helpers;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;

import static java.lang.String.format;

public class ProductSyncStatistics extends BaseSyncStatistics {
    /**
     * Builds a summary of the product sync statistics instance that looks like the following example:
     *
     * <p>"Summary: 2 products were processed in total (0 created, 0 updated and 0 failed to sync)."
     *
     * @return a summary message of the product sync statistics instance.
     */
    @Override
    public String getReportMessage() {
        reportMessage = format(
            "Summary: %s products were processed in total (%s created, %s updated and %s failed to sync).",
            getProcessed(), getCreated(), getUpdated(), getFailed());
        return reportMessage;
    }
}

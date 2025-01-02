//package zzk.townshipscheduler.backend.crawling;
//
//import org.jsoup.nodes.Document;
//import org.jsoup.select.Elements;
//import org.springframework.retry.support.RetryTemplate;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.support.TransactionTemplate;
//import zzk.townshipscheduler.backend.dao.WikiCrawledEntityRepository;
//
//import java.util.concurrent.ExecutorService;
//import java.util.function.Supplier;
//
//@Component
//public class TownshipFandomCrawlingProcessorImplProductsProcess extends BaseTownshipFandomCrawlingProcessor {
//
//    public TownshipFandomCrawlingProcessorImplProductsProcess(
//            CrawledDataMemory crawledDataMemory,
//            WikiCrawledEntityRepository wikiCrawledEntityRepository,
//            ExecutorService townshipExecutorService,
//            RetryTemplate retryTemplate,
//            TransactionTemplate transactionTemplate
//    ) {
//        super(
//                crawledDataMemory,
//                wikiCrawledEntityRepository,
//                townshipExecutorService,
//                retryTemplate,
//                transactionTemplate
//        );
//    }
//
//    @Override
//    public Supplier<Elements> useFunctionToGetElements(Document document) {
//        return () -> {
//            Elements elements = document.getElementsByClass("article-table");
//            elements.removeIf(element -> {
//                String tableZoneString = super.findTableZoneString(element);
//                for (String string : TownshipDataCrawlingConstants.TOWNSHIP_FANDOM_GOODS_ABANDON_ZONE) {
//                    if (string.equalsIgnoreCase(tableZoneString)) {
//                        return true;
//                    }
//                }
//                return false;
//            });
//            return elements;
//        };
//    }
//
//    @Override
//    public String useUrlStringAsFetchSource() {
//        return TownshipDataCrawlingConstants.TOWNSHIP_FANDOM_GOODS;
//    }
//
//}

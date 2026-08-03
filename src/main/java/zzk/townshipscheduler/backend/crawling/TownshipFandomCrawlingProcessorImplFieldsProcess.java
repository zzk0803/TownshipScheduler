//package zzk.townshipscheduler.backend.crawling;
//
//import org.jsoup.nodes.Document;
//import org.jsoup.select.Elements;
//import org.springframework.retry.support.RetryTemplate;
//import org.springframework.transaction.support.TransactionTemplate;
//import zzk.townshipscheduler.backend.persistence.dao.WikiCrawledEntityRepository;
//
//import java.util.concurrent.ExecutorService;
//import java.util.function.Supplier;
//
//public class TownshipFandomCrawlingProcessorImplFieldsProcess extends BaseTownshipFandomCrawlingProcessor {
//
//    public TownshipFandomCrawlingProcessorImplFieldsProcess(
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
//        return ()->{
//            return document.select(TownshipDataCrawlingConstants.TOWNSHIP_FANDOM_FIELDS_SELECTOR__CSS);
//        };
//    }
//
//    @Override
//    public String useUrlStringAsFetchSource() {
//        return TownshipDataCrawlingConstants.TOWNSHIP_FANDOM_FIELDS;
//    }
//
//}

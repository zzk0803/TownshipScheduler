//package zzk.townshipscheduler.backend.crawling;
//
//import org.jsoup.nodes.Document;
//import org.jsoup.nodes.Element;
//import org.jsoup.select.Elements;
//import org.springframework.retry.support.RetryTemplate;
//import org.springframework.transaction.support.TransactionTemplate;
//import zzk.townshipscheduler.backend.persistence.dao.WikiCrawledEntityRepository;
//
//import java.util.List;
//import java.util.concurrent.ExecutorService;
//import java.util.function.Supplier;
//
//public class TownshipFandomCrawlingProcessorImplFactoriesProcess extends BaseTownshipFandomCrawlingProcessor {
//
//    public TownshipFandomCrawlingProcessorImplFactoriesProcess(
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
//            Elements elements = document.selectXpath(TownshipDataCrawlingConstants.TOWNSHIP_FANDOM_FACTORIES_SELECTOR__XPATH);
//            assert elements.size() == 1;
//
//            Element first = elements.getFirst();
//            Elements firstTr = first.getElementsContainingText(
//                    "General table information can be found here.");
//            Elements headTr = first.select("tr::first-child");
//            Element tr = headTr.first();
//            Element secondTr = tr.nextElementSibling();
//            Element replacedHeadTr = new Element("tr");
//            List<Element> appendChild = List.of(
//                    new Element("td").text("Sequence"),
//                    new Element("td").text("Image"),
//                    new Element("td").text("Level"),
//                    new Element("td").text("Population"),
//                    new Element("td").text("Cost"),
//                    new Element("td").text("Duration"),
//                    new Element("td").text("Game Version")
//            );
//
//            replacedHeadTr.appendChildren(appendChild);
//            secondTr.prependChild(replacedHeadTr);
//            tr.remove();
//            return elements;
//        };
//    }
//
//    @Override
//    public String useUrlStringAsFetchSource() {
//        return TownshipDataCrawlingConstants.TOWNSHIP_FANDOM_FACTORIES;
//    }
//
//}

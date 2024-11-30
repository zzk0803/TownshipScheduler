package zzk.townshipscheduler.backend.crawling;

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import lombok.Data;

@Data
public final class ParsedResultSegment {

    public static final String STR_EMPTY_FLAG = "EMPTY";

    public static ParsedResultSegment NA = new ParsedResultSegment(STR_EMPTY_FLAG);

    private String category = "";

//    private MultiValueMap<String, RawDataCrawledCell> columnValuesMap;

    private Table<Integer, String, CrawledDataCell> table;

    public ParsedResultSegment(String category) {
        this.category = category;
//        this.columnValuesMap = new LinkedMultiValueMap<>();
        this.table = TreeBasedTable.create();
    }

//    public void add(String columnName, RawDataCrawledCell dataAsCell) {
//        columnValuesMap.add(columnName, dataAsCell);
//    }

    public void add(int row, String columnName, CrawledDataCell cell) {
        this.table.put(row, columnName, cell);
    }

    public int size() {
//        return columnValuesMap.size();
        return table.rowMap().size();
    }

    public void clear() {
//        columnValuesMap.clear();
        this.table.clear();
    }

//    public int alignSize() {
//        int alignSize = columnValuesMap.values()
//                .stream()
//                .findFirst()
//                .map(List::size)
//                .orElse(0)
//                ;
//        Collection<List<RawDataCrawledCell>> values = columnValuesMap.values();
//        for (List<RawDataCrawledCell> serials : values) {
//            int serialsSize = serials.size();
//            if (alignSize != serialsSize) {
//                throw new IllegalStateException("every serials size should be equal!");
//            }
//        }
//        return alignSize;
//    }

//    public Object mapToEntity() {
//        MultiValueMap<String, RawDataCrawledCell> map = getColumnValuesMap();
//        Set<String> columns = map.keySet();
//        int size = checkValuesSizeAlign(map);
//
//        for (int index = 0; index < size; index++) {
//            for (String column : columns) {
//                RawDataCrawledCell dataCell = map.get(column).get(index);
//
//            }
//        }
//    }
//
//    private int checkValuesSizeAlign(MultiValueMap<String, RawDataCrawledCell> map) {
//        int firstSize = map.values().iterator().next().size();
//        if (map.values().stream().allMatch(list -> list.size() != firstSize)) {
//            throw new IllegalStateException("size of cell for every column should be equal");
//        }
//        return firstSize;
//    }

}

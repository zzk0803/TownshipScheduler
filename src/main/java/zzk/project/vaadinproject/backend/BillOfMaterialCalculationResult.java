package zzk.project.vaadinproject.backend;

import zzk.project.vaadinproject.backend.persistence.Goods;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class BillOfMaterialCalculationResult {

    private final Map<Goods, Integer> bom;

    private BillOfMaterialCalculationResult deepResult;

    public BillOfMaterialCalculationResult() {
        bom = new LinkedHashMap<>();
    }

    public Integer put(Goods key, Integer value) {
        return bom.put(key, value);
    }

    public void clear() {
        bom.clear();
    }

    public boolean remove(Object key, Object value) {
        return bom.remove(key, value);
    }

    public Integer computeIfAbsent(Goods key, Function<? super Goods, ? extends Integer> mappingFunction) {
        return bom.computeIfAbsent(key, mappingFunction);
    }

    public Integer computeIfPresent(
            Goods key,
            BiFunction<? super Goods, ? super Integer, ? extends Integer> remappingFunction
    ) {
        return bom.computeIfPresent(key, remappingFunction);
    }

    public Integer compute(
            Goods key,
            BiFunction<? super Goods, ? super Integer, ? extends Integer> remappingFunction
    ) {
        return bom.compute(key, remappingFunction);
    }

    public Integer merge(
            Goods key,
            Integer value,
            BiFunction<? super Integer, ? super Integer, ? extends Integer> remappingFunction
    ) {
        return bom.merge(key, value, remappingFunction);
    }

    public Integer get(Object key) {
        return bom.get(key);
    }

    public boolean containsKey(Object key) {
        return bom.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return bom.containsValue(value);
    }

    public int size() {
        return bom.size();
    }

    public boolean isEmpty() {
        return bom.isEmpty();
    }

    public Integer remove(Object key) {
        return bom.remove(key);
    }

    public void putAll(Map<? extends Goods, ? extends Integer> m) {
        bom.putAll(m);
    }

    public Set<Goods> keySet() {
        return bom.keySet();
    }

    public Collection<Integer> values() {
        return bom.values();
    }

    public Set<Map.Entry<Goods, Integer>> entrySet() {
        return bom.entrySet();
    }

    public Integer getOrDefault(Object key, Integer defaultValue) {
        return bom.getOrDefault(key, defaultValue);
    }

    public Integer putIfAbsent(Goods key, Integer value) {
        return bom.putIfAbsent(key, value);
    }

    public BillOfMaterialCalculationResult setupDeepResult() {
        this.deepResult = new BillOfMaterialCalculationResult();
        return this.deepResult;
    }

    public boolean hasDeepResult() {
        return Objects.isNull(deepResult);
    }

}

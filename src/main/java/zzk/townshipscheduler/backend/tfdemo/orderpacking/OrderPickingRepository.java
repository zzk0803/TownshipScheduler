package zzk.townshipscheduler.backend.tfdemo.orderpacking;

public class OrderPickingRepository {

    private OrderPickingSolution orderPickingSolution;

    public OrderPickingSolution find() {
        return orderPickingSolution;
    }

    public void save(OrderPickingSolution orderPickingSolution) {
        this.orderPickingSolution = orderPickingSolution;
    }

}

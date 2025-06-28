package zzk.townshipscheduler.ui.views.player;

import com.vaadin.flow.spring.annotation.SpringComponent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Sort;
import zzk.townshipscheduler.backend.TownshipAuthenticationContext;
import zzk.townshipscheduler.backend.dao.ProductEntityRepository;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.service.PlayerService;

import java.util.Set;

@SpringComponent
@RequiredArgsConstructor
@Getter
@Setter
public class PlayerViewPresenter {

    private final PlayerService playerService;

    private final ProductEntityRepository productEntityRepository;

    private PlayerView playerView;

    private TownshipAuthenticationContext townshipAuthenticationContext;

    public Set<ProductEntity> fetchProducts() {
        return productEntityRepository.findBy(
                ProductEntity.class,
                Sort.by(Sort.Order.asc("level"))
        );
    }

}

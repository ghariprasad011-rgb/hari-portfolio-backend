
package com.hari.portfolio_backend.entity.repository;

import com.hari.portfolio_backend.entity.PortfolioMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioMediaRepository
        extends JpaRepository<PortfolioMedia, Long> {

    // Retrieves only media files approved for public display.
    List<PortfolioMedia> findByPublicVisibleTrue();
}

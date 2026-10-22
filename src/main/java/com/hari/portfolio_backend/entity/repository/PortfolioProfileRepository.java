
package com.hari.portfolio_backend.entity.repository;

import com.hari.portfolio_backend.entity.PortfolioProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioProfileRepository
        extends JpaRepository<PortfolioProfile, Long> {
}

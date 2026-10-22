
package com.hari.portfolio_backend.controller;

import com.hari.portfolio_backend.entity.PortfolioProfile;
import com.hari.portfolio_backend.entity.repository.PortfolioProfileRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/profile")
public class PublicPortfolioProfileController {

    private final PortfolioProfileRepository profileRepository;

    public PublicPortfolioProfileController(
            PortfolioProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @GetMapping
    public ResponseEntity<PortfolioProfile> getPublicProfile() {

        PortfolioProfile profile = profileRepository.findById(1L)
                .orElseGet(() -> {
                    PortfolioProfile emptyProfile = new PortfolioProfile();
                    emptyProfile.setId(1L);
                    return emptyProfile;
                });

        return ResponseEntity.ok(profile);
    }
}

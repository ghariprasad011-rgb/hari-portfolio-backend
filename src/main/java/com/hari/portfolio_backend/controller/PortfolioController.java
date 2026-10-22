
package com.hari.portfolio_backend.controller;

import com.hari.portfolio_backend.entity.PortfolioProfile;
import com.hari.portfolio_backend.entity.repository.PortfolioProfileRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/profile")
public class PortfolioController {

    private final PortfolioProfileRepository profileRepository;

    public PortfolioController(
            PortfolioProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    // Retrieve the saved portfolio profile
    @GetMapping
    public ResponseEntity<?> getProfile() {
        return ResponseEntity.ok(
            profileRepository.findById(1L)
                .orElseGet(() -> {
                    PortfolioProfile profile =
                        new PortfolioProfile();
                    profile.setId(1L);
                    return profile;
                })
        );
    }

    // Save or update the portfolio profile
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> saveProfile(
            @RequestBody PortfolioProfile request) {

        if (request.getFullName() == null
                || request.getFullName().isBlank()) {
            return ResponseEntity.badRequest().body(
                Map.of("message", "Full name is required.")
            );
        }

        PortfolioProfile profile =
            profileRepository.findById(1L)
                .orElseGet(PortfolioProfile::new);

        profile.setFullName(request.getFullName());
        profile.setIntroduction(request.getIntroduction());
        profile.setEducation(request.getEducation());
        profile.setCollege(request.getCollege());
        profile.setGithubUrl(request.getGithubUrl());
        profile.setLinkedinUrl(request.getLinkedinUrl());
        profile.setLeetcodeUrl(request.getLeetcodeUrl());

        // New editable About section fields
        profile.setAboutDescription(
            request.getAboutDescription()
        );
        profile.setCurrentFocus(
            request.getCurrentFocus()
        );

        PortfolioProfile saved =
            profileRepository.save(profile);

        return ResponseEntity.ok(saved);
    }
}

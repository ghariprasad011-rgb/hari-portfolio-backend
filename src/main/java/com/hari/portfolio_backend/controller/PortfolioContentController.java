
package com.hari.portfolio_backend.controller;

import com.hari.portfolio_backend.entity.Project;
import com.hari.portfolio_backend.entity.Skill;
import com.hari.portfolio_backend.repository.ProjectRepository;
import com.hari.portfolio_backend.repository.SkillRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class PortfolioContentController {

    private final SkillRepository skillRepository;
    private final ProjectRepository projectRepository;

    public PortfolioContentController(
            SkillRepository skillRepository,
            ProjectRepository projectRepository) {
        this.skillRepository = skillRepository;
        this.projectRepository = projectRepository;
    }

    // Public: view skills
    @GetMapping("/api/public/skills")
    public List<Skill> getPublicSkills() {
        return skillRepository.findAllByOrderByDisplayOrderAscIdAsc();
    }

    // Public: view projects
    @GetMapping("/api/public/projects")
    public List<Project> getPublicProjects() {
        return projectRepository.findAllByOrderByDisplayOrderAscIdAsc();
    }

    // Admin: manage skills
    @GetMapping("/api/admin/skills")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Skill> getAdminSkills() {
        return skillRepository.findAllByOrderByDisplayOrderAscIdAsc();
    }

    @PostMapping("/api/admin/skills")
    @PreAuthorize("hasRole('ADMIN')")
    public Skill createSkill(@RequestBody Skill skill) {
        skill.setId(null);
        validateSkill(skill);
        return skillRepository.save(skill);
    }

    @PutMapping("/api/admin/skills/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Skill updateSkill(
            @PathVariable Long id,
            @RequestBody Skill updatedSkill) {

        Skill existing = skillRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Skill not found"));

        validateSkill(updatedSkill);
        existing.setName(updatedSkill.getName());
        existing.setCategory(updatedSkill.getCategory());
        existing.setDisplayOrder(updatedSkill.getDisplayOrder());

        return skillRepository.save(existing);
    }

    @DeleteMapping("/api/admin/skills/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteSkill(@PathVariable Long id) {
        if (!skillRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Skill not found");
        }
        skillRepository.deleteById(id);
    }

    // Admin: manage projects
    @GetMapping("/api/admin/projects")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Project> getAdminProjects() {
        return projectRepository.findAllByOrderByDisplayOrderAscIdAsc();
    }

    @PostMapping("/api/admin/projects")
    @PreAuthorize("hasRole('ADMIN')")
    public Project createProject(@RequestBody Project project) {
        project.setId(null);
        validateProject(project);
        return projectRepository.save(project);
    }

    @PutMapping("/api/admin/projects/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Project updateProject(
            @PathVariable Long id,
            @RequestBody Project updatedProject) {

        Project existing = projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Project not found"));

        validateProject(updatedProject);

        existing.setTitle(updatedProject.getTitle());
        existing.setDescription(updatedProject.getDescription());
        existing.setTechnologies(updatedProject.getTechnologies());
        existing.setGithubUrl(updatedProject.getGithubUrl());
        existing.setDemoUrl(updatedProject.getDemoUrl());
        existing.setImageUrl(updatedProject.getImageUrl());
        existing.setDisplayOrder(updatedProject.getDisplayOrder());

        return projectRepository.save(existing);
    }

    @DeleteMapping("/api/admin/projects/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteProject(@PathVariable Long id) {
        if (!projectRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Project not found");
        }
        projectRepository.deleteById(id);
    }

    private void validateSkill(Skill skill) {
        if (skill.getName() == null || skill.getName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Skill name is required");
        }
        if (skill.getDisplayOrder() == null) {
            skill.setDisplayOrder(0);
        }
    }

    private void validateProject(Project project) {
        if (project.getTitle() == null || project.getTitle().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Project title is required");
        }
        if (project.getDisplayOrder() == null) {
            project.setDisplayOrder(0);
        }
    }
}

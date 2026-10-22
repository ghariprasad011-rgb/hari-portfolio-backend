
package com.hari.portfolio_backend.repository;

import com.hari.portfolio_backend.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findAllByOrderByDisplayOrderAscIdAsc();
}

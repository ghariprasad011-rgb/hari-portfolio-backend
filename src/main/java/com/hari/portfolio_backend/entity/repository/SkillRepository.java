
package com.hari.portfolio_backend.repository;

import com.hari.portfolio_backend.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    List<Skill> findAllByOrderByDisplayOrderAscIdAsc();
}

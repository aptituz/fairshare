package com.fairshare.repo

import com.fairshare.model.Category
import org.springframework.data.jpa.repository.JpaRepository

interface CategoryRepository : JpaRepository<Category, Long> {
    fun findTopByOrderByRankDesc(): Category?
}

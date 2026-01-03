package com.fairshare.controller

import com.fairshare.dto.CategoryResponse
import com.fairshare.dto.CreateCategoryRequest
import com.fairshare.dto.UpdateCategoryRequest
import com.fairshare.service.CategoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categories", description = "Manage budget categories.")
class CategoryController(
    private val categoryService: CategoryService
) {
    @GetMapping
    @Operation(summary = "List categories")
    fun list(): List<CategoryResponse> =
        categoryService.list()

    @PostMapping
    @Operation(summary = "Create a category")
    fun create(@RequestBody request: CreateCategoryRequest): CategoryResponse {
        return categoryService.create(request)
    }

    @PutMapping("/{id}")
    @Operation(summary = "Rename a category")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: UpdateCategoryRequest
    ): CategoryResponse =
        categoryService.update(id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a category")
    fun delete(@PathVariable id: Long) {
        categoryService.delete(id)
    }
}

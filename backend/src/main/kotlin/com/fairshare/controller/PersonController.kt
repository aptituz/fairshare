package com.fairshare.controller

import com.fairshare.dto.CreatePersonRequest
import com.fairshare.dto.PersonResponse
import com.fairshare.dto.UpdatePersonRequest
import com.fairshare.service.PersonService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/persons")
@Tag(name = "Persons", description = "Manage persons for shared budgets.")
class PersonController(
    private val personService: PersonService,
) {
    @GetMapping
    @Operation(summary = "List persons")
    fun list(): List<PersonResponse> = personService.list()

    @PostMapping
    @Operation(summary = "Create a person")
    fun create(
        @RequestBody request: CreatePersonRequest,
    ): PersonResponse = personService.create(request)

    @PutMapping("/{id}")
    @Operation(summary = "Rename a person")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: UpdatePersonRequest,
    ): PersonResponse = personService.update(id, request)
}

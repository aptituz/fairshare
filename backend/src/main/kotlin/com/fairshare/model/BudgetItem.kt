package com.fairshare.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Entity
@Table(name = "budget_items")
class BudgetItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false, precision = 14, scale = 2)
    var amount: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: BudgetItemType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var frequency: Frequency = Frequency.MONTHLY,

    @Column(nullable = false)
    var active: Boolean = true,

    @Column(nullable = false)
    var planned: Boolean = true,

    @Column(name = "category_correction", nullable = false)
    var categoryCorrection: Boolean = false,

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate = LocalDate.now(),

    @Column(name = "end_date")
    var endDate: LocalDate? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    var category: Category? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    var person: Person? = null
) {
    fun monthlyAmount(): BigDecimal =
        when (frequency) {
            Frequency.MONTHLY -> amount
            Frequency.ONE_TIME -> amount
            Frequency.QUARTERLY -> amount.divide(BigDecimal("3"), 2, RoundingMode.HALF_UP)
            Frequency.HALF_YEARLY -> amount.divide(BigDecimal("6"), 2, RoundingMode.HALF_UP)
            Frequency.YEARLY -> amount.divide(BigDecimal("12"), 2, RoundingMode.HALF_UP)
        }
}

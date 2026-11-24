package com.ledger.app.cloudgroup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudGroupPairwiseBalanceCalculatorTest {

    private fun member(id: String) = CloudGroupMember(
        id = id,
        groupId = "group-1",
        userId = null,
        memberName = id,
        memberEmail = "$id@example.com",
        role = "member",
        status = "joined",
        createdAt = "2026-11-24T00:00:00Z"
    )

    private fun expense(id: String) = CloudGroupExpense(
        id = id,
        groupId = "group-1",
        title = "Expense",
        amount = 100.0,
        category = "Cash",
        note = "",
        expenseDateMs = 1000L,
        splitMode = "Equal",
        createdBy = "user-1",
        createdAtMs = 1000L
    )

    private fun split(
        expenseId: String,
        memberId: String,
        amount: Double
    ) = CloudGroupExpenseSplit(
        id = "split-$expenseId-$memberId",
        groupId = "group-1",
        expenseId = expenseId,
        memberId = memberId,
        memberNameSnapshot = memberId,
        amount = amount
    )

    private fun payment(
        expenseId: String,
        memberId: String,
        amount: Double
    ) = CloudGroupExpensePayment(
        id = "payment-$expenseId-$memberId",
        groupId = "group-1",
        expenseId = expenseId,
        memberId = memberId,
        memberNameSnapshot = memberId,
        amount = amount,
        paidAtMs = 1000L
    )

    @Test
    fun singlePayer_createsDebtFromOtherMembersToPayer() {
        val result = CloudGroupPairwiseBalanceCalculator.calculate(
            members = listOf(member("A"), member("B")),
            expenses = listOf(expense("E1")),
            splits = listOf(
                split("E1", "A", 50.0),
                split("E1", "B", 50.0)
            ),
            payments = listOf(payment("E1", "A", 100.0))
        )

        assertEquals(
            listOf(CloudGroupPairwiseDebt("B", "A", 5000L)),
            result.debts
        )
        assertTrue(result.unallocatedExpenseIds.isEmpty())
    }

    @Test
    fun multiplePayers_allocatesDebtProportionally() {
        val result = CloudGroupPairwiseBalanceCalculator.calculate(
            members = listOf(
                member("A"), member("B"), member("C")
            ),
            expenses = listOf(expense("E1")),
            splits = listOf(
                split("E1", "A", 30.0),
                split("E1", "B", 30.0),
                split("E1", "C", 40.0)
            ),
            payments = listOf(
                payment("E1", "A", 50.0),
                payment("E1", "B", 50.0)
            )
        )

        assertEquals(
            4000L,
            result.debts
                .filter { it.fromMemberId == "C" }
                .sumOf { it.amountPaise }
        )

        assertEquals(
            2000L,
            result.debts
                .filter { it.toMemberId == "A" }
                .sumOf { it.amountPaise }
        )

        assertEquals(
            2000L,
            result.debts
                .filter { it.toMemberId == "B" }
                .sumOf { it.amountPaise }
        )
    }

    @Test
    fun expenseWithoutPayments_isMarkedUnallocated() {
        val result = CloudGroupPairwiseBalanceCalculator.calculate(
            members = listOf(member("A"), member("B")),
            expenses = listOf(expense("E1")),
            splits = listOf(
                split("E1", "A", 50.0),
                split("E1", "B", 50.0)
            ),
            payments = emptyList()
        )

        assertTrue(result.debts.isEmpty())
        assertEquals(setOf("E1"), result.unallocatedExpenseIds)
    }

    @Test
    fun mismatchedPaymentTotal_isMarkedUnallocated() {
        val result = CloudGroupPairwiseBalanceCalculator.calculate(
            members = listOf(member("A"), member("B")),
            expenses = listOf(expense("E1")),
            splits = listOf(
                split("E1", "A", 50.0),
                split("E1", "B", 50.0)
            ),
            payments = listOf(payment("E1", "A", 90.0))
        )

        assertTrue(result.debts.isEmpty())
        assertEquals(setOf("E1"), result.unallocatedExpenseIds)
    }
}

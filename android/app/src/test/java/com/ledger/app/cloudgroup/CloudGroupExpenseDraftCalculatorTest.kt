package com.ledger.app.cloudgroup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CloudGroupExpenseDraftCalculatorTest {

    private fun member(
        id: String,
        name: String = id,
        status: String = "joined"
    ) = CloudGroupMember(
        id = id,
        groupId = "group-1",
        memberName = name,
        memberEmail = "$id@example.com",
        role = "member",
        status = status,
        createdAt = "2026-11-23T00:00:00Z"
    )

    @Test
    fun equalSplit_distributesRemainderWithoutLosingPaise() {
        val result = CloudGroupExpenseDraftCalculator.buildSplits(
            totalAmount = 100.00,
            mode = "Equal",
            selectedMembers = listOf(member("m1"), member("m2"), member("m3"))
        )

        assertEquals(listOf(33.34, 33.33, 33.33), result.map { it.amount })
        assertEquals(10000L, result.sumOf { (it.amount * 100).toLong() })
    }

    @Test
    fun customSplit_requiresExactTotal() {
        val result = CloudGroupExpenseDraftCalculator.buildSplits(
            totalAmount = 100.00,
            mode = "Custom",
            selectedMembers = listOf(member("m1"), member("m2")),
            customAmounts = mapOf("m1" to 60.00, "m2" to 40.00)
        )

        assertEquals(listOf(60.0, 40.0), result.map { it.amount })

        assertThrows(IllegalArgumentException::class.java) {
            CloudGroupExpenseDraftCalculator.buildSplits(
                totalAmount = 100.00,
                mode = "Custom",
                selectedMembers = listOf(member("m1"), member("m2")),
                customAmounts = mapOf("m1" to 60.00, "m2" to 30.00)
            )
        }
    }

    @Test
    fun shareSplit_preservesTotalToThePaise() {
        val result = CloudGroupExpenseDraftCalculator.buildSplits(
            totalAmount = 100.00,
            mode = "Share",
            selectedMembers = listOf(member("m1"), member("m2"), member("m3")),
            shareUnits = mapOf("m1" to 1.0, "m2" to 2.0, "m3" to 3.0)
        )

        assertEquals(listOf(16.67, 33.33, 50.0), result.map { it.amount })
        assertEquals(10000L, result.sumOf { (it.amount * 100).toLong() })
    }

    @Test
    fun shareSplit_rejectsNonPositiveUnits() {
        assertThrows(IllegalArgumentException::class.java) {
            CloudGroupExpenseDraftCalculator.buildSplits(
                totalAmount = 100.00,
                mode = "Share",
                selectedMembers = listOf(member("m1")),
                shareUnits = mapOf("m1" to 0.0)
            )
        }
    }

    @Test
    fun pendingMembersCannotBeSelected() {
        assertThrows(IllegalArgumentException::class.java) {
            CloudGroupExpenseDraftCalculator.buildSplits(
                totalAmount = 100.00,
                mode = "Equal",
                selectedMembers = listOf(member("m1"), member("m2", status = "pending"))
            )
        }
    }

    @Test
    fun paidBy_calculatesYouAsRemainingAmount() {
        val result = CloudGroupExpenseDraftCalculator.buildPayments(
            totalAmount = 100.00,
            members = listOf(member("you", "You"), member("m1"), member("m2")),
            youMemberId = "you",
            otherMemberAmounts = mapOf("m1" to 30.00, "m2" to 20.00),
            paymentType = "Online"
        )

        assertEquals(listOf(50.0, 30.0, 20.0), result.map { it.amount })
        assertEquals(listOf("you", "m1", "m2"), result.map { it.memberId })
        assertEquals(setOf("Online"), result.map { it.paymentType }.toSet())
    }

    @Test
    fun paidBy_rejectsPaymentsAboveExpenseAmount() {
        assertThrows(IllegalArgumentException::class.java) {
            CloudGroupExpenseDraftCalculator.buildPayments(
                totalAmount = 100.00,
                members = listOf(member("you"), member("m1")),
                youMemberId = "you",
                otherMemberAmounts = mapOf("m1" to 100.01)
            )
        }
    }

    @Test
    fun paidBy_omitsZeroPaymentsAndAssignsFullAmountToYou() {
        val result = CloudGroupExpenseDraftCalculator.buildPayments(
            totalAmount = 50.00,
            members = listOf(member("you"), member("m1")),
            youMemberId = "you",
            otherMemberAmounts = mapOf("m1" to 0.0)
        )

        assertEquals(1, result.size)
        assertEquals("you", result.single().memberId)
        assertEquals(50.0, result.single().amount, 0.001)
    }

    @Test
    fun manualPayments_useEnteredPayersWithoutAutomaticYouAmount() {
        val result = CloudGroupExpenseDraftCalculator.buildManualPayments(
            totalAmount = 100.00,
            members = listOf(member("you", "You"), member("m1"), member("m2")),
            payerAmounts = mapOf("you" to 40.00, "m2" to 60.00),
            excludedMemberIds = setOf("m1"),
            paymentType = "Online"
        )

        assertEquals(listOf("you", "m2"), result.map { it.memberId })
        assertEquals(listOf(40.0, 60.0), result.map { it.amount })
        assertEquals(setOf("Online"), result.map { it.paymentType }.toSet())
    }

    @Test
    fun manualPayments_rejectAmountsThatDoNotMatchTotal() {
        assertThrows(IllegalArgumentException::class.java) {
            CloudGroupExpenseDraftCalculator.buildManualPayments(
                totalAmount = 100.00,
                members = listOf(member("you"), member("m1")),
                payerAmounts = mapOf("you" to 40.00, "m1" to 50.00)
            )
        }
    }

    @Test
    fun manualPayments_rejectReceivingMemberAsPayer() {
        assertThrows(IllegalArgumentException::class.java) {
            CloudGroupExpenseDraftCalculator.buildManualPayments(
                totalAmount = 100.00,
                members = listOf(member("you"), member("m1")),
                payerAmounts = mapOf("you" to 100.00),
                excludedMemberIds = setOf("you")
            )
        }
    }
}

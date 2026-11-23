package com.ledger.app.cloudgroup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CloudGroupBalanceCalculatorTest {

    private fun member(id: String) = CloudGroupMember(
        id = id,
        groupId = "group-1",
        memberName = id,
        memberEmail = "$id@example.com",
        role = "member",
        status = "joined",
        createdAt = "2026-11-23T00:00:00Z"
    )

    @Test
    fun calculate_subtractsPaymentsAndSettlementsFromShare() {
        val result = CloudGroupBalanceCalculator.calculate(
            members = listOf(member("m1")),
            splits = listOf(
                CloudGroupExpenseSplit(
                    id = "s1",
                    groupId = "group-1",
                    expenseId = "e1",
                    memberId = "m1",
                    memberNameSnapshot = "m1",
                    amount = 100.00
                )
            ),
            payments = listOf(
                CloudGroupExpensePayment(
                    id = "p1",
                    groupId = "group-1",
                    expenseId = "e1",
                    memberId = "m1",
                    memberNameSnapshot = "m1",
                    amount = 30.00,
                    paidAtMs = 1000L
                )
            ),
            settlements = listOf(
                CloudGroupSettlement(
                    id = "t1",
                    groupId = "group-1",
                    memberId = "m1",
                    memberNameSnapshot = "m1",
                    amount = 20.00,
                    receivedAtMs = 2000L
                )
            )
        )

        val balance = result.getValue("m1")

        assertEquals(10000L, balance.totalSharePaise)
        assertEquals(3000L, balance.totalPaidPaise)
        assertEquals(2000L, balance.totalReceivedPaise)
        assertEquals(5000L, balance.remainingBalancePaise)
    }

    @Test
    fun calculate_excludesPendingMembers() {
        val pending = member("m2").copy(status = "pending")

        val result = CloudGroupBalanceCalculator.calculate(
            members = listOf(member("m1"), pending),
            splits = emptyList(),
            payments = emptyList(),
            settlements = emptyList()
        )

        assertEquals(setOf("m1"), result.keys)
    }

    @Test
    fun calculate_rejectsUnknownMemberReference() {
        assertThrows(IllegalArgumentException::class.java) {
            CloudGroupBalanceCalculator.calculate(
                members = listOf(member("m1")),
                splits = listOf(
                    CloudGroupExpenseSplit(
                        id = "s1",
                        groupId = "group-1",
                        expenseId = "e1",
                        memberId = "unknown",
                        memberNameSnapshot = "Unknown",
                        amount = 10.0
                    )
                ),
                payments = emptyList(),
                settlements = emptyList()
            )
        }
    }

    @Test
    fun calculate_usesIntegerPaise() {
        val result = CloudGroupBalanceCalculator.calculate(
            members = listOf(member("m1")),
            splits = listOf(
                CloudGroupExpenseSplit(
                    id = "s1",
                    groupId = "group-1",
                    expenseId = "e1",
                    memberId = "m1",
                    memberNameSnapshot = "m1",
                    amount = 10.10
                )
            ),
            payments = emptyList(),
            settlements = emptyList()
        )

        assertEquals(1010L, result.getValue("m1").remainingBalancePaise)
    }
}
